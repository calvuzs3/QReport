package net.calvuz.qreport.data.repository.backup

import kotlinx.datetime.Clock
import net.calvuz.qreport.data.backup.model.ArchiveProgress
import net.calvuz.qreport.data.backup.model.BackupInfo
import net.calvuz.qreport.data.backup.model.ExtractionProgress
import net.calvuz.qreport.domain.repository.backup.DatabaseExportRepository
import net.calvuz.qreport.domain.model.backup.*
import net.calvuz.qreport.domain.model.file.FileManager
import net.calvuz.qreport.domain.repository.backup.BackupRepository
import net.calvuz.qreport.domain.repository.backup.PhotoArchiveRepository
import net.calvuz.qreport.domain.repository.backup.SettingsBackupRepository
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates all backup system components (Database, Photos, Settings).
 */

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val databaseExportRepository: DatabaseExportRepository,
    private val photoArchiveRepository: PhotoArchiveRepository,
    private val settingsBackupRepository: SettingsBackupRepository,
    private val jsonSerializer: net.calvuz.qreport.data.backup.BackupJsonSerializer,
    private val fileManager: FileManager
) : BackupRepository {

    /**
     * Create a full backup
     */
    override suspend fun createFullBackup(
        includePhotos: Boolean,
        includeThumbnails: Boolean,
        backupMode: BackupMode,
        description: String?
    ): kotlinx.coroutines.flow.Flow<BackupProgress> = kotlinx.coroutines.flow.flow {

        emit(BackupProgress.InProgress("Full backup start", 0f))

        try {
            // ID CREATION
            val backupId = java.util.UUID.randomUUID().toString()
            var photoManifest = PhotoManifest.empty()
            var photoError: String? = null
            val startTime = Clock.System.now()
            // Select a PATH
            val archivePath = fileManager.getArchivePath(backupId)
            // Select a Photos ArchivePath
            val photoArchivePath = File(archivePath, "photos.zip").absolutePath

            // 1. Database validation
            //
            Timber.d("1. DB validation")
            emit(BackupProgress.InProgress("1. Database validation", 0.05f))
            val validation = databaseExportRepository.validateDatabaseIntegrity()
            if (!validation.isValid) {
                emit(BackupProgress.Error("- Database is invalid: ${validation.errors.joinToString()}"))
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
                emit(BackupProgress.InProgress(
                    "- Database exported",
                    0.3f,
                    processedRecords = databaseBackup.getTotalRecordCount(),
                    totalRecords = databaseBackup.getTotalRecordCount()
                ))

                // 3. Settings backup
                //
                Timber.d("3. Export settings")
                emit(BackupProgress.InProgress("3. Export settings", 0.3f))
                val settingsBackup = settingsBackupRepository.exportSettings()
                emit(
                    BackupProgress.InProgress(
                        "- Settings exported", 0.35f))

                // 4. Backup foto (se richiesto)
                //
                                if (includePhotos) {
                    Timber.d("4. Export photos")
                    emit(BackupProgress.InProgress("4. Export photos", 0.4f))

                    Timber.d ("PhotoPath = $photoArchivePath")

                    photoArchiveRepository.createPhotoArchive(
                        outputPath = photoArchivePath,
                        includesThumbnails = includeThumbnails
                    ).collect { progress ->
                        when (progress) {
                            is ArchiveProgress.InProgress -> {
                                emit(BackupProgress.InProgress(
                                    "- Photos backup: ${progress.processedFiles}/${progress.totalFiles}",
                                    0.4f + (progress.progress * 0.4f),
                                    currentTable = "Photos",
                                    processedRecords = progress.processedFiles,
                                    totalRecords = progress.totalFiles
                                ))
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
                    val appVersion = getAppVersion()
                    val databaseVersion = getDatabaseVersion()

                    val metadata = BackupMetadata.create(
                        id = backupId,
                        appVersion = appVersion,
                        databaseVersion = databaseVersion,
                        deviceInfo = deviceInfo,
                        backupType = BackupType.FULL,
                        totalSize = 0L, // Calcolato dopo
                        description = description
                    )

                    // 6. Assemblaggio backup finale
                    //
                    emit(BackupProgress.InProgress("Finalizing backup", 0.9f))
                    val backupData = BackupData(
                        metadata = metadata,
                        database = databaseBackup,
                        settings = settingsBackup,
                        photoManifest = photoManifest
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
                        val backupPath = fileManager.saveBackup(finalBackupData, backupMode, archivePath)

                        val duration = Clock.System.now() - startTime

                        emit(BackupProgress.Completed(
                            backupId = backupId,
                            backupPath = backupPath,
                            totalSize = finalBackupData.metadata.totalSize,
                            duration = duration,
                            tablesBackedUp = 9
                        ))
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
    ): kotlinx.coroutines.flow.Flow<RestoreProgress> = kotlinx.coroutines.flow.flow {

        emit(RestoreProgress.InProgress("Loading backup", 0f))

        try {
            // 1. Caricamento backup
            //
            val backupData = fileManager.loadBackup(backupPath)

            // 2. Validazione backup
            //
            emit(RestoreProgress.InProgress("Validating backup", 0.05f))
            val validation = validateBackup(backupPath)

            if (!validation.isValid) {
                emit(RestoreProgress.Error("Backup is invalid: ${validation.errors.joinToString()}"))
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
                            emit(RestoreProgress.InProgress(
                                "Database restored",
                                0.6f,
                                processedRecords = backupData.database.getTotalRecordCount()
                            ))

                            // 5. Ripristino foto
                            //
                            var photoError: String? = null

                            if (backupData.includesPhotos()) {
                                emit(RestoreProgress.InProgress("Restoring photos", 0.65f))

                                val photoArchivePath = "${dirPath}/photos.zip" //fileManager.getPhotoArchivePathFromBackup(backupPath)
                                val photosDir = fileManager.getPhotosDirectory()

                                photoArchiveRepository.extractPhotoArchive(
                                    archivePath = photoArchivePath,
                                    outputDir = photosDir
                                ).collect { progress ->
                                    when (progress) {
                                        is ExtractionProgress.InProgress -> {
                                            emit(RestoreProgress.InProgress(
                                                "Restoring photos: ${progress.extractedFiles}/${progress.totalFiles}",
                                                0.65f + (progress.progress * 0.25f),
                                                currentTable = "Photos",
                                                processedRecords = progress.extractedFiles,
                                                totalRecords = progress.totalFiles
                                            ))
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

                            // Check for photos errors
                            if (photoError != null) {
                                emit(RestoreProgress.Error("Photos restoration failed: $photoError"))
                            } else {
                                // 6. Ripristino impostazioni
                                //
                                emit(RestoreProgress.InProgress("Restoring settings", 0.95f))
                                val settingsResult = settingsBackupRepository.importSettings(backupData.settings)

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
            fileManager.listAvailableBackups()
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
            fileManager.deleteBackup(backupId)
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
            fileManager.validateBackupFile(backupPath)
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

            // Settings (piccolo)
            estimatedSize += 64L * 1024L // 64KB

            estimatedSize

        } catch (e: Exception) {
            Timber.e(e, "Backup size estimation failed")
            0L
        }
    }

    // ===== HELPER METHODS =====

    private fun getAppVersion(): String {
        // TODO: Implementa lettura versione app da BuildConfig
        return "1.0.0"
    }

    private fun getDatabaseVersion(): Int {
        // TODO: Implementa lettura versione database
        return 3
    }

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
}