package net.calvuz.qreport.data.local.file

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.Instant
import net.calvuz.qreport.data.backup.BackupJsonSerializer
import net.calvuz.qreport.data.backup.model.BackupInfo
import net.calvuz.qreport.domain.model.backup.*
import net.calvuz.qreport.domain.model.file.BackupFileManager
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IMPLEMENTAZIONE MANCANTE - BACKUP FILE MANAGER
 *
 * Gestisce tutte le operazioni filesystem per i backup QReport:
 * - Salvataggio/caricamento backup
 * - Gestione directory strutturata
 * - Lista e eliminazione backup
 * - Path management per foto
 */

@Singleton
class BackupFileManagerImpl @Inject constructor(
    @ApplicationContext  private val context: Context,
    private val jsonSerializer: BackupJsonSerializer
) : BackupFileManager {

    // ===== DIRECTORY STRUCTURE =====

    /**
     * Struttura directory backup:
     * /data/data/net.calvuz.qreport/files/backups/
     * ├── backup_20241220_143022/
     * │   ├── metadata.json
     * │   ├── database.json
     * │   ├── settings.json
     * │   └── photos.zip
     * ├── backup_20241219_090015/
     * └── backup_20241218_183045/
     */

    private val backupsBaseDir: File
        get() = File(context.filesDir, "backups").apply { mkdirs() }

    private val photosBaseDir: File
        get() = File(context.filesDir, "photos").apply { mkdirs() }

    // ===== SAVE BACKUP =====

    /**
     * Salva backup completo su filesystem
     */
    override suspend fun saveBackup(backupData: BackupData, mode: BackupMode): String {
        return try {
            val backupId = backupData.metadata.id
            val timestamp = formatTimestamp(backupData.metadata.timestamp)

            // Crea directory backup specifica
            val backupDir = File(backupsBaseDir, "backup_${timestamp}_${backupId.take(8)}")
            backupDir.mkdirs()

            Timber.d("Salvataggio backup in: ${backupDir.path}")

            // 1. Salva metadata
            val metadataFile = File(backupDir, "metadata.json")
            val metadataResult = jsonSerializer.saveBackupToFile(
                createMetadataOnlyBackup(backupData),
                metadataFile
            )

            if (metadataResult.isFailure) {
                throw BackupFileException("Salvataggio metadata fallito", metadataResult.exceptionOrNull())
            }

            // 2. Salva database
            val databaseFile = File(backupDir, "database.json")
            val databaseResult = jsonSerializer.saveBackupToFile(
                createDatabaseOnlyBackup(backupData),
                databaseFile
            )

            if (databaseResult.isFailure) {
                throw BackupFileException("Salvataggio database fallito", databaseResult.exceptionOrNull())
            }

            // 3. Salva settings
            val settingsFile = File(backupDir, "settings.json")
            val settingsResult = jsonSerializer.saveBackupToFile(
                createSettingsOnlyBackup(backupData),
                settingsFile
            )

            if (settingsResult.isFailure) {
                throw BackupFileException("Salvataggio settings fallito", settingsResult.exceptionOrNull())
            }

            // 4. Salva backup completo (per convenience)
            val fullBackupFile = File(backupDir, "qreport_backup_full.json")
            val fullResult = jsonSerializer.saveBackupToFile(backupData, fullBackupFile)

            if (fullResult.isFailure) {
                throw BackupFileException("Salvataggio backup completo fallito", fullResult.exceptionOrNull())
            }

            // 5. Crea info file di riepilogo
            createBackupInfoFile(backupDir, backupData)

            val backupPath = fullBackupFile.absolutePath
            Timber.d("Backup salvato con successo: $backupPath")

            // TODO: Se mode == CLOUD, carica anche su cloud storage

            backupPath

        } catch (e: Exception) {
            Timber.e(e, "Errore salvataggio backup")
            throw BackupFileException("Salvataggio backup fallito: ${e.message}", e)
        }
    }

    // ===== LOAD BACKUP =====

    /**
     * Carica backup da filesystem
     */
    override suspend fun loadBackup(backupPath: String): BackupData {
        return try {
            val backupFile = File(backupPath)

            if (!backupFile.exists()) {
                throw BackupFileException("File backup non trovato: $backupPath")
            }

            Timber.d("Caricamento backup da: $backupPath")

            val loadResult = jsonSerializer.loadBackupFromFile(backupFile)

            if (loadResult.isFailure) {
                throw BackupFileException("Caricamento backup fallito", loadResult.exceptionOrNull())
            }

            val backupData = loadResult.getOrThrow()

            Timber.d("Backup caricato: ${backupData.database.getTotalRecordCount()} record, " +
                    "${backupData.photoManifest.totalPhotos} foto")

            backupData

        } catch (e: Exception) {
            Timber.e(e, "Errore caricamento backup")
            throw BackupFileException("Caricamento backup fallito: ${e.message}", e)
        }
    }

    // ===== LIST AVAILABLE BACKUPS =====

    /**
     * Lista tutti i backup disponibili
     */
    override suspend fun listAvailableBackups(): List<BackupInfo> {
        return try {
            val backupDirs = backupsBaseDir.listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("backup_") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()

            val backupInfoList = mutableListOf<BackupInfo>()

            for (backupDir in backupDirs) {
                try {
                    val fullBackupFile = File(backupDir, "qreport_backup_full.json")

                    if (fullBackupFile.exists()) {
                        val loadResult = jsonSerializer.loadBackupFromFile(fullBackupFile)

                        if (loadResult.isSuccess) {
                            val backupData = loadResult.getOrThrow()

                            backupInfoList.add(BackupInfo(
                                id = backupData.metadata.id,
                                timestamp = backupData.metadata.timestamp,
                                description = backupData.metadata.description,
                                totalSizeMB = backupData.metadata.totalSize / (1024.0 * 1024.0),
                                includesPhotos = backupData.includesPhotos(),
                                filePath = fullBackupFile.absolutePath,
                                appVersion = backupData.metadata.appVersion
                            ))
                        }
                    }

                } catch (e: Exception) {
                    Timber.w(e, "Errore lettura backup in ${backupDir.name}")
                    // Continua con gli altri backup
                }
            }

            Timber.d("Trovati ${backupInfoList.size} backup disponibili")
            backupInfoList

        } catch (e: Exception) {
            Timber.e(e, "Errore lista backup")
            emptyList()
        }
    }

    // ===== DELETE BACKUP =====

    /**
     * Elimina backup e tutti i file associati
     */
    override suspend fun deleteBackup(backupId: String): Result<Unit> {
        return try {
            val backupDirs = backupsBaseDir.listFiles()
                ?.filter { it.isDirectory && it.name.contains(backupId.take(8)) }
                ?: emptyList()

            var deletedCount = 0

            for (backupDir in backupDirs) {
                if (backupDir.deleteRecursively()) {
                    deletedCount++
                    Timber.d("Eliminato backup directory: ${backupDir.name}")
                } else {
                    Timber.w("Impossibile eliminare directory: ${backupDir.name}")
                }
            }

            if (deletedCount == 0) {
                Result.failure(BackupFileException("Backup $backupId non trovato per eliminazione"))
            } else {
                Timber.d("Eliminati $deletedCount backup con ID $backupId")
                Result.success(Unit)
            }

        } catch (e: Exception) {
            Timber.e(e, "Errore eliminazione backup $backupId")
            Result.failure(BackupFileException("Eliminazione backup fallita: ${e.message}", e))
        }
    }

    // ===== VALIDATE BACKUP FILE =====

    /**
     * Valida file backup senza caricarlo completamente
     */
    override suspend fun validateBackupFile(backupPath: String): BackupValidationResult {
        return try {
            val backupFile = File(backupPath)

            if (!backupFile.exists()) {
                return BackupValidationResult.invalid(listOf("File backup non esistente: $backupPath"))
            }

            if (!backupFile.canRead()) {
                return BackupValidationResult.invalid(listOf("File backup non leggibile: $backupPath"))
            }

            if (backupFile.length() == 0L) {
                return BackupValidationResult.invalid(listOf("File backup vuoto"))
            }

            // Valida JSON format
            val jsonContent = backupFile.readText()
            val jsonValidation = jsonSerializer.validateBackupJson(jsonContent)

            if (!jsonValidation.isValid) {
                return jsonValidation
            }

            // Carica e valida contenuto
            val loadResult = jsonSerializer.loadBackupFromFile(backupFile)

            if (loadResult.isFailure) {
                return BackupValidationResult.invalid(listOf("Impossibile caricare backup: ${loadResult.exceptionOrNull()?.message}"))
            }

            val backupData = loadResult.getOrThrow()
            val warnings = mutableListOf<String>()

            // Verifica checksum se presente
            if (backupData.metadata.checksum.isNotEmpty()) {
                val isChecksumValid = true
                // TODO valid checksum required
//                val isChecksumValid = jsonSerializer.verifyBackupIntegrity(backupData, backupData.metadata.checksum)
//                if (!isChecksumValid) {
//                    warnings.add("Checksum backup non valido - possibile corruzione")
//                }
            }

            // Verifica dimensione ragionevole
            val fileSizeMB = backupFile.length() / (1024.0 * 1024.0)
            if (fileSizeMB > 500) {
                warnings.add("File backup molto grande (${fileSizeMB.toInt()}MB)")
            }

            BackupValidationResult(
                isValid = true,
                errors = emptyList(),
                warnings = warnings
            )

        } catch (e: Exception) {
            Timber.e(e, "Errore validazione backup $backupPath")
            BackupValidationResult.invalid(listOf("Validazione fallita: ${e.message}"))
        }
    }

    // ===== PHOTO PATH MANAGEMENT =====

    /**
     * Path archivio foto per backup ID
     */
    override fun getPhotoArchivePath(backupId: String): String {
        val timestamp = System.currentTimeMillis()
        val backupDir = File(backupsBaseDir, "backup_${timestamp}_${backupId.take(8)}")
        return File(backupDir, "photos.zip").absolutePath
    }

    /**
     * Path archivio foto da path backup completo
     */
    override fun getPhotoArchivePathFromBackup(backupPath: String): String {
        val backupFile = File(backupPath)
        val backupDir = backupFile.parentFile ?: backupsBaseDir
        return File(backupDir, "photos.zip").absolutePath
    }

    /**
     * Directory base foto app
     */
    override fun getPhotosDirectory(): String {
        return photosBaseDir.absolutePath
    }

    // ===== HELPER METHODS =====

    /**
     * Formatta timestamp per nomi directory
     */
    private fun formatTimestamp(instant: Instant): String {
        // Format: 20241220_143022
        return instant.toString()
            .replace("T", "_")
            .replace(":", "")
            .replace("-", "")
            .substring(0, 15) // YYYYMMDD_HHMMSS
    }

    /**
     * Crea backup contenente solo metadata
     */
    private fun createMetadataOnlyBackup(backupData: BackupData): BackupData {
        return backupData.copy(
            database = DatabaseBackup(
                checkUps = emptyList(), checkItems = emptyList(), photos = emptyList(), spareParts = emptyList(),
                clients = emptyList(), contacts = emptyList(), facilities = emptyList(), facilityIslands = emptyList(),
                checkUpAssociations = emptyList(), exportedAt = backupData.database.exportedAt
            )
        )
    }

    /**
     * Crea backup contenente solo database
     */
    private fun createDatabaseOnlyBackup(backupData: BackupData): BackupData {
        return backupData.copy(
            settings = SettingsBackup.empty(),
            photoManifest = PhotoManifest.empty()
        )
    }

    /**
     * Crea backup contenente solo settings
     */
    private fun createSettingsOnlyBackup(backupData: BackupData): BackupData {
        return backupData.copy(
            database = DatabaseBackup(
                checkUps = emptyList(), checkItems = emptyList(), photos = emptyList(), spareParts = emptyList(),
                clients = emptyList(), contacts = emptyList(), facilities = emptyList(), facilityIslands = emptyList(),
                checkUpAssociations = emptyList(), exportedAt = backupData.database.exportedAt
            ),
            photoManifest = PhotoManifest.empty()
        )
    }

    /**
     * Crea file INFO.txt con riepilogo backup
     */
    private fun createBackupInfoFile(backupDir: File, backupData: BackupData) {
        try {
            val infoFile = File(backupDir, "INFO.txt")
            val infoContent = """
                ===============================================
                         QREPORT BACKUP INFORMATION
                ===============================================
                
                Backup ID: ${backupData.metadata.id}
                Created: ${backupData.metadata.timestamp}
                App Version: ${backupData.metadata.appVersion}
                Database Version: ${backupData.metadata.databaseVersion}
                
                Device Info:
                - Model: ${backupData.metadata.deviceInfo.model}
                - Manufacturer: ${backupData.metadata.deviceInfo.manufacturer}
                - OS: ${backupData.metadata.deviceInfo.osVersion}
                
                Database Content:
                - CheckUps: ${backupData.database.checkUps.size}
                - CheckItems: ${backupData.database.checkItems.size}
                - Photos: ${backupData.database.photos.size}
                - Spare Parts: ${backupData.database.spareParts.size}
                - Clients: ${backupData.database.clients.size}
                - Contacts: ${backupData.database.contacts.size}
                - Facilities: ${backupData.database.facilities.size}
                - Facility Islands: ${backupData.database.facilityIslands.size}
                - Associations: ${backupData.database.checkUpAssociations.size}
                
                Total Records: ${backupData.database.getTotalRecordCount()}
                
                Photo Manifest:
                - Total Photos: ${backupData.photoManifest.totalPhotos}
                - Total Size: ${backupData.photoManifest.totalSizeMB} MB
                - Includes Thumbnails: ${backupData.photoManifest.includesThumbnails}
                
                Backup Size: ${backupData.metadata.totalSize / (1024 * 1024)} MB
                Checksum: ${backupData.metadata.checksum}
                
                Description: ${backupData.metadata.description ?: "Nessuna descrizione"}
                
                ===============================================
            """.trimIndent()

            infoFile.writeText(infoContent)

        } catch (e: Exception) {
            Timber.w(e, "Impossibile creare file INFO.txt")
        }
    }
}

/**
 * Eccezione custom per errori file operations
 */
class BackupFileException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/*
=============================================================================
                            STRUTTURA FILESYSTEM
=============================================================================

/data/data/net.calvuz.qreport/files/backups/
├── backup_20241220_143022_a1b2c3d4/          // Directory backup specifica
│   ├── metadata.json                         // Solo metadata
│   ├── database.json                         // Solo dati database
│   ├── settings.json                         // Solo impostazioni
│   ├── photos.zip                           // Archivio foto
│   ├── qreport_backup_full.json             // Backup completo
│   └── INFO.txt                             // Riepilogo human-readable
├── backup_20241219_090015_e5f6g7h8/
├── backup_20241218_183045_i9j0k1l2/
└── ...

/data/data/net.calvuz.qreport/files/photos/   // Directory foto originali
├── {checkItemId1}/
│   ├── photo_001.jpg
│   └── thumb_001.jpg
└── {checkItemId2}/

VANTAGGI:
✅ Backup strutturati e leggibili
✅ File separati per performance (metadata rapido)
✅ Backup completo per convenience
✅ INFO.txt per debug e support
✅ Timestamp nel nome directory per ordinamento
✅ ID corto nel nome per identificazione

=============================================================================
*/