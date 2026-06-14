package net.calvuz.qreport.backup.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import net.calvuz.qreport.backup.data.model.ArchiveProgress
import net.calvuz.qreport.backup.data.model.ExtractionProgress
import net.calvuz.qreport.backup.domain.model.BackupValidationResult
import net.calvuz.qreport.backup.domain.model.DocumentBackupInfo
import net.calvuz.qreport.backup.domain.model.DocumentManifest
import net.calvuz.qreport.backup.domain.repository.DocumentArchiveRepository
import net.calvuz.qreport.client.document.data.local.dao.DocumentDao
import net.calvuz.qreport.client.document.data.local.entity.DocumentEntity
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DocumentArchiveRepository
 *
 * Creates/extracts document archives following SignatureArchiveRepositoryImpl pattern:
 * - ZIP compression
 * - SHA256 hash verification
 * - Progress tracking
 * - Archive validation
 *
 * `relativePath` for each entry mirrors DocumentDirectories layout (e.g.
 * "islands/{islandId}/{fileName}", "global/{fileName}") so extraction into the
 * "documents" base directory reconstructs the original file layout.
 */
@Singleton
class DocumentArchiveRepositoryImpl @Inject constructor(
    private val documentDao: DocumentDao
) : DocumentArchiveRepository {

    companion object {
        private const val BUFFER_SIZE = 8192
        private const val MAX_SINGLE_FILE_SIZE = 50 * 1024 * 1024L // 50MB per document
        private const val MAX_TOTAL_ARCHIVE_SIZE = 1024 * 1024 * 1024L // 1GB total
    }

    // ===== CREATE DOCUMENT ARCHIVE =====

    override suspend fun createDocumentArchive(
        outputPath: String
    ): Flow<ArchiveProgress> = flow {

        try {
            Timber.d("Document archive creation begin - output: $outputPath")

            val allDocuments = documentDao.getAllForBackup()

            if (allDocuments.isEmpty()) {
                Timber.d("No documents to archive")
                emit(ArchiveProgress.Completed(outputPath, 0, 0L))
                return@flow
            }

            val existingDocuments = allDocuments.filter { document ->
                File(document.filePath).exists()
            }

            if (existingDocuments.isEmpty()) {
                Timber.d("No existing document files found")
                emit(ArchiveProgress.Completed(outputPath, 0, 0L))
                return@flow
            }

            val totalFiles = existingDocuments.size
            Timber.d("Archiving $totalFiles document files")

            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()

            var processedFiles = 0
            var totalSizeBytes = 0L
            val documentHashes = mutableMapOf<String, String>()

            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zipOut ->

                for (document in existingDocuments) {
                    val currentProgress = processedFiles.toFloat() / totalFiles
                    emit(
                        ArchiveProgress.InProgress(
                            processedFiles = processedFiles,
                            totalFiles = totalFiles,
                            currentFile = document.fileName,
                            progress = currentProgress
                        )
                    )

                    val documentFile = File(document.filePath)
                    val fileSizeBytes = documentFile.length()

                    if (fileSizeBytes == 0L) {
                        Timber.w("Document file ${document.fileName} has zero size, skipping")
                        continue
                    }

                    if (fileSizeBytes > MAX_SINGLE_FILE_SIZE) {
                        Timber.w("Document file ${document.fileName} too large, skipping")
                        continue
                    }

                    val entryPath = document.relativeArchivePath()
                    val fileHash = addFileToZip(
                        zipOut = zipOut,
                        file = documentFile,
                        entryPath = entryPath
                    )

                    documentHashes[document.filePath] = fileHash
                    totalSizeBytes += fileSizeBytes
                    processedFiles++

                    Timber.v("Added document: ${document.fileName}, hash=${fileHash.take(8)}...")

                    if (totalSizeBytes > MAX_TOTAL_ARCHIVE_SIZE) {
                        Timber.w("Archive size limit reached")
                        break
                    }
                }

                addHashManifestToZip(zipOut, documentHashes)
            }

            val archiveSize = outputFile.length()
            Timber.d("Document archive created: $processedFiles files, ${archiveSize / 1024}KB")

            emit(ArchiveProgress.Completed(outputPath, processedFiles, archiveSize))

        } catch (e: Exception) {
            Timber.e(e, "Document archive creation failed")
            emit(ArchiveProgress.Error("Archive creation failed: ${e.message}"))
        }
    }

    // ===== EXTRACT DOCUMENT ARCHIVE =====

    override suspend fun extractDocumentArchive(
        archivePath: String,
        outputDir: String
    ): Flow<ExtractionProgress> = flow {

        try {
            Timber.d("Document archive extraction begin - archive: $archivePath, output: $outputDir")

            val archiveFile = File(archivePath)
            if (!archiveFile.exists()) {
                emit(ExtractionProgress.Error("Archive file not found: $archivePath"))
                return@flow
            }

            val outputDirectory = File(outputDir)
            outputDirectory.mkdirs()

            var extractedFiles = 0
            var totalFiles = 0

            ZipInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && !entry.name.startsWith("MANIFEST")) {
                        totalFiles++
                    }
                    entry = zipIn.nextEntry
                }
            }

            if (totalFiles == 0) {
                emit(ExtractionProgress.Completed(outputDir, 0))
                return@flow
            }

            ZipInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { zipIn ->
                var entry = zipIn.nextEntry

                while (entry != null) {
                    if (!entry.isDirectory && !entry.name.startsWith("MANIFEST")) {
                        val currentProgress = extractedFiles.toFloat() / totalFiles
                        emit(
                            ExtractionProgress.InProgress(
                                extractedFiles = extractedFiles,
                                totalFiles = totalFiles,
                                currentFile = entry.name,
                                progress = currentProgress
                            )
                        )

                        val outputFile = File(outputDirectory, entry.name)
                        extractFileFromZip(zipIn, outputFile)
                        extractedFiles++

                        Timber.v("Extracted: ${entry.name}")
                    }
                    entry = zipIn.nextEntry
                }
            }

            Timber.d("Document extraction completed: $extractedFiles files")
            emit(ExtractionProgress.Completed(outputDir, extractedFiles))

        } catch (e: Exception) {
            Timber.e(e, "Document archive extraction failed")
            emit(ExtractionProgress.Error("Extraction failed: ${e.message}"))
        }
    }

    // ===== GENERATE MANIFEST =====

    override suspend fun generateDocumentManifest(): DocumentManifest {
        return try {
            val documentBackupInfos = mutableListOf<DocumentBackupInfo>()
            var totalSizeBytes = 0L

            for (document in documentDao.getAllForBackup()) {
                val documentFile = File(document.filePath)
                if (documentFile.exists()) {
                    val fileHash = withContext(Dispatchers.IO) {
                        calculateFileHash(documentFile)
                    }

                    documentBackupInfos.add(
                        DocumentBackupInfo(
                            documentId = document.id,
                            scope = document.scope,
                            fileName = document.fileName,
                            relativePath = document.relativeArchivePath(),
                            sizeBytes = document.fileSize,
                            sha256Hash = fileHash
                        )
                    )

                    totalSizeBytes += document.fileSize
                }
            }

            DocumentManifest(
                totalDocuments = documentBackupInfos.size,
                totalSizeMB = totalSizeBytes / (1024.0 * 1024.0),
                documents = documentBackupInfos
            )

        } catch (e: Exception) {
            Timber.e(e, "Error generating document manifest")
            DocumentManifest.empty()
        }
    }

    // ===== VALIDATE INTEGRITY =====

    override suspend fun validateDocumentIntegrity(manifest: DocumentManifest): BackupValidationResult {
        return try {
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()
            var validatedDocuments = 0

            for (documentInfo in manifest.documents) {
                val document = documentDao.getDocumentById(documentInfo.documentId)
                if (document == null) {
                    warnings.add("Documento non trovato in db: ${documentInfo.fileName}")
                    continue
                }

                val documentFile = File(document.filePath)
                if (!documentFile.exists()) {
                    errors.add("File documento mancante: ${documentInfo.fileName}")
                    continue
                }

                if (documentFile.length() != documentInfo.sizeBytes) {
                    warnings.add(
                        "Dimensione documento diversa: ${documentInfo.fileName} " +
                                "(attesa: ${documentInfo.sizeBytes}, trovata: ${documentFile.length()})"
                    )
                }

                withContext(Dispatchers.IO) {
                    val actualHash = calculateFileHash(documentFile)
                    if (actualHash != documentInfo.sha256Hash) {
                        errors.add("Hash documento non valido: ${documentInfo.fileName}")
                    } else {
                        validatedDocuments++
                    }
                }
            }

            Timber.d("Validated documents: $validatedDocuments/${manifest.totalDocuments}")

            BackupValidationResult(
                isValid = errors.isEmpty(),
                errors = errors,
                warnings = warnings
            )

        } catch (e: Exception) {
            Timber.e(e, "Document integrity validation failed")
            BackupValidationResult.invalid(listOf("Validazione fallita: ${e.message}"))
        }
    }

    // ===== HELPER METHODS =====

    /**
     * Relative path within documents.zip, mirroring DocumentDirectories layout
     * (without the "documents/" root, which is the extraction output dir).
     */
    private fun DocumentEntity.relativeArchivePath(): String {
        val scopeDir = when (scope) {
            "ISLAND" -> "islands/$islandId"
            "FACILITY" -> "facilities/$facilityId"
            "CLIENT" -> "clients/$clientId"
            else -> "global"
        }
        return "$scopeDir/$fileName"
    }

    private suspend fun addFileToZip(
        zipOut: ZipOutputStream,
        file: File,
        entryPath: String
    ): String = withContext(Dispatchers.IO) {

        val entry = ZipEntry(entryPath)
        entry.time = file.lastModified()
        entry.size = file.length()

        zipOut.putNextEntry(entry)

        val hash = calculateFileHashWhileReading(file) { buffer, bytesRead ->
            zipOut.write(buffer, 0, bytesRead)
        }

        zipOut.closeEntry()
        hash
    }

    private suspend fun extractFileFromZip(
        zipIn: ZipInputStream,
        outputFile: File
    ): String = withContext(Dispatchers.IO) {

        outputFile.parentFile?.mkdirs()

        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)

        BufferedOutputStream(FileOutputStream(outputFile)).use { fileOut ->
            var bytesRead: Int
            while (zipIn.read(buffer).also { bytesRead = it } != -1) {
                fileOut.write(buffer, 0, bytesRead)
                digest.update(buffer, 0, bytesRead)
            }
        }

        digest.digest().joinToString("") { "%02x".format(it) }
    }

    private suspend fun calculateFileHash(file: File): String = withContext(Dispatchers.IO) {
        calculateFileHashWhileReading(file) { _, _ -> }
    }

    private suspend fun calculateFileHashWhileReading(
        file: File,
        onRead: (ByteArray, Int) -> Unit
    ): String = withContext(Dispatchers.IO) {

        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)

        BufferedInputStream(FileInputStream(file)).use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
                onRead(buffer, bytesRead)
            }
        }

        digest.digest().joinToString("") { "%02x".format(it) }
    }

    private suspend fun addHashManifestToZip(
        zipOut: ZipOutputStream,
        documentHashes: Map<String, String>
    ) = withContext(Dispatchers.IO) {

        val manifestEntry = ZipEntry("MANIFEST.txt")
        zipOut.putNextEntry(manifestEntry)

        val manifestContent = StringBuilder()
        manifestContent.appendLine("# QReport Document Archive Manifest")
        manifestContent.appendLine("# Generated: ${Clock.System.now()}")
        manifestContent.appendLine("# Format: filepath=sha256hash")
        manifestContent.appendLine()

        for ((filePath, hash) in documentHashes) {
            manifestContent.appendLine("${File(filePath).name}=$hash")
        }

        zipOut.write(manifestContent.toString().toByteArray())
        zipOut.closeEntry()
    }
}
