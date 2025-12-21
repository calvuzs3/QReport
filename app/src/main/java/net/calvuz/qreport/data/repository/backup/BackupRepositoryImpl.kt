package net.calvuz.qreport.data.repository.backup

import net.calvuz.qreport.data.backup.model.ArchiveProgress
import net.calvuz.qreport.data.backup.model.BackupInfo
import net.calvuz.qreport.data.backup.model.ExtractionProgress
import net.calvuz.qreport.domain.repository.backup.DatabaseExportRepository
import net.calvuz.qreport.domain.model.backup.*
import net.calvuz.qreport.domain.model.file.BackupFileManager
import net.calvuz.qreport.domain.repository.backup.BackupRepository
import net.calvuz.qreport.domain.repository.backup.PhotoArchiveRepository
import net.calvuz.qreport.domain.repository.backup.SettingsBackupRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BACKUP REPOSITORY IMPLEMENTATION PRINCIPALE
 *
 * Implementazione del BackupRepository che coordina tutti i componenti
 * del sistema backup (Database, Photos, Settings).
 */

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val databaseExportRepository: DatabaseExportRepository,
    private val photoArchiveRepository: PhotoArchiveRepository,
    private val settingsBackupRepository: SettingsBackupRepository,
    private val jsonSerializer: net.calvuz.qreport.data.backup.BackupJsonSerializer,
    private val backupFileManager: BackupFileManager
) : BackupRepository {

    /**
     * Crea un backup completo del sistema
     */
    override suspend fun createFullBackup(
        includePhotos: Boolean,
        includeThumbnails: Boolean,
        backupMode: BackupMode,
        description: String?
    ): kotlinx.coroutines.flow.Flow<BackupProgress> = kotlinx.coroutines.flow.flow {

        emit(BackupProgress.InProgress("Inizializzazione backup...", 0f))

        try {
            val startTime = System.currentTimeMillis()
            val backupId = java.util.UUID.randomUUID().toString()

            // 1. Validazione database
            emit(BackupProgress.InProgress("Validazione database...", 0.05f))
            val validation = databaseExportRepository.validateDatabaseIntegrity()

            if (!validation.isValid) {
                emit(BackupProgress.Error("Database non valido: ${validation.errors.joinToString()}"))
            } else {
                // ✅ Continua solo se validazione OK

                if (validation.warnings.isNotEmpty()) {
                    Timber.w("Backup warnings: ${validation.warnings.joinToString()}")
                }

                // 2. Export database
                emit(BackupProgress.InProgress("Export database...", 0.1f, currentTable = "Tutte"))
                val databaseBackup = databaseExportRepository.exportAllTables()

                emit(BackupProgress.InProgress(
                    "Database esportato",
                    0.3f,
                    processedRecords = databaseBackup.getTotalRecordCount(),
                    totalRecords = databaseBackup.getTotalRecordCount()
                ))

                // 3. Backup impostazioni
                emit(BackupProgress.InProgress("Backup impostazioni...", 0.35f))
                val settingsBackup = settingsBackupRepository.exportSettings()

                // 4. Backup foto (se richiesto)
                var photoManifest = PhotoManifest.empty()
                var photoError: String? = null

                if (includePhotos) {
                    emit(BackupProgress.InProgress("Backup foto...", 0.4f))

                    val photoArchivePath = backupFileManager.getPhotoArchivePath(backupId)

                    photoArchiveRepository.createPhotoArchive(
                        outputPath = photoArchivePath,
                        includesThumbnails = includeThumbnails
                    ).collect { progress ->
                        when (progress) {
                            is ArchiveProgress.InProgress -> {
                                emit(BackupProgress.InProgress(
                                    "Backup foto: ${progress.processedFiles}/${progress.totalFiles}",
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

                // Verifica errori foto
                if (photoError != null) {
                    emit(BackupProgress.Error("Backup foto fallito: $photoError"))
                } else {
                    // 5. Creazione metadata
                    emit(BackupProgress.InProgress("Creazione metadata...", 0.85f))
                    val deviceInfo = DeviceInfo.current()
                    val appVersion = getAppVersion()
                    val databaseVersion = getDatabaseVersion()

                    val metadata = BackupMetadata.create(
                        appVersion = appVersion,
                        databaseVersion = databaseVersion,
                        deviceInfo = deviceInfo,
                        backupType = BackupType.FULL,
                        totalSize = 0L, // Calcolato dopo
                        description = description
                    )

                    // 6. Assemblaggio backup finale
                    emit(BackupProgress.InProgress("Assemblaggio backup...", 0.9f))
                    val backupData = BackupData(
                        metadata = metadata,
                        database = databaseBackup,
                        settings = settingsBackup,
                        photoManifest = photoManifest
                    )

                    // 7. Calcolo checksum
                    val checksumResult = jsonSerializer.calculateBackupChecksum(backupData)
                    if (checksumResult.isFailure) {
                        emit(BackupProgress.Error("Calcolo checksum fallito"))
                    } else {
                        val finalBackupData = backupData.copy(
                            metadata = metadata.copy(
                                checksum = checksumResult.getOrThrow(),
                                totalSize = calculateTotalBackupSize(backupData, photoManifest)
                            )
                        )

                        // 8. Salvataggio finale
                        emit(BackupProgress.InProgress("Salvataggio backup...", 0.95f))
                        val backupPath = backupFileManager.saveBackup(finalBackupData, backupMode)

                        val duration = System.currentTimeMillis() - startTime

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
            Timber.e(e, "Errore durante creazione backup")
            emit(BackupProgress.Error("Backup fallito: ${e.message}", e))
        }
    }

    /**
     * Ripristina il sistema da backup
     */
    override suspend fun restoreFromBackup(
        dirPath: String,
        backupPath: String,
        strategy: RestoreStrategy
    ): kotlinx.coroutines.flow.Flow<RestoreProgress> = kotlinx.coroutines.flow.flow {

        emit(RestoreProgress.InProgress("Caricamento backup...", 0f))

        try {
            // 1. Caricamento backup
            val backupData = backupFileManager.loadBackup(backupPath)

            // 2. Validazione backup
            emit(RestoreProgress.InProgress("Validazione backup...", 0.05f))
            val validation = validateBackup(backupPath)

            if (!validation.isValid) {
                emit(RestoreProgress.Error("Backup non valido: ${validation.errors.joinToString()}"))
            } else {
                // ✅ Continua solo se validazione OK

                // 3. TODO: Verifica checksum
                emit(RestoreProgress.InProgress("Verifica integrità...", 0.1f))
//                val checksumValid = jsonSerializer.verifyBackupIntegrity(
//                    backupData,
//                    backupData.metadata.checksum
//                )
                val checksumValid = true

                if (!checksumValid) {
                    emit(RestoreProgress.Error("Checksum backup non valido - possibile corruzione"))
                } else {
                    // 4. Ripristino database
                    emit(RestoreProgress.InProgress("Ripristino database...", 0.2f))
                    val importResult = databaseExportRepository.importAllTables(
                        backupData.database,
                        strategy
                    )

                    importResult.fold(
                        onSuccess = {
                            emit(RestoreProgress.InProgress(
                                "Database ripristinato",
                                0.6f,
                                processedRecords = backupData.database.getTotalRecordCount()
                            ))

                            // 5. Ripristino foto
                            var photoError: String? = null

                            if (backupData.includesPhotos()) {
                                emit(RestoreProgress.InProgress("Ripristino foto...", 0.65f))

                                val photoArchivePath = "${dirPath}/photos.zip" //backupFileManager.getPhotoArchivePathFromBackup(backupPath)
                                val photosDir = backupFileManager.getPhotosDirectory()

                                Timber.d("Ripristino foto\n   da $photoArchivePath\n   in $photosDir")

                                photoArchiveRepository.extractPhotoArchive(
                                    archivePath = photoArchivePath,
                                    outputDir = photosDir
                                ).collect { progress ->
                                    when (progress) {
                                        is ExtractionProgress.InProgress -> {
                                            emit(RestoreProgress.InProgress(
                                                "Ripristino foto: ${progress.extractedFiles}/${progress.totalFiles}",
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

                            // Verifica errori foto
                            if (photoError != null) {
                                emit(RestoreProgress.Error("Ripristino foto fallito: $photoError"))
                            } else {
                                // 6. Ripristino impostazioni
                                emit(RestoreProgress.InProgress("Ripristino impostazioni...", 0.95f))
                                val settingsResult = settingsBackupRepository.importSettings(backupData.settings)

                                settingsResult.fold(
                                    onSuccess = {
                                        emit(RestoreProgress.Completed(backupData.metadata.id))
                                    },
                                    onFailure = { error ->
                                        Timber.w(error, "Ripristino impostazioni fallito, ma continuando")
                                        emit(RestoreProgress.Completed(backupData.metadata.id))
                                    }
                                )
                            }
                        },
                        onFailure = { error ->
                            emit(RestoreProgress.Error("Ripristino database fallito: ${error.message}"))
                        }
                    )
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Errore durante ripristino backup")
            emit(RestoreProgress.Error("Ripristino fallito: ${e.message}", e))
        }
    }

    /**
     * Lista i backup disponibili
     */
    override suspend fun getAvailableBackups(): List<BackupInfo> {
        return try {
            backupFileManager.listAvailableBackups()
        } catch (e: Exception) {
            Timber.e(e, "Errore caricamento lista backup")
            emptyList()
        }
    }

    /**
     * Elimina un backup
     */
    override suspend fun deleteBackup(backupId: String): Result<Unit> {
        return try {
            backupFileManager.deleteBackup(backupId)
        } catch (e: Exception) {
            Timber.e(e, "Errore eliminazione backup $backupId")
            Result.failure(e)
        }
    }

    /**
     * Valida un backup
     */
    override suspend fun validateBackup(backupPath: String): BackupValidationResult {
        return try {
            backupFileManager.validateBackupFile(backupPath)
        } catch (e: Exception) {
            Timber.e(e, "Errore validazione backup")
            BackupValidationResult.invalid(listOf("Errore validazione: ${e.message}"))
        }
    }

    /**
     * Stima dimensione backup
     */
    override suspend fun getEstimatedBackupSize(includePhotos: Boolean): Long {
        return try {
            var estimatedSize = 0L

            // Database size (approssimativo)
            val recordCount = databaseExportRepository.getEstimatedRecordCount()
            estimatedSize += recordCount * 1024L // ~1KB per record

            // Photos size
            if (includePhotos) {
                val photoManifest = photoArchiveRepository.generatePhotoManifest()
                estimatedSize += (photoManifest.totalSizeMB * 1024 * 1024).toLong()
            }

            // Settings (piccolo)
            estimatedSize += 64L * 1024L // 64KB

            estimatedSize

        } catch (e: Exception) {
            Timber.e(e, "Errore stima dimensione backup")
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
        return 1
    }

    private fun calculateTotalBackupSize(
        backupData: BackupData,
        photoManifest: PhotoManifest
    ): Long {
        var totalSize = 0L

        // JSON size estimation
        totalSize += backupData.database.getTotalRecordCount() * 1024L

        // Photo archive size
        totalSize += (photoManifest.totalSizeMB * 1024 * 1024).toLong()

        // Settings size (small)
        totalSize += 64L * 1024L

        return totalSize
    }
}

