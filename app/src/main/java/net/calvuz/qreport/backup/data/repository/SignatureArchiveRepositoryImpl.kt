package net.calvuz.qreport.backup.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.backup.data.model.ArchiveProgress
import net.calvuz.qreport.backup.data.model.ExtractionProgress
import net.calvuz.qreport.backup.domain.model.BackupValidationResult
import net.calvuz.qreport.backup.domain.model.SignatureBackupInfo
import net.calvuz.qreport.backup.domain.model.SignatureManifest
import net.calvuz.qreport.backup.domain.repository.SignatureArchiveRepository
import net.calvuz.qreport.ti.domain.model.SignatureType
import net.calvuz.qreport.ti.domain.repository.SignatureFileRepository
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
 * Implementation of SignatureArchiveRepository
 *
 * Creates/extracts signature archives following PhotoArchiveRepositoryImpl pattern:
 * - ZIP compression
 * - SHA256 hash verification
 * - Progress tracking
 * - Archive validation
 */
@Singleton
class SignatureArchiveRepositoryImpl @Inject constructor(
    private val signatureFileRepository: SignatureFileRepository
) : SignatureArchiveRepository {

    companion object {
        private const val BUFFER_SIZE = 8192
        private const val MAX_SINGLE_FILE_SIZE = 10 * 1024 * 1024L // 10MB per signature
        private const val MAX_TOTAL_ARCHIVE_SIZE = 500 * 1024 * 1024L // 500MB total
    }

    // ===== CREATE SIGNATURE ARCHIVE =====

    override suspend fun createSignatureArchive(
        outputPath: String
    ): Flow<ArchiveProgress> = flow {

        try {
            Timber.d("Signature archive creation begin - output: $outputPath")

            // 1. Get all signatures from repository
            val signaturesResult = signatureFileRepository.getAllSignatures()

            when (signaturesResult) {
                is QrResult.Error -> {
                    emit(ArchiveProgress.Error("Failed to get signatures: ${signaturesResult.error}"))
                    return@flow
                }
                is QrResult.Success -> {
                    val allSignatures = signaturesResult.data

                    if (allSignatures.isEmpty()) {
                        Timber.d("No signatures to archive")
                        emit(ArchiveProgress.Completed(outputPath, 0, 0L))
                        return@flow
                    }

                    // 2. Filter existing files
                    val existingSignatures = allSignatures.filter { signature ->
                        File(signature.path).exists()
                    }

                    if (existingSignatures.isEmpty()) {
                        Timber.d("No existing signature files found")
                        emit(ArchiveProgress.Completed(outputPath, 0, 0L))
                        return@flow
                    }

                    val totalFiles = existingSignatures.size
                    Timber.d("Archiving $totalFiles signature files")

                    // 3. Create output directory if needed
                    val outputFile = File(outputPath)
                    outputFile.parentFile?.mkdirs()

                    var processedFiles = 0
                    var totalSizeBytes = 0L
                    val signatureHashes = mutableMapOf<String, String>()

                    // 4. Create ZIP archive
                    ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zipOut ->

                        for (signature in existingSignatures) {
                            // Progress update
                            val currentProgress = processedFiles.toFloat() / totalFiles
                            emit(
                                ArchiveProgress.InProgress(
                                    processedFiles = processedFiles,
                                    totalFiles = totalFiles,
                                    currentFile = signature.name,
                                    progress = currentProgress
                                )
                            )

                            val signatureFile = File(signature.path)
                            if (signatureFile.exists()) {
                                val fileSizeBytes = signatureFile.length()

                                // Skip empty files
                                if (fileSizeBytes == 0L) {
                                    Timber.w("Signature file ${signature.name} has zero size, skipping")
                                    continue
                                }

                                // Skip files too large
                                if (fileSizeBytes > MAX_SINGLE_FILE_SIZE) {
                                    Timber.w("Signature file ${signature.name} too large, skipping")
                                    continue
                                }

                                // Add to ZIP with entry path preserving intervention grouping
                                val entryPath = "${signature.interventionId}/${signature.name}"
                                val fileHash = addFileToZip(
                                    zipOut = zipOut,
                                    file = signatureFile,
                                    entryPath = entryPath
                                )

                                signatureHashes[signature.path] = fileHash
                                totalSizeBytes += fileSizeBytes
                                processedFiles++

                                Timber.v("Added signature: ${signature.name}, hash=${fileHash.take(8)}...")
                            }

                            // Check total archive size limit
                            if (totalSizeBytes > MAX_TOTAL_ARCHIVE_SIZE) {
                                Timber.w("Archive size limit reached")
                                break
                            }
                        }

                        // 5. Add manifest to ZIP
                        addHashManifestToZip(zipOut, signatureHashes)
                    }

                    // 6. Verify created archive
                    val archiveSize = outputFile.length()
                    Timber.d("Signature archive created: $processedFiles files, ${archiveSize / 1024}KB")

                    emit(ArchiveProgress.Completed(outputPath, processedFiles, archiveSize))
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Signature archive creation failed")
            emit(ArchiveProgress.Error("Archive creation failed: ${e.message}"))
        }
    }

    // ===== EXTRACT SIGNATURE ARCHIVE =====

    override suspend fun extractSignatureArchive(
        archivePath: String,
        outputDir: String
    ): Flow<ExtractionProgress> = flow {

        try {
            Timber.d("Signature archive extraction begin - archive: $archivePath, output: $outputDir")

            val archiveFile = File(archivePath)
            if (!archiveFile.exists()) {
                emit(ExtractionProgress.Error("Archive file not found: $archivePath"))
                return@flow
            }

            // Create output directory
            val outputDirectory = File(outputDir)
            outputDirectory.mkdirs()

            var extractedFiles = 0
            var totalFiles = 0

            // Count entries first
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

            // Extract files
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

            Timber.d("Signature extraction completed: $extractedFiles files")
            emit(ExtractionProgress.Completed(outputDir, extractedFiles))

        } catch (e: Exception) {
            Timber.e(e, "Signature archive extraction failed")
            emit(ExtractionProgress.Error("Extraction failed: ${e.message}"))
        }
    }

    // ===== GENERATE MANIFEST =====

    override suspend fun generateSignatureManifest(): SignatureManifest {
        return try {
            val signatureBackupInfos = mutableListOf<SignatureBackupInfo>()
            var totalSizeBytes = 0L
            var technicianCount = 0
            var customerCount = 0

            when (val signaturesResult = signatureFileRepository.getAllSignatures()) {
                is QrResult.Error -> {
                    Timber.e("Failed to get signatures for manifest")
                    SignatureManifest.empty()
                }
                is QrResult.Success -> {
                    for (signature in signaturesResult.data) {
                        val signatureFile = File(signature.path)
                        if (signatureFile.exists()) {
                            val fileHash = withContext(Dispatchers.IO) {
                                calculateFileHash(signatureFile)
                            }

                            val signatureInfo = SignatureBackupInfo(
                                interventionId = signature.interventionId,
                                signatureType = signature.signatureType.name,
                                fileName = signature.name,
                                relativePath = "${signature.interventionId}/${signature.name}",
                                sizeBytes = signature.size,
                                sha256Hash = fileHash
                            )

                            signatureBackupInfos.add(signatureInfo)
                            totalSizeBytes += signature.size

                            // Count by type
                            when (signature.signatureType) {
                                SignatureType.TECHNICIAN -> technicianCount++
                                SignatureType.CUSTOMER -> customerCount++
                            }
                        }
                    }

                    SignatureManifest(
                        totalSignatures = signatureBackupInfos.size,
                        totalSizeMB = totalSizeBytes / (1024.0 * 1024.0),
                        signatures = signatureBackupInfos,
                        technicianCount = technicianCount,
                        customerCount = customerCount
                    )
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Error generating signature manifest")
            SignatureManifest.empty()
        }
    }

    // ===== VALIDATE INTEGRITY =====

    override suspend fun validateSignatureIntegrity(manifest: SignatureManifest): BackupValidationResult {
        return try {
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()
            var validatedSignatures = 0

            for (signatureInfo in manifest.signatures) {
                // Get signature directory from repository
                val signatureDirResult = signatureFileRepository.getSignaturesDirectory()

                when (signatureDirResult) {
                    is QrResult.Error -> {
                        errors.add("Cannot access signatures directory")
                        continue
                    }
                    is QrResult.Success -> {
                        val signaturePath = "${signatureDirResult.data}/${signatureInfo.relativePath}"
                        val signatureFile = File(signaturePath)

                        if (!signatureFile.exists()) {
                            errors.add("Firma mancante: ${signatureInfo.fileName}")
                            continue
                        }

                        // Size check
                        if (signatureFile.length() != signatureInfo.sizeBytes) {
                            warnings.add(
                                "Dimensione firma diversa: ${signatureInfo.fileName} " +
                                        "(attesa: ${signatureInfo.sizeBytes}, trovata: ${signatureFile.length()})"
                            )
                        }

                        // Hash check
                        withContext(Dispatchers.IO) {
                            val actualHash = calculateFileHash(signatureFile)
                            if (actualHash != signatureInfo.sha256Hash) {
                                errors.add("Hash firma non valido: ${signatureInfo.fileName}")
                            } else {
                                validatedSignatures++
                            }
                        }
                    }
                }
            }

            Timber.d("Validated signatures: $validatedSignatures/${manifest.totalSignatures}")

            if (manifest.totalSignatures > 0 && validatedSignatures < manifest.totalSignatures * 0.9) {
                warnings.add("Meno del 90% delle firme hanno superato la validazione")
            }

            BackupValidationResult(
                isValid = errors.isEmpty(),
                errors = errors,
                warnings = warnings
            )

        } catch (e: Exception) {
            Timber.e(e, "Signature integrity validation failed")
            BackupValidationResult.invalid(listOf("Validazione fallita: ${e.message}"))
        }
    }

    // ===== HELPER METHODS =====

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
        signatureHashes: Map<String, String>
    ) = withContext(Dispatchers.IO) {

        val manifestEntry = ZipEntry("MANIFEST.txt")
        zipOut.putNextEntry(manifestEntry)

        val manifestContent = StringBuilder()
        manifestContent.appendLine("# QReport Signature Archive Manifest")
        manifestContent.appendLine("# Generated: ${Clock.System.now()}")
        manifestContent.appendLine("# Format: filepath=sha256hash")
        manifestContent.appendLine()

        for ((filePath, hash) in signatureHashes) {
            manifestContent.appendLine("${File(filePath).name}=$hash")
        }

        zipOut.write(manifestContent.toString().toByteArray())
        zipOut.closeEntry()
    }
}

/**
 * Extension function for empty SignatureManifest
 */
fun SignatureManifest.Companion.empty(): SignatureManifest {
    return SignatureManifest(
        totalSignatures = 0,
        totalSizeMB = 0.0,
        signatures = emptyList(),
        technicianCount = 0,
        customerCount = 0
    )
}

/*
=============================================================================
                        SIGNATURE ARCHIVE STRUCTURE
=============================================================================

ZIP Archive Structure:
signatures.zip
├── {interventionId1}/
│   ├── TECH_INT001_1234567890.png
│   └── CUST_INT001_1234567891.png
├── {interventionId2}/
│   └── TECH_INT002_1234567892.png
└── MANIFEST.txt
    # TECH_INT001_1234567890.png=a1b2c3d4e5f6...
    # CUST_INT001_1234567891.png=f6e5d4c3b2a1...

=============================================================================
*/