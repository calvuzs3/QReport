package net.calvuz.qreport.data.repository.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import net.calvuz.qreport.data.backup.model.ArchiveProgress
import net.calvuz.qreport.data.backup.model.ExtractionProgress
import net.calvuz.qreport.data.local.dao.PhotoDao
import net.calvuz.qreport.domain.model.backup.*
import net.calvuz.qreport.domain.repository.backup.PhotoArchiveRepository
import net.calvuz.qreport.util.SizeUtils.getFormattedSize
import timber.log.Timber
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.iterator

/**
 * Creation/Extraction for Photos' archives with:
 * - ZIP compression
 * - SHA256 hash
 * - Progress tracking
 * - Option thumbnails
 * - Archive Validation
 */

@Singleton
class PhotoArchiveRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoDao: PhotoDao
) : PhotoArchiveRepository {

    companion object {
        private const val BUFFER_SIZE = 8192
        private const val MAX_SINGLE_FILE_SIZE = 50 * 1024 * 1024
        private const val MAX_TOTAL_ARCHIVE_SIZE = Long.MAX_VALUE - 1024 // 5000 * 1024 * 1024
    }

    // ===== CREATE PHOTO ARCHIVE =====

    /**
     * Create a photos' ZIP archive with progress tracking
     */
    override suspend fun createPhotoArchive(
        outputPath: String,
        includesThumbnails: Boolean
    ): Flow<ArchiveProgress> = flow {

        try {
            Timber.v(" Photo Archive creation begin\n- output path: $outputPath\n- include thumbnails: $includesThumbnails")

            // 1. Load photos' list from db
            val allPhotos = withContext(Dispatchers.IO) {
                photoDao.getAllForBackup()
            }

            if (allPhotos.isEmpty()) {
                emit(ArchiveProgress.Completed(outputPath, 0, 0L))
                return@flow
            }

            // 2. Filtering existing photos and summarize totals
            val existingPhotos = allPhotos.filter { photo ->
                val photoFile = File(photo.filePath)
                val thumbnailFile = if (includesThumbnails && photo.thumbnailPath != null) {
                    File(photo.thumbnailPath)
                } else null

                photoFile.exists() && (thumbnailFile?.exists() != false)
            }

            val totalFiles = if (includesThumbnails) {
                existingPhotos.size + existingPhotos.count { it.thumbnailPath != null }
            } else {
                existingPhotos.size
            }

            if (existingPhotos.isEmpty()) {
                emit(ArchiveProgress.Completed(outputPath, 0, 0L))
                return@flow
            }

            Timber.d("- photos: ${existingPhotos.size}\n- total files: $totalFiles")

            // 3. Crea directory output se necessaria
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()

            var processedFiles = 0
            var totalSizeBytes = 0L
            val photoHashes = mutableMapOf<String, String>()

            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zipOut ->

                // Rename index as _ because it is not used
                for ((_, photo) in existingPhotos.withIndex()) {

                    // Progress update
                    val currentProgress = processedFiles.toFloat() / totalFiles
                    emit(
                        ArchiveProgress.InProgress(
                            processedFiles = processedFiles,
                            totalFiles = totalFiles,
                            currentFile = photo.fileName,
                            progress = currentProgress
                        )
                    )

                    // 4. Add photos
                    val photoFile = File(photo.filePath)
                    if (photoFile.exists()) {

                        // DEBUG----
                        val fileSizeBytes = photoFile.length()
                        Timber.v("PHOTO DEBUG: ${photo.fileName}")
                        Timber.v("   Path: ${photo.filePath}")
                        Timber.v("   Size: ${fileSizeBytes.getFormattedSize()}")
                        Timber.v("   Exists: ${photoFile.exists()}")
                        Timber.v("   Readable: ${photoFile.canRead()}")

                        if (fileSizeBytes == 0L) {
                            Timber.w("File ${photo.fileName} has ZERO size!")
                            continue  // ✅ Salta file vuoti
                        }

                        if (fileSizeBytes > MAX_SINGLE_FILE_SIZE) {
                            Timber.w("Photo ${photo.fileName} too big (${fileSizeBytes.getFormattedSize()}), skipped")
                            continue
                        }
                        // ----DEBUG

                        // Verifica dimensione file
                        if (fileSizeBytes > MAX_SINGLE_FILE_SIZE) {
                            Timber.w("Photo ${photo.fileName} too big (${fileSizeBytes.getFormattedSize()}), skipped")
                            continue
                        }

                        // DEBUG
                        Timber.v("Adding to ZIP...")

                        val photoHash = addFileToZip(
                            zipOut = zipOut,
                            file = photoFile,
                            entryPath = photo.fileName
//                          entryPath = "photos/${photo.checkItemId}/${photo.fileName}"
                        )

                        photoHashes[photo.filePath] = photoHash
                        totalSizeBytes += photoFile.length()
                        processedFiles++


                        // DEBUG dopo aggiunta
                        Timber.v(
                            "✓ Added: \n- filename=${photo.fileName}, \n- hash=${
                                photoHash.take(
                                    8
                                )
                            }..., \ntotalSize=${totalSizeBytes.getFormattedSize()}"
                        )
                    }

                    // 5. Aggiungi thumbnail se richiesto e disponibile
                    if (includesThumbnails && photo.thumbnailPath != null) {
                        val thumbnailFile = File(photo.thumbnailPath)
                        if (thumbnailFile.exists()) {

                            // DEBUG----
                            val thumbSizeBytes = thumbnailFile.length()
                            Timber.v("THUMBNAIL DEBUG: ${photo.fileName}")
                            Timber.v("   Thumb path: ${photo.thumbnailPath}")
                            Timber.v("   Thumb size: ${thumbSizeBytes.getFormattedSize()}")

                            if (thumbSizeBytes == 0L) {
                                Timber.w("Thumbnail ${photo.fileName} has ZERO size!")
                                continue  // ✅ Salta thumbnails vuoti
                            }
                            // ----DEBUG


                            val thumbnailHash = addFileToZip(
                                zipOut = zipOut,
                                file = thumbnailFile,
                                entryPath = "thumbnails/thumb_${photo.fileName}"
//                              entryPath = "thumbnails/${photo.checkItemId}/thumb_${photo.fileName}"
                            )

                            photoHashes[photo.thumbnailPath] = thumbnailHash
                            totalSizeBytes += thumbnailFile.length()
                            processedFiles++

                            Timber.v(
                                "✓ Thumbnail added: \n- filename=${photo.fileName}\n- hash=${
                                    thumbnailHash.take(
                                        8
                                    )
                                }, \ntotalSize=${totalSizeBytes.getFormattedSize()}"
                            )
                        }
                    }


                    // 6. Verifica dimensione totale archivio
                    if (totalSizeBytes > MAX_TOTAL_ARCHIVE_SIZE) {
                        Timber.w("Archive dimension limit reached (${totalSizeBytes.getFormattedSize()})")
                        break
                    }
                }

                // 7. Aggiungi manifesto hash
                addHashManifestToZip(zipOut, photoHashes)
            }

            // 2. ✅ DEBUG Total Size - Modifica il log finale (riga ~173)
            val finalSize = totalSizeBytes
            Timber.d("PHOTO ARCHIVE FINAL DEBUG:")
            Timber.d("   Processed files: $processedFiles")
            Timber.d("   Total size: ${totalSizeBytes.getFormattedSize()}")
            Timber.d("   Output path: $outputPath")

            //val outputFile = File(outputPath)
            if (outputFile.exists()) {
                val zipSizeBytes = outputFile.length()
                Timber.d("ZIP file: size=${zipSizeBytes.getFormattedSize()}")

                if (zipSizeBytes == 0L) {
                    Timber.e("ZIP file is EMPTY! ($outputPath)")
                }
            } else {
                Timber.e("ZIP file NOT CREATED! ($outputPath)")
            }

            Timber.d("Photos archive: n.files=$processedFiles, size=${finalSize.getFormattedSize()}")

            emit(
                ArchiveProgress.Completed(
                    archivePath = outputPath,
                    totalFiles = processedFiles,
                    totalSize = finalSize
                )
            )

        } catch (e: Exception) {
            Timber.e(e, "Photo archive creation failed")
            emit(ArchiveProgress.Error("Creazione archivio fallita: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)

    // ===== EXTRACT PHOTO ARCHIVE =====

    /**
     * Photo archive extraction with progress tracking
     */
    override suspend fun extractPhotoArchive(
        archivePath: String,
        outputDir: String
    ): Flow<ExtractionProgress> = flow {

        try {
            Timber.v(" Photo Archive extraction begin\n- output path: $outputDir\n- input path: $archivePath")

            val archiveFile = File(archivePath)
            if (!archiveFile.exists()) {
                emit(ExtractionProgress.Error("File archivio non trovato: $archivePath"))
                return@flow
            }

            val outputDirectory = File(outputDir)
//            outputDirectory.mkdirs()

            // 1. File sum in the archive
            var totalFiles = 0
            ZipInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { zipIn ->
                while (zipIn.nextEntry != null) {
                    totalFiles++
                }
            }

            if (totalFiles == 0) {
                emit(ExtractionProgress.Completed(outputDir, 0))
                return@flow
            }

            Timber.v("Files: $totalFiles")

            // 2. File extraction with progress tracking
            var extractedFiles = 0
            val extractedHashes = mutableMapOf<String, String>()

            ZipInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry

                while (entry != null) {
                    val entryName = entry.name

                    // Progress update
                    val currentProgress = extractedFiles.toFloat() / totalFiles
                    emit(
                        ExtractionProgress.InProgress(
                            extractedFiles = extractedFiles,
                            totalFiles = totalFiles,
                            currentFile = entryName,
                            progress = currentProgress
                        )
                    )

                    if (!entry.isDirectory && !entryName.endsWith("MANIFEST.txt")) {
                        // 3. Estrai file normale
                        val outputFile = File(outputDirectory, entryName)
//                        outputFile.parentFile?.mkdirs()

                        val extractedHash = extractFileFromZip(zipIn, outputFile)
                        extractedHashes[outputFile.absolutePath] = extractedHash

                        extractedFiles++
                        Timber.v("✓ Extracted: $entryName")

                    } else if (entryName.endsWith("MANIFEST.txt")) {

                        // 4. Leggi manifesto hash per validazione
                        val manifestContent = zipIn.readBytes().toString(Charsets.UTF_8)
                        Timber.d("Manifest loaded (${manifestContent.lines().size} entries)")
                    }

                    entry = zipIn.nextEntry
                }
            }
//            }

            // 5. Hash validation from Manifest (if manifest exists)
            // TODO: Implementa validazione hash estratti vs manifesto

            Timber.v("Extraction completed ($extractedFiles files")

            emit(
                ExtractionProgress.Completed(
                    outputDir = outputDir,
                    extractedFiles = extractedFiles
                )
            )

        } catch (e: Exception) {
            Timber.e(e, "Photo archive extraction failed")
            emit(ExtractionProgress.Error("Estrazione archivio fallita: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)

    // ===== GENERATE PHOTO MANIFEST =====

    /**
     * Generate Photo Manifest with photos in the db
     */
    override suspend fun generatePhotoManifest(): PhotoManifest {
        return try {
            val allPhotos = withContext(Dispatchers.IO) {
                photoDao.getAllForBackup()
            }

            var totalSizeBytes = 0L
            val photoBackupInfos = mutableListOf<PhotoBackupInfo>()

            for (photo in allPhotos) {
                val photoFile = File(photo.filePath)

                if (photoFile.exists()) {
                    val photoHash = withContext(Dispatchers.IO) {
                        calculateFileHash(photoFile)
                    }

                    val photoInfo = PhotoBackupInfo(
                        checkItemId = photo.checkItemId,
                        fileName = photo.fileName,
                        relativePath = "photos/${photo.checkItemId}/${photo.fileName}",
                        sizeBytes = photo.fileSize,
                        sha256Hash = photoHash,
                        hasThumbnail = photo.thumbnailPath?.let { File(it).exists() } == true
                    )

                    photoBackupInfos.add(photoInfo)
                    totalSizeBytes += photo.fileSize
                }
            }

            PhotoManifest(
                totalPhotos = photoBackupInfos.size,
                totalSize = totalSizeBytes,
                photos = photoBackupInfos,
                includesThumbnails = photoBackupInfos.any { it.hasThumbnail }
            )

        } catch (e: Exception) {
            Timber.e(e, "Error generating Photo Manifest")
            PhotoManifest.empty()
        }
    }

    // ===== VALIDATE PHOTO INTEGRITY =====

    /**
     * Validate photo integrity with its hash
     */
    override suspend fun validatePhotoIntegrity(manifest: PhotoManifest): BackupValidationResult {
        return try {
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()
            var validatedPhotos = 0

            for (photoInfo in manifest.photos) {
                val photoPath = "${context.filesDir}/photos/${photoInfo.relativePath}"
                val photoFile = File(photoPath)

                if (!photoFile.exists()) {
                    errors.add("Foto mancante: ${photoInfo.fileName}")
                    continue
                }

                // Dimension check
                if (photoFile.length() != photoInfo.sizeBytes) {
                    warnings.add(
                        "Dimensione foto diversa: ${photoInfo.fileName} " +
                                "(attesa: ${photoInfo.sizeBytes}, trovata: ${photoFile.length()})"
                    )
                }

                // Hash check
                withContext(Dispatchers.IO) {
                    val actualHash = calculateFileHash(photoFile)
                    if (actualHash != photoInfo.sha256Hash) {
                        errors.add("Hash foto non valido: ${photoInfo.fileName}")
                    } else {
                        validatedPhotos++
                    }
                }
            }

            Timber.v("Valid photos: $validatedPhotos/${manifest.totalPhotos}")

            if (validatedPhotos < manifest.totalPhotos * 0.9) {
                warnings.add("Meno del 90% delle foto hanno superato la validazione")
            }

            BackupValidationResult(
                isValid = errors.isEmpty(),
                errors = errors,
                warnings = warnings
            )

        } catch (e: Exception) {
            Timber.e(e, "Photo integrity validation failed")
            BackupValidationResult.invalid(listOf("Validazione fallita: ${e.message}"))
        }
    }

    // ===== HELPER METHODS =====

    /**
     * Add file to ZIP
     * @return hash SHA256
     */
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

    /**
     * Extract file from
     * @return hash SHA256
     */
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

    /**
     * Calculate hash SHA256 of the file
     */
    private suspend fun calculateFileHash(file: File): String = withContext(Dispatchers.IO) {
        calculateFileHashWhileReading(file) { _, _ -> }
    }

    /**
     * Calculate hash SHA256 of the file with callback
     */
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

    /**
     * Add Hash Manifest to ZIP
     */
    private suspend fun addHashManifestToZip(
        zipOut: ZipOutputStream,
        photoHashes: Map<String, String>
    ) = withContext(Dispatchers.IO) {

        val manifestEntry = ZipEntry("MANIFEST.txt")
        zipOut.putNextEntry(manifestEntry)

        val manifestContent = StringBuilder()
        manifestContent.appendLine("# QReport Photo Archive Manifest")
        manifestContent.appendLine("# Generated: ${Clock.System.now()}")
        manifestContent.appendLine("# Format: filepath=sha256hash")
        manifestContent.appendLine()

        for ((filePath, hash) in photoHashes) {
            manifestContent.appendLine("${File(filePath).name}=$hash")
        }

        zipOut.write(manifestContent.toString().toByteArray())
        zipOut.closeEntry()
    }
}

/*
=============================================================================
                            PHOTO ARCHIVE STRUCTURE
=============================================================================

ZIP Archive Structure:
photos_archive.zip
├── photos/
│   ├── {checkItemId1}/
│   │   ├── photo_001.jpg
│   │   └── photo_002.jpg
│   └── {checkItemId2}/
│       └── photo_003.jpg
├── thumbnails/                    # Optional
│   ├── {checkItemId1}/
│   │   ├── thumb_photo_001.jpg
│   │   └── thumb_photo_002.jpg
│   └── {checkItemId2}/
│       └── thumb_photo_003.jpg
└── MANIFEST.txt                   # Hash manifest
    # photo_001.jpg=a1b2c3d4e5f6...
    # photo_002.jpg=f6e5d4c3b2a1...
    # thumb_photo_001.jpg=1234567890ab...

=============================================================================
*/