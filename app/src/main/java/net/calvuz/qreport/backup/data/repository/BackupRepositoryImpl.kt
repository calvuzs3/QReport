package net.calvuz.qreport.backup.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import net.calvuz.qreport.backup.data.BackupJsonSerializer
import net.calvuz.qreport.backup.data.model.ArchiveProgress
import net.calvuz.qreport.backup.domain.model.BackupInfo
import net.calvuz.qreport.backup.data.model.ExtractionProgress
import net.calvuz.qreport.backup.domain.model.BackupData
import net.calvuz.qreport.backup.domain.model.BackupMetadata
import net.calvuz.qreport.backup.domain.model.enum.BackupMode
import net.calvuz.qreport.backup.presentation.ui.model.BackupProgress
import net.calvuz.qreport.backup.domain.model.enum.BackupType
import net.calvuz.qreport.backup.domain.model.BackupValidationResult
import net.calvuz.qreport.backup.domain.model.DeviceInfo
import net.calvuz.qreport.backup.domain.model.PhotoManifest
import net.calvuz.qreport.backup.presentation.ui.model.RestoreProgress
import net.calvuz.qreport.backup.domain.model.enum.RestoreStrategy
import net.calvuz.qreport.backup.domain.repository.BackupFileRepository
import net.calvuz.qreport.backup.domain.repository.DatabaseExportRepository
import net.calvuz.qreport.backup.domain.repository.BackupRepository
import net.calvuz.qreport.backup.domain.repository.PhotoArchiveRepository
import net.calvuz.qreport.app.app.domain.AppVersionInfo
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.backup.domain.model.SignatureManifest
import net.calvuz.qreport.backup.domain.repository.SignatureArchiveRepository
import net.calvuz.qreport.settings.domain.repository.SettingsRepository
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates all backup system components (Database, Photos, Settings).
 */

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val appVersionInfo: AppVersionInfo,
    private val databaseExportRepository: DatabaseExportRepository,
    private val photoArchiveRepository: PhotoArchiveRepository,
    private val settingsRepository: SettingsRepository,
    private val jsonSerializer: BackupJsonSerializer,
    private val backupFileRepo: BackupFileRepository,
    private val signatureArchiveRepository: SignatureArchiveRepository

) : BackupRepository {

    /**
     * Create a full backup
     */
    override suspend fun createFullBackup(
        includePhotos: Boolean,
        includeThumbnails: Boolean,
        backupMode: BackupMode,
        description: String?
    ): Flow<BackupProgress> = flow {

        emit(BackupProgress.InProgress("Full backup start", 0f))

        try {
            // ID CREATION
            val backupId = UUID.randomUUID().toString()
            var photoManifest = PhotoManifest.empty()
            var photoError: String? = null
            val startTime = Clock.System.now()

            var signatureManifest = SignatureManifest.empty()
            var signatureError: String? = null


            // Select a PATH
            val archivePath = when (val result = backupFileRepo.generateBackupPath(backupId)) {
                is QrResult.Success -> result.data
                is QrResult.Error -> {
                    emit(BackupProgress.Error("Failed to generate backup path"))
                    return@flow
                }
            }
            // Select a Photos ArchivePath
            val photoArchivePath = File(archivePath, "photos.zip").absolutePath

            // Selcect a Signatures ArchivePath
            val signatureArchivePath = File(archivePath, "signatures.zip").absolutePath

            // 1. Database validation
            //
            Timber.d("1. DB validation")
            emit(BackupProgress.InProgress("1. Database validation", 0.05f))
            val validation = databaseExportRepository.validateDatabaseIntegrity()
            if (!validation.isValid) {
                emit(BackupProgress.Error("- Database is invalid: ${validation.issues.joinToString()}"))
            } else {
                if (validation.warnings.isNotEmpty())
                    for (w in validation.warnings)
                        Timber.w("Backup warnings: $w")

                emit(BackupProgress.InProgress("- Database is valid", 0.09f))

                // 2. Export database
                //
                Timber.d("2. Export DB")
                emit(BackupProgress.InProgress("2. Export DB", 0.1f, currentTable = "Tutte"))
                val databaseBackup = databaseExportRepository.exportAllTables()
                emit(
                    BackupProgress.InProgress(
                        "- Database exported",
                        0.3f,
                        processedRecords = databaseBackup.getTotalRecordCount(),
                        totalRecords = databaseBackup.getTotalRecordCount()
                    )
                )

                // 3. Settings backup
                //
                Timber.d("3. Export settings")
                emit(BackupProgress.InProgress("3. Export settings", 0.3f))
                val settingsBackup = settingsRepository.exportSettings()
                emit(
                    BackupProgress.InProgress(
                        "- Settings exported", 0.35f
                    )
                )

                // 4. Backup foto (se richiesto)
                //
                if (includePhotos) {
                    Timber.d("4. Export photos")
                    emit(BackupProgress.InProgress("4. Export photos", 0.4f))

                    Timber.d("PhotoPath = $photoArchivePath")

                    photoArchiveRepository.createPhotoArchive(
                        outputPath = photoArchivePath,
                        includesThumbnails = includeThumbnails
                    ).collect { progress ->
                        when (progress) {
                            is ArchiveProgress.InProgress -> {
                                emit(
                                    BackupProgress.InProgress(
                                        "- Photos backup: ${progress.processedFiles}/${progress.totalFiles}",
                                        0.4f + (progress.progress * 0.4f),
                                        currentTable = "Photos",
                                        processedRecords = progress.processedFiles,
                                        totalRecords = progress.totalFiles
                                    )
                                )
                            }

                            is ArchiveProgress.Completed -> {
                                photoManifest = photoArchiveRepository.generatePhotoManifest()
                            }

                            is ArchiveProgress.Error -> {
                                photoError = progress.message
                            }
                        }
                    }
                }


                // Photo export error check
                //
                if (photoError != null) {
                    emit(BackupProgress.Error("Export photos failed: $photoError"))
                } else {
                    emit(BackupProgress.InProgress("- Photos exported", 0.8f))
                    // 5. Metadata creation
                    //
                    Timber.d("5. Metadata creation")
                    emit(BackupProgress.InProgress("5. Metadata creation", 0.85f))
                    val deviceInfo = DeviceInfo.current()
                    val appVersion = appVersionInfo.appVersion
                    val databaseVersion = appVersionInfo.databaseVersion

                    val metadata = BackupMetadata.create(
                        id = backupId,
                        appVersion = appVersion,
                        databaseVersion = databaseVersion,
                        deviceInfo = deviceInfo,
                        backupType = BackupType.FULL,
                        totalSize = 0L, // Calcolato dopo
                        description = description
                    )


                    // 4b. Backup signatures
                    Timber.d("4b. Export signatures")
                    emit(BackupProgress.InProgress("4b. Export signatures", 0.75f))

                    signatureArchiveRepository.createSignatureArchive(
                        outputPath = signatureArchivePath
                    ).collect { progress ->
                        when (progress) {
                            is ArchiveProgress.InProgress -> {
                                emit(
                                    BackupProgress.InProgress(
                                        "- Signatures backup: ${progress.processedFiles}/${progress.totalFiles}",
                                        0.75f + (progress.progress * 0.05f),
                                        currentTable = "Signatures",
                                        processedRecords = progress.processedFiles,
                                        totalRecords = progress.totalFiles
                                    )
                                )
                            }

                            is ArchiveProgress.Completed -> {
                                signatureManifest =
                                    signatureArchiveRepository.generateSignatureManifest()
                            }

                            is ArchiveProgress.Error -> {
                                signatureError = progress.message
                            }
                        }
                    }

                    // Signature export error check
                    if (signatureError != null) {
                        Timber.w("Signature export failed: $signatureError (continuing without signatures)")
                        // Non-blocking: continue without signatures
                    }


                    // 6. Assemblaggio backup finale
                    //
                    emit(BackupProgress.InProgress("Finalizing backup", 0.9f))
                    val backupData = BackupData(
                        metadata = metadata,
                        database = databaseBackup,
                        settings = settingsBackup,
                        photoManifest = photoManifest,
                        signatureManifest = signatureManifest
                    )

                    // 7. Calcolo checksum
                    //
                    emit(BackupProgress.InProgress("Checking checksum", 0.95f))
                    val checksumResult = jsonSerializer.calculateBackupChecksum(backupData)
                    if (checksumResult.isFailure) {
                        emit(BackupProgress.Error("Checksum check failed"))
                    } else {
                        val finalBackupData = backupData.copy(
                            metadata = metadata.copy(
                                checksum = checksumResult.getOrThrow(),
                                totalSize = calculateTotalBackupSize(backupData, photoManifest)
                            )
                        )

                        // 8. Salvataggio finale
                        //
                        Timber.d("Saving Backup in $archivePath")
                        emit(BackupProgress.InProgress("Saving Backup", 0.95f))
                        val backupPath = when (val result =
                            backupFileRepo.saveBackup(finalBackupData, backupMode, archivePath)) {
                            is QrResult.Success -> result.data
                            is QrResult.Error -> {
                                emit(BackupProgress.Error("Failed to save backup"))
                                return@flow
                            }
                        }

                        val duration = Clock.System.now() - startTime

                        emit(
                            BackupProgress.Completed(
                                backupId = backupId,
                                backupPath = backupPath,
                                totalSize = finalBackupData.metadata.totalSize,
                                duration = duration,
                                tablesBackedUp = 9
                            )
                        )
                    }
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Backup creation failed")
            emit(BackupProgress.Error("Backup failed: ${e.message}", e))
        }
    }

    /**
     * Restore from backup
     */
    override suspend fun restoreFromBackup(
        dirPath: String,
        backupPath: String,
        strategy: RestoreStrategy
    ): Flow<RestoreProgress> = flow {

        emit(RestoreProgress.InProgress("Loading backup", 0f))

        try {
            // 1. Caricamento backup
            //
            val backupData = when (val result = backupFileRepo.loadBackup(backupPath)) {
                is QrResult.Success -> result.data
                is QrResult.Error -> {
                    emit(RestoreProgress.Error("Failed to load backup"))
                    return@flow
                }
            }

            // 2. Validazione backup
            //
            emit(RestoreProgress.InProgress("Validating backup", 0.05f))
            val validation = validateBackup(backupPath)

            if (!validation.isValid) {
                emit(RestoreProgress.Error("Backup is invalid: ${validation.issues.joinToString()}"))
            } else {

                // 3. Checksum check
                //
                emit(RestoreProgress.InProgress("Checking checksum", 0.1f))
                val checksumValid = jsonSerializer.verifyBackupIntegrity(
                    backupData
                )

                if (!checksumValid) {
                    emit(RestoreProgress.Error("Checksum invalid - possible corruption"))
                } else {

                    // 4. DB restoration
                    //
                    emit(RestoreProgress.InProgress("Restoring database", 0.2f))
                    val importResult = databaseExportRepository.importAllTables(
                        backupData.database,
                        strategy
                    )

                    importResult.fold(
                        onSuccess = {
                            emit(
                                RestoreProgress.InProgress(
                                    "Database restored",
                                    0.6f,
                                    processedRecords = backupData.database.getTotalRecordCount()
                                )
                            )

                            // 5. Ripristino foto
                            //
                            var photoError: String? = null

                            if (backupData.includesPhotos()) {
                                emit(RestoreProgress.InProgress("Restoring photos", 0.65f))

                                val photoArchivePath =
                                    "${dirPath}/photos.zip" //fileManager.getPhotoArchivePathFromBackup(backupPath)
                                val photosDir =
                                    when (val result = backupFileRepo.getPhotosDirectory()) {
                                        is QrResult.Success -> result.data
                                        is QrResult.Error -> {
                                            emit(RestoreProgress.Error("Failed to get photos directory"))
                                            return@flow

                                        }
                                    }

                                photoArchiveRepository.extractPhotoArchive(
                                    archivePath = photoArchivePath,
                                    outputDir = photosDir
                                ).collect { progress ->
                                    when (progress) {
                                        is ExtractionProgress.InProgress -> {
                                            emit(
                                                RestoreProgress.InProgress(
                                                    "Restoring photos: ${progress.extractedFiles}/${progress.totalFiles}",
                                                    0.65f + (progress.progress * 0.25f),
                                                    currentTable = "Photos",
                                                    processedRecords = progress.extractedFiles,
                                                    totalRecords = progress.totalFiles
                                                )
                                            )
                                        }

                                        is ExtractionProgress.Error -> {
                                            photoError = progress.message
                                        }

                                        is ExtractionProgress.Completed -> {
                                            // Photo extraction completed
                                        }
                                    }
                                }
                            }

                            // 5b. Restore signatures
                            //
                            var signatureError: String? = null

                            if (backupData.includesSignatures()) {
                                emit(RestoreProgress.InProgress("Ripristino firme...", 0.88f))

                                val signatureArchivePath = "${dirPath}/signatures.zip"
                                val signatureArchiveFile = File(signatureArchivePath)

                                if (signatureArchiveFile.exists()) {
                                    // Get signatures directory
                                    val signaturesDir = when (val result =
                                        backupFileRepo.getSignaturesDirectory()) {
                                        is QrResult.Success -> result.data
                                        is QrResult.Error -> {
                                            Timber.w("Failed to get signatures directory, skipping signature restore")
                                            null
                                        }
                                    }

                                    if (signaturesDir != null) {
                                        signatureArchiveRepository.extractSignatureArchive(
                                            archivePath = signatureArchivePath,
                                            outputDir = signaturesDir
                                        ).collect { progress ->
                                            when (progress) {
                                                is ExtractionProgress.InProgress -> {
                                                    emit(
                                                        RestoreProgress.InProgress(
                                                            "Ripristino firme: ${progress.extractedFiles}/${progress.totalFiles}",
                                                            0.88f + (progress.progress * 0.05f),
                                                            currentTable = "Signatures",
                                                            processedRecords = progress.extractedFiles,
                                                            totalRecords = progress.totalFiles
                                                        )
                                                    )
                                                }

                                                is ExtractionProgress.Error -> {
                                                    signatureError = progress.message
                                                    Timber.w("Signature restoration failed: ${progress.message}")
                                                }

                                                is ExtractionProgress.Completed -> {
                                                    Timber.d("Signatures restored: ${progress.extractedFiles} files to $signaturesDir")
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Timber.d("No signatures.zip found in backup, skipping signature restore")
                                }
                            }

                            // Note: signatureError is non-blocking - we continue even if signature restore fails
                            // because the database data is more critical
                            if (signatureError != null) {
                                Timber.w("Signature restore had errors but continuing: $signatureError")
                            }

                            // Check for photos errors
                            if (photoError != null) {
                                emit(RestoreProgress.Error("Photos restoration failed: $photoError"))
                            } else {
                                // 6. Ripristino impostazioni
                                //
                                emit(RestoreProgress.InProgress("Restoring settings", 0.95f))
                                val settingsResult =
                                    settingsRepository.importSettings(backupData.settings)

                                settingsResult.fold(
                                    onSuccess = {
                                        emit(RestoreProgress.Completed(backupData.metadata.id))
                                    },
                                    onFailure = { error ->
                                        Timber.w(error, "Settings restoration failed, continuing..")
                                        emit(RestoreProgress.Completed(backupData.metadata.id))
                                    }
                                )
                            }
                        },
                        onFailure = { error ->
                            emit(RestoreProgress.Error("Database restoration failed: ${error.message}"))
                        }
                    )
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Backup restoration failed")
            emit(RestoreProgress.Error("Backup restoration failed: ${e.message}", e))
        }
    }

    /**
     * Available backup listing
     */
    override suspend fun getAvailableBackups(): List<BackupInfo> {
        return try {
            when (val result = backupFileRepo.listBackups()) {
                is QrResult.Success -> result.data
                is QrResult.Error -> {
                    Timber.e("Failed to list backups: ${result.error}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Available backup listing failed")
            emptyList()
        }
    }

    /**
     * Delete backup
     */
    override suspend fun deleteBackup(backupId: String): Result<Unit> {
        return try {
            when (val result = backupFileRepo.deleteBackupById(backupId)) {
                is QrResult.Success -> Result.success(Unit)
                is QrResult.Error -> Result.failure(Exception("Delete backup failed"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Delete backup failed (id:$backupId)")
            Result.failure(e)
        }
    }

    /**
     * Validate backup
     */
    override suspend fun validateBackup(backupPath: String): BackupValidationResult {
        return try {
            when (val result = backupFileRepo.validateBackup(backupPath)) {
                is QrResult.Success -> result.data
                is QrResult.Error -> BackupValidationResult.invalid(listOf("Backup Validation failed"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Backup Validation failed")
            BackupValidationResult.invalid(listOf("Backup Validation failed: ${e.message}"))
        }
    }

    /**
     * Backup size estimation
     */
    override suspend fun getEstimatedBackupSize(includePhotos: Boolean): Long {
        return try {
            var estimatedSize = 0L

            // Database size (approximately)
            val recordCount = databaseExportRepository.getEstimatedRecordCount()
            estimatedSize += recordCount * 100 // ~100 bytes per record

            // Photos size
            if (includePhotos) {
                val photoManifest = photoArchiveRepository.generatePhotoManifest()
                estimatedSize += (photoManifest.totalSize)
            }

            // Signatures size
            val signatureManifest = signatureArchiveRepository.generateSignatureManifest()
            estimatedSize += (signatureManifest.totalSizeMB * 1024 * 1024).toLong()

            // Settings (piccolo)
            estimatedSize += 64L * 1024L // 64KB

            estimatedSize

        } catch (e: Exception) {
            Timber.e(e, "Backup size estimation failed")
            0L
        }
    }

    // ===== HELPER METHODS =====

    private fun calculateTotalBackupSize(
        backupData: BackupData,
        photoManifest: PhotoManifest
    ): Long {
        var totalSize = 0L

        // JSON size estimation
        totalSize += backupData.database.getTotalRecordCount() * 100

        // Photo archive size
        totalSize += photoManifest.totalSize

        // Settings size (small)
        totalSize += 64L * 1024L

        return totalSize
    }


    /**
     * =============================================================================
     * ADDITIONS FOR BackupRepositoryImpl.kt
     * =============================================================================
     *
     * This file shows the changes needed to add signature archive support
     * to the existing BackupRepositoryImpl class.
     */

// =============================================================================
// 1. ADD IMPORTS
// =============================================================================
// import net.calvuz.qreport.backup.domain.model.SignatureManifest
// import net.calvuz.qreport.backup.domain.repository.SignatureArchiveRepository

// =============================================================================
// 2. ADD REPOSITORY TO CONSTRUCTOR
// =============================================================================
// Add to the @Singleton class BackupRepositoryImpl @Inject constructor:
//
// private val signatureArchiveRepository: SignatureArchiveRepository
//
// Full constructor becomes:
// @Singleton
// class BackupRepositoryImpl @Inject constructor(
//     private val appVersionInfo: AppVersionInfo,
//     private val databaseExportRepository: DatabaseExportRepository,
//     private val photoArchiveRepository: PhotoArchiveRepository,
//     private val signatureArchiveRepository: SignatureArchiveRepository,  // <-- ADD
//     private val settingsRepository: SettingsRepository,
//     private val jsonSerializer: BackupJsonSerializer,
//     private val backupFileRepo: BackupFileRepository
// ) : BackupRepository

// =============================================================================
// 3. ADD SIGNATURE VARIABLES IN createFullBackup()
// =============================================================================
// After line 62 (photoManifest declaration), add:
//
// var signatureManifest = SignatureManifest.empty()
// var signatureError: String? = null

// =============================================================================
// 4. ADD SIGNATURE ARCHIVE PATH
// =============================================================================
// After line 75 (photoArchivePath), add:
//
// val signatureArchivePath = File(archivePath, "signatures.zip").absolutePath

// =============================================================================
// 5. ADD SIGNATURE BACKUP STEP (after photo backup, around line 144)
// =============================================================================
// Add new step after photo export completion:
//
// // 4b. Backup signatures
// Timber.d("4b. Export signatures")
// emit(BackupProgress.InProgress("4b. Export signatures", 0.75f))
//
// signatureArchiveRepository.createSignatureArchive(
//     outputPath = signatureArchivePath
// ).collect { progress ->
//     when (progress) {
//         is ArchiveProgress.InProgress -> {
//             emit(BackupProgress.InProgress(
//                 "- Signatures backup: ${progress.processedFiles}/${progress.totalFiles}",
//                 0.75f + (progress.progress * 0.05f),
//                 currentTable = "Signatures",
//                 processedRecords = progress.processedFiles,
//                 totalRecords = progress.totalFiles
//             ))
//         }
//         is ArchiveProgress.Completed -> {
//             signatureManifest = signatureArchiveRepository.generateSignatureManifest()
//         }
//         is ArchiveProgress.Error -> {
//             signatureError = progress.message
//         }
//     }
// }
//
// // Signature export error check
// if (signatureError != null) {
//     Timber.w("Signature export failed: $signatureError (continuing without signatures)")
//     // Non-blocking: continue without signatures
// }

// =============================================================================
// 6. UPDATE BackupData CREATION (around line 173)
// =============================================================================
// Add signatureManifest to BackupData:
//
// val backupData = BackupData(
//     metadata = metadata,
//     database = databaseBackup,
//     settings = settingsBackup,
//     photoManifest = photoManifest,
//     signatureManifest = signatureManifest  // <-- ADD THIS
// )

// =============================================================================
// 7. ADD SIGNATURE RESTORE IN restoreFromBackup() (around line 285)
// =============================================================================
// After photo restoration, add signature restoration:
//
// // 5b. Restore signatures
// if (backupData.includesSignatures()) {
//     emit(RestoreProgress.InProgress("Restoring signatures", 0.88f))
//
//     val signatureArchivePath = "${dirPath}/signatures.zip"
//     val signaturesDir = when (val result = backupFileRepo.getSignaturesDirectory()) {
//         is QrResult.Success -> result.data
//         is QrResult.Error -> {
//             Timber.w("Failed to get signatures directory, skipping")
//             null
//         }
//     }
//
//     if (signaturesDir != null && File(signatureArchivePath).exists()) {
//         signatureArchiveRepository.extractSignatureArchive(
//             archivePath = signatureArchivePath,
//             outputDir = signaturesDir
//         ).collect { progress ->
//             when (progress) {
//                 is ExtractionProgress.InProgress -> {
//                     emit(RestoreProgress.InProgress(
//                         "Restoring signatures: ${progress.extractedFiles}/${progress.totalFiles}",
//                         0.88f + (progress.progress * 0.05f),
//                         currentTable = "Signatures",
//                         processedRecords = progress.extractedFiles,
//                         totalRecords = progress.totalFiles
//                     ))
//                 }
//                 is ExtractionProgress.Error -> {
//                     Timber.w("Signature restoration failed: ${progress.message}")
//                 }
//                 is ExtractionProgress.Completed -> {
//                     Timber.d("Signatures restored: ${progress.extractedFiles} files")
//                 }
//             }
//         }
//     }
// }

// =============================================================================
// 8. UPDATE getEstimatedBackupSize() (around line 411)
// =============================================================================
// Add signature size estimation:
//
// // Signatures size
// val signatureManifest = signatureArchiveRepository.generateSignatureManifest()
// estimatedSize += (signatureManifest.totalSizeMB * 1024 * 1024).toLong()

// =============================================================================
// SUMMARY OF CHANGES:
// =============================================================================
// 1. Add imports for SignatureManifest and SignatureArchiveRepository
// 2. Add SignatureArchiveRepository to constructor
// 3. Add signatureManifest and signatureError variables
// 4. Add signatureArchivePath
// 5. Add signature backup step (step 4b)
// 6. Add signatureManifest to BackupData creation
// 7. Add signature restore step (step 5b)
// 8. Update size estimation
// =============================================================================

}