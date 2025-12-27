package net.calvuz.qreport.data.local.file

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.calvuz.qreport.data.backup.BackupJsonSerializer
import net.calvuz.qreport.data.backup.model.BackupInfo
import net.calvuz.qreport.domain.model.backup.*
import net.calvuz.qreport.domain.model.export.ExportFormat
import net.calvuz.qreport.domain.model.export.ExportResult
import net.calvuz.qreport.domain.model.file.FileManager
import net.calvuz.qreport.util.DateTimeUtils.formatTimestampToDateTime
import net.calvuz.qreport.util.DateTimeUtils.toItalianDate
import net.calvuz.qreport.util.SizeUtils.getFormattedSize
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backup filesystem operationa
 * - save/load backup
 * - directory strutturata
 * - List
 * - delete backup
 * - Path management for photos
 */
@Singleton
class FileManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val jsonSerializer: BackupJsonSerializer
) : FileManager {

    /**
     * Struttura directory backup:
     * /data/data/net.calvuz.qreport/files/
     * ├──backups/
     * │    /data/data/net.calvuz.qreport/files/backups/
     * │    ├── backup_20241220_143022/
     * │    │   ├── metadata.json
     * │    │   ├── database.json
     * │    │   ├── settings.json
     * │    │   └── photos.zip
     * │    ├── backup_20241219_090015/
     * │    └── backup_20241218_183045/
     * ├──exports/
     * └──photos/
     */

    private val photosDir = "photos"
    private val exportsDir = "exports"
    private val backupsDir = "backups"

    // ===== DIRECTORIES =====

    override fun getPhotosDirectory(): String {
        val dir = context.filesDir.resolve(photosDir)
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    override fun getExportsDirectory(): String {
        val dir = context.filesDir.resolve(exportsDir)
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    override fun getBackupsDirectory(): String {
        val dir = context.filesDir.resolve(backupsDir)
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    // ===== ARCHIVE PATH MANAGEMENT =====

    /**
     * Photo archive Path
     */
    override fun getArchivePath(backupId: String): String {
        val timestamp = Clock.System.now()
        val backupDir = File(
            getBackupsDirectory(),
            "backup_${formatTimestampToDateTime(timestamp)}_${backupId.take(8)}"
        )

        Timber.v("getArchivePath: $backupDir")
        return (backupDir).absolutePath
    }

    /**
     * Path archivio foto da path backup completo
     */
    override fun getArchivePathFromBackup(backupPath: String): String {
        val backupFile = File(backupPath)
        val backupDir = backupFile.parentFile ?: getBackupsDirectory()

        Timber.v("getArchivePathFromBackup: $backupDir")
        return (backupDir).toString()
    }

    // ===== SAVE BACKUP =====

    /**
     * Save backup
     */
    override suspend fun saveBackup(
        backupData: BackupData,
        mode: BackupMode,
        backupPath: String
    ): String {
        return try {

            // Create backup directory if it does not exists
            val backupDir = File(backupPath)
            backupDir.mkdirs()

            Timber.v("Save backup path: ${backupDir.path}")

            // 1. Save metadata
            //
            val metadataFile = File(backupDir, "metadata.json")
            val metadataResult = jsonSerializer.saveBackupToFile(
                createMetadataOnlyBackup(backupData),
                metadataFile
            )
            if (metadataResult.isFailure) {
                throw BackupFileException(
                    "Metadata saving failed",
                    metadataResult.exceptionOrNull()
                )
            }

            // 2. Save database
            //
            val databaseFile = File(backupDir, "database.json")
            val databaseResult = jsonSerializer.saveBackupToFile(
                createDatabaseOnlyBackup(backupData),
                databaseFile
            )
            if (databaseResult.isFailure) {
                throw BackupFileException(
                    "Database saving failed",
                    databaseResult.exceptionOrNull()
                )
            }

            // 3. Save settings
            //
            val settingsFile = File(backupDir, "settings.json")
            val settingsResult = jsonSerializer.saveBackupToFile(
                createSettingsOnlyBackup(backupData),
                settingsFile
            )
            if (settingsResult.isFailure) {
                throw BackupFileException(
                    "Settings saving failed",
                    settingsResult.exceptionOrNull()
                )
            }

            // 4. Save full backup
            //
            val fullBackupFile = File(backupDir, "qreport_backup_full.json")
            val fullResult = jsonSerializer.saveBackupToFile(backupData, fullBackupFile)
            if (fullResult.isFailure) {
                throw BackupFileException(
                    "Salvataggio backup completo fallito",
                    fullResult.exceptionOrNull()
                )
            }

            // 5. Create summary info file
            createBackupInfoFile(backupDir, backupData)

            val backupPath = fullBackupFile.absolutePath
            Timber.v("Backup saved: $backupPath")

            // TODO: Se mode == CLOUD, carica anche su cloud storage

            backupPath

        } catch (e: Exception) {
            Timber.e(e, "Backup saving failed")
            throw BackupFileException("Backup saving failed: ${e.message}", e)
        }
    }

    // ===== LOAD BACKUP =====

    /**
     * Load backup
     */
    override suspend fun loadBackup(backupPath: String): BackupData {
        return try {
            val backupFile = File(backupPath)

            if (!backupFile.exists()) {
                throw BackupFileException("Backup file not found: $backupPath")
            }

            Timber.v("Backup loading: $backupPath")

            val loadResult = jsonSerializer.loadBackupFromFile(backupFile)

            if (loadResult.isFailure) {
                throw BackupFileException("Backup loading failed", loadResult.exceptionOrNull())
            }

            val backupData = loadResult.getOrThrow()

            Timber.v(
                "Backup loaded: ${backupData.database.getTotalRecordCount()} records, " +
                        "${backupData.photoManifest.totalPhotos} photos"
            )

            backupData

        } catch (e: Exception) {
            Timber.e(e, "Backup loading failed")
            throw BackupFileException("Backup loading failed: ${e.message}", e)
        }
    }

    // ===== LIST AVAILABLE BACKUPS =====

    /**
     * List available backups
     */
    override suspend fun listAvailableBackups(): List<BackupInfo> {
        return try {
            val backupDirs = File(getBackupsDirectory()).listFiles()
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

                            backupInfoList.add(
                                BackupInfo(
                                    id = backupData.metadata.id,
                                    createdAt = backupData.metadata.timestamp,
                                    description = backupData.metadata.description,
                                    totalSize = backupData.metadata.totalSize,
                                    includesPhotos = backupData.includesPhotos(),
                                    dirPath = backupDir.absolutePath,
                                    filePath = fullBackupFile.absolutePath,
                                    appVersion = backupData.metadata.appVersion
                                )
                            )
                        }
                    }

                } catch (e: Exception) {
                    Timber.w(e, "Listing backup failed: ${backupDir.name}")
                    // Continua con gli altri backup
                }
            }

            Timber.v("Found ${backupInfoList.size} available backups")
            backupInfoList

        } catch (e: Exception) {
            Timber.e(e, "Errore lista backup")
            emptyList()
        }
    }

    // ===== DELETE BACKUP =====

    /**
     * Delete backup and all files associated
     */
    override suspend fun deleteBackup(backupId: String): Result<Unit> {
        return try {
            Timber.v("Backup Directory: ${getBackupsDirectory()}")

            val backupDirs = File(getBackupsDirectory()).listFiles()
                ?.filter { it.isDirectory && it.name.contains(backupId.take(8)) }
                ?: emptyList()

            var deletedCount = 0

            for (backupDir in backupDirs) {
                if (backupDir.deleteRecursively()) {
                    deletedCount++
                    Timber.v("Deleted backup directory: ${backupDir.name}")
                } else {
                    Timber.w("Deleting backup directory failed: ${backupDir.name}")
                }
            }

            if (deletedCount == 0) {
                Result.failure(BackupFileException("Backup not found: $backupId"))
            } else {
                Timber.v("Deleted $deletedCount backups with ID $backupId")
                Result.success(Unit)
            }

        } catch (e: Exception) {
            Timber.e(e, "Delete backup failed $backupId")
            Result.failure(BackupFileException("Delete backup failed: ${e.message}", e))
        }
    }

    // ===== VALIDATE BACKUP FILE =====

    /**
     * Validate backup file without a complete load
     */
    override suspend fun validateBackupFile(backupPath: String): BackupValidationResult {
        return try {
            val backupFile = File(backupPath)

            if (!backupFile.exists()) {
                return BackupValidationResult.invalid(listOf("Backup file does not exists: $backupPath"))
            }

            if (!backupFile.canRead()) {
                return BackupValidationResult.invalid(listOf("Backup file is not readable: $backupPath"))
            }

            if (backupFile.length() == 0L) {
                return BackupValidationResult.invalid(listOf("Backup file is empty"))
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
                return BackupValidationResult.invalid(listOf("Backup file loading failed: ${loadResult.exceptionOrNull()?.message}"))
            }

            val backupData = loadResult.getOrThrow()
            val warnings = mutableListOf<String>()

            // Verifica checksum se presente
            if (backupData.metadata.checksum.isNotEmpty()) {
                val isChecksumValid = jsonSerializer.verifyBackupIntegrity(backupData)
                if (!isChecksumValid) {
                    warnings.add("Checksum not valid - possibile corruption")
                }
            }

            // Verifica dimensione ragionevole
            val fileSize = backupFile.length()
            if (fileSize > 2 * 1024 * 1024 * 1024) {
                warnings.add("Backup file size big (${fileSize.getFormattedSize()} )")
            }

            BackupValidationResult(
                isValid = true,
                errors = emptyList(),
                warnings = warnings
            )

        } catch (e: Exception) {
            Timber.e(e, "Backup validation failed $backupPath")
            BackupValidationResult.invalid(listOf("Backup validation failed: ${e.message}"))
        }
    }

    // ===== PHOTO MANAGEMENT =====

    override fun createPhotoFile(checkItemId: String): String {
        val photosDir = getPhotosDirectory()
        val timestamp = System.currentTimeMillis()
        return "$photosDir/${checkItemId}_$timestamp.jpg"
    }

    override fun deletePhotoFile(filePath: String): Boolean {
        return try {
            File(filePath).delete()
        } catch (_: Exception) {
            false
        }
    }

    override fun getFileSize(filePath: String): Long {
        return try {
            File(filePath).length()
        } catch (_: Exception) {
            0L
        }
    }

    // Export file management
    override fun openExportedFile(exportResult: ExportResult.Success): Result<Unit> {
        return try {
            val file = File(exportResult.filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(exportResult.format))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Apri con"))
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to open exported file")
            Result.failure(e)
        }
    }

    override fun shareExportedFile(exportResult: ExportResult.Success): Result<Unit> {
        return try {
            val file = File(exportResult.filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(exportResult.format)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Report QReport - ${exportResult.fileName}")
                putExtra(Intent.EXTRA_TEXT, "Report generato dall'app QReport")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Condividi"))
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to share exported file")
            Result.failure(e)
        }
    }

    override fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.toString()
        } catch (e: Exception) {
            Timber.w(e, "Failed to get app version")
            "unknown"
        }
    }

    // COMPRESSED FILES

    /**
     * ✅ NEW: Create compressed ZIP backup for sharing
     *
     * @param backupPath Path to backup (directory or single file)
     * @param includeAllFiles If true, compress all files; if false, only main JSON
     * @return Result<File> ZIP file ready for sharing
     */
    override suspend fun createCompressedBackup(
        backupPath: String,
        includeAllFiles: Boolean
    ): Result<File> {
        return try {
            val backupFile = File(backupPath)

            if (!backupFile.exists()) {
                return Result.failure(IllegalArgumentException("Backup path does not exist: $backupPath"))
            }

            // Create temporary ZIP file in cache directory
//            val tempDir = context.cacheDir  // ✅ Direct cache directory
            val tempDir = File(context.cacheDir, "shared")  // ❌ Subfolder might be wrong
            tempDir.mkdirs()

            val timestamp = System.currentTimeMillis()
            val zipFileName = "qreport_backup_$timestamp.zip"
            val zipFile = File(tempDir, zipFileName)

            // Create ZIP archive
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->

                if (backupFile.isDirectory) {
                    // Directory backup - compress files based on includeAllFiles flag
                    val filesToCompress = if (includeAllFiles) {
                        // All files in directory
                        backupFile.listFiles()?.filter { it.isFile } ?: emptyList()
                    } else {
                        // Only main JSON file
                        val mainFile = findMainBackupFile(backupFile)
                        if (mainFile != null) listOf(mainFile) else emptyList()
                    }

                    Timber.d("Compressing ${filesToCompress.size} files from directory to ZIP")

                    filesToCompress.forEach { file ->
                        addFileToZip(zos, file, file.name)
                    }

                } else {
                    // Single file backup
                    Timber.d("Compressing single file to ZIP: ${backupFile.name}")
                    addFileToZip(zos, backupFile, backupFile.name)
                }
            }

            Timber.d("ZIP backup created successfully:")
            Timber.d("  - File: ${zipFile.absolutePath}")
            Timber.d("  - Size: ${(zipFile.length()).getFormattedSize()}")

            // Schedule automatic cleanup after 10 minutes
            scheduleZipCleanup(zipFile, delayMinutes = 10)

            Result.success(zipFile)

        } catch (e: Exception) {
            Timber.e(e, "Failed to create compressed backup")
            Result.failure(e)
        }
    }


    /**
     * ✅ NEW: Clean up all temporary ZIP backups (call on app start)
     */
    override suspend fun cleanupTempZipBackups() {
        try {
            val tempDir = File(context.cacheDir, "shared_backups")
            if (!tempDir.exists()) return

            val zipFiles = tempDir.listFiles()?.filter {
                it.extension.equals("zip", ignoreCase = true)
            } ?: return

            var cleanedCount = 0
            zipFiles.forEach { zipFile ->
                try {
                    if (zipFile.delete()) {
                        cleanedCount++
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to cleanup ZIP: ${zipFile.name}")
                }
            }

            if (cleanedCount > 0) {
                Timber.d("Cleaned up $cleanedCount temporary ZIP backups")
            }

        } catch (e: Exception) {
            Timber.w(e, "Error during ZIP cleanup")
        }
    }

    // COMPRESSED HELPERS

    /**
     * ✅ NEW: Add single file to ZIP archive
     */
    private fun addFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists() || !file.canRead()) {
            Timber.w("Skipping unreadable file: ${file.name}")
            return
        }

        try {
            val entry = ZipEntry(entryName).apply {
                time = file.lastModified()
                // Set compression method
                method = ZipEntry.DEFLATED
            }

            zos.putNextEntry(entry)

            FileInputStream(file).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    bis.copyTo(zos, bufferSize = 8192)
                }
            }

            zos.closeEntry()
            Timber.d("Added to ZIP: $entryName (${file.length().getFormattedSize()})")

        } catch (e: Exception) {
            Timber.e(e, "Error adding file to ZIP: $entryName")
            throw e
        }
    }

    /**
     * ✅ NEW: Find main backup JSON file in directory
     */
    private fun findMainBackupFile(backupDir: File): File? {
        val files = backupDir.listFiles()?.filter { it.isFile } ?: return null

        // Priority order for main backup file
        val candidates = listOf(
            "qreport_backup_full.json",
            "database.json",
            "backup.json"
        )

        return candidates.firstNotNullOfOrNull { fileName ->
            files.find { it.name == fileName }
        } ?: files.find { it.extension.equals("json", ignoreCase = true) }
    }

    /**
     * ✅ NEW: Schedule automatic ZIP file cleanup
     */
    private fun scheduleZipCleanup(zipFile: File, delayMinutes: Long = 10) {
        // Using coroutine for cleanup (requires CoroutineScope)
        CoroutineScope(Dispatchers.IO).launch {
            delay(delayMinutes * 60 * 1000) // Convert minutes to milliseconds

            try {
                if (zipFile.exists()) {
                    val deleted = zipFile.delete()
                    if (deleted) {
                        Timber.d("Auto-cleaned ZIP backup: ${zipFile.name}")
                    } else {
                        Timber.w("Failed to delete ZIP backup: ${zipFile.name}")
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Error during ZIP cleanup: ${zipFile.name}")
            }
        }
    }

    // ===== HELPER METHODS =====

    /**
     * Crea backup contenente solo metadata
     */
    private fun createMetadataOnlyBackup(backupData: BackupData): BackupData {
        return backupData.copy(
            database = DatabaseBackup(
                checkUps = emptyList(),
                checkItems = emptyList(),
                photos = emptyList(),
                spareParts = emptyList(),
                clients = emptyList(),
                contacts = emptyList(),
                facilities = emptyList(),
                facilityIslands = emptyList(),
                checkUpAssociations = emptyList(),
                exportedAt = backupData.database.exportedAt
            ),
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
                checkUps = emptyList(),
                checkItems = emptyList(),
                photos = emptyList(),
                spareParts = emptyList(),
                clients = emptyList(),
                contacts = emptyList(),
                facilities = emptyList(),
                facilityIslands = emptyList(),
                checkUpAssociations = emptyList(),
                exportedAt = backupData.database.exportedAt
            ),
            photoManifest = PhotoManifest.empty()
        )
    }

    /**
     * Create file INFO.txt with backup summary
     */
    private fun createBackupInfoFile(backupDir: File, backupData: BackupData) {
        try {
            val infoFile = File(backupDir, "INFO.txt")
            val infoContent = """
                ===============================================
                         QREPORT BACKUP INFORMATION
                ===============================================
                
                Backup ID: ${backupData.metadata.id}
                Created: ${backupData.metadata.timestamp.toItalianDate()}
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
                - Total Size: ${backupData.photoManifest.totalSize} MB
                - Includes Thumbnails: ${backupData.photoManifest.includesThumbnails}
                
                Backup Size: ${backupData.metadata.totalSize.getFormattedSize()}
                Checksum: ${backupData.metadata.checksum}
                
                Description: ${backupData.metadata.description ?: "No description"}
                
                ===============================================
            """.trimIndent()

            infoFile.writeText(infoContent)

        } catch (e: Exception) {
            Timber.w(e, "File INFO.txt creation failed")
        }
    }

    private fun getMimeType(format: ExportFormat): String {
        return when (format) {
            ExportFormat.WORD -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ExportFormat.TEXT -> "text/plain"
            ExportFormat.PHOTO_FOLDER -> "application/zip"
            ExportFormat.COMBINED_PACKAGE -> "application/zip"
        }
    }
}

/**
 * Custom excepion for file operation errors
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

=============================================================================
*/