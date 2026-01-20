package net.calvuz.qreport.backup.data.repository

import kotlinx.datetime.Clock
import net.calvuz.qreport.backup.data.BackupJsonSerializer
import net.calvuz.qreport.backup.domain.model.*
import net.calvuz.qreport.backup.domain.model.backup.DatabaseBackup
import net.calvuz.qreport.backup.domain.model.backup.SettingsBackup
import net.calvuz.qreport.backup.domain.model.enum.BackupMode
import net.calvuz.qreport.backup.domain.model.enum.BackupSortOrder
import net.calvuz.qreport.backup.domain.repository.BackupFileRepository
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.file.domain.model.CoreFileInfo
import net.calvuz.qreport.app.file.domain.model.DirectorySpec
import net.calvuz.qreport.app.file.domain.repository.*
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.app.util.DateTimeUtils.toFilenameSafeDateTime
import net.calvuz.qreport.feature.backup.domain.repository.BackupFileInfo
import net.calvuz.qreport.feature.backup.domain.repository.BackupStats
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BackupFileRepositoryImpl - MAPPING 1:1 con FileManager esistente
 * Zero breaking changes per BackupRepositoryImpl
 */
@Singleton
class BackupFileRepositoryImpl @Inject constructor(
    private val coreFileRepo: CoreFileRepository,
    private val jsonSerializer: BackupJsonSerializer
) : BackupFileRepository {

    // ===== MAPPING DIRETTO FileManager methods =====

    override suspend fun generateBackupPath(backupId: String): QrResult<String, QrError> {
        return try {
            val timestamp = (Clock.System.now().toFilenameSafeDateTime())
            val shortId = backupId.take(8)
            val dirName = "backup_${timestamp}_${shortId}"

            when (val baseResult = coreFileRepo.getOrCreateDirectory(BackupDirectories.BACKUPS)) {
                is QrResult.Error -> baseResult
                is QrResult.Success -> QrResult.Success(File(baseResult.data, dirName).absolutePath)
            }
        } catch (e: Exception) {
            Timber.e(e, "Generate backup path failed")
            QrResult.Error(QrError.BackupError.PATH_GENERATION)
        }
    }

    override suspend fun loadBackup(backupPath: String): QrResult<BackupData, QrError> {
        return try {
            val backupFile = File(backupPath)

            if (!coreFileRepo.fileExists(backupPath)) {
                Timber.e("Backup file not found: $backupPath")
                return QrResult.Error(QrError.BackupError.LOAD)
            }

            Timber.d("Loading backup: $backupPath")

            val loadResult = if (backupFile.isDirectory) {
                loadBackupFromDirectory(backupFile)
            } else {
                jsonSerializer.loadBackupFromFile(backupFile)
            }

            when {
                loadResult.isSuccess -> {
                    val backupData = loadResult.getOrThrow()
                    Timber.i("Backup loaded: ${backupData.database.getTotalRecordCount()} records")
                    QrResult.Success(backupData)
                }
                else -> {
                    Timber.e("Failed to load backup")
                    QrResult.Error(QrError.BackupError.LOAD)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Backup load failed")
            QrResult.Error(QrError.BackupError.LOAD)
        }
    }

    override suspend fun saveBackup(
        backupData: BackupData,
        mode: BackupMode,
        customPath: String?
    ): QrResult<String, QrError> {
        return try {
            val backupDir = File(customPath!!)
            backupDir.mkdirs()

            // Save structured files
            saveBackupFiles(backupData, backupDir)

            val fullBackupFile = File(backupDir, "qreport_backup_full.json")
            QrResult.Success(fullBackupFile.absolutePath)

        } catch (e: Exception) {
            Timber.e(e, "Save backup failed")
            QrResult.Error(QrError.BackupError.SAVE)
        }
    }

    override suspend fun listBackups(sortBy: BackupSortOrder): QrResult<List<BackupInfo>, QrError> {
        return try {
            val backupsDirResult = coreFileRepo.getOrCreateDirectory(BackupDirectories.BACKUPS)
            when (backupsDirResult) {
                is QrResult.Error -> QrResult.Error(backupsDirResult.error)
                is QrResult.Success -> {
                    val listResult = coreFileRepo.listFiles(backupsDirResult.data)
                    when (listResult) {
                        is QrResult.Error -> QrResult.Error(listResult.error)
                        is QrResult.Success -> {
                            val backupInfos = listResult.data.mapNotNull { fileInfo ->
                                extractBackupInfo(fileInfo)
                            }
                            QrResult.Success(sortBackups(backupInfos, sortBy))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "List backups failed")
            QrResult.Error(QrError.BackupError.LOAD)
        }
    }

    override suspend fun deleteBackupById(backupId: String): QrResult<Unit, QrError> {
        return try {
            val listResult = listBackups()
            when (listResult) {
                is QrResult.Error -> QrResult.Error(listResult.error)
                is QrResult.Success -> {
                    val backup = listResult.data.find { it.id == backupId }
                    backup?.let {
                        coreFileRepo.deleteDirectory(it.dirPath)
                    } ?: QrResult.Error(QrError.BackupError.DELETE) // NotFound
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Delete backup failed")
            QrResult.Error(QrError.BackupError.DELETE)
        }
    }

    override suspend fun validateBackup(backupPath: String): QrResult<BackupValidationResult, QrError> {
        return try {
            val loadResult = jsonSerializer.loadBackupFromFile(File(backupPath))
            val validationResult = if (loadResult.isSuccess) {
                BackupValidationResult.valid()
            } else {
                BackupValidationResult.invalid(listOf("Backup file corrupted"))
            }
            QrResult.Success(validationResult)
        } catch (e: Exception) {
            Timber.e(e, "Validate backup failed")
            QrResult.Success(BackupValidationResult.invalid(listOf("Validation error: ${e.message}")))
        }
    }

    override suspend fun getPhotosDirectory(): QrResult<String, QrError> {
        return coreFileRepo.getOrCreateDirectory(DirectorySpec.Core.PHOTOS)
    }

    override suspend fun getSignaturesDirectory(): QrResult<String, QrError> {
        return coreFileRepo.getOrCreateDirectory(DirectorySpec.Core.SIGNATURES)
    }

    /*override*/ suspend fun getBackupStats(backupPath: String): QrResult<BackupStats, QrError> {
        return try {
            val file = File(backupPath)
            QrResult.Success(BackupStats(
                totalSize = file.length(),
                fileCount = 1,
                jsonFiles = listOf(BackupFileInfo(file.name, file.absolutePath, file.length(), file.lastModified())),
                photoArchive = null,
                infoFile = null,
                createdAt = file.lastModified(),
                lastModified = file.lastModified()
            ))
        } catch (e: Exception) {
            Timber.e(e, "Get backup stats failed")
            QrResult.Error(QrError.BackupError.STATS_CALCULATION)
        }
    }

    /*override*/ suspend fun cleanupTemporaryFiles(): QrResult<Int, QrError> {
        return try {
            val tempDirResult = coreFileRepo.getOrCreateDirectory(DirectorySpec.Core.TEMP)
            when (tempDirResult) {
                is QrResult.Error -> QrResult.Error(tempDirResult.error)
                is QrResult.Success -> coreFileRepo.cleanupOldFiles(tempDirResult.data, 0)
            }
        } catch (e: Exception) {
            Timber.e(e, "Cleanup temporary files failed")
            QrResult.Error(QrError.BackupError.CLEANUP_FAILED)
        }
    }

    // ===== HELPER METHODS =====

    private fun saveBackupFiles(backupData: BackupData, backupDir: File) {
        // Metadata
        jsonSerializer.saveBackupToFile(createMetadataOnlyBackup(backupData), File(backupDir, "metadata.json"))
        // Database
        jsonSerializer.saveBackupToFile(createDatabaseOnlyBackup(backupData), File(backupDir, "database.json"))
        // Settings
        jsonSerializer.saveBackupToFile(createSettingsOnlyBackup(backupData), File(backupDir, "settings.json"))
        // Full backup
        jsonSerializer.saveBackupToFile(backupData, File(backupDir, "qreport_backup_full.json"))
        // Info file
        createInfoFile(backupData, backupDir)
    }

    private fun createInfoFile(backupData: BackupData, backupDir: File) {
        try {
            File(backupDir, "INFO.txt").writeText("""
                QREPORT BACKUP
                ==============
                ID: ${backupData.metadata.id}
                Created: ${backupData.metadata.timestamp}
                Records: ${backupData.database.getTotalRecordCount()}
                Photos: ${backupData.photoManifest.totalPhotos}
            """.trimIndent())
        } catch (e: Exception) {
            Timber.e(e, "INFO.txt creation failed")
        }
    }

    private fun extractBackupInfo(fileInfo: CoreFileInfo): BackupInfo? {
        return try {
            val file = File(fileInfo.path)
            if (file.isDirectory) {
                val mainFile = findMainBackupFile(file)
                mainFile?.let { extractBackupInfoFromJson(it, fileInfo) }
            } else if (file.extension.equals("json", ignoreCase = true)) {
                extractBackupInfoFromJson(file, fileInfo)
            } else null
        } catch (e: Exception) {
            Timber.e(e, "Extract backup info failed")
            null
        }
    }

    private fun extractBackupInfoFromJson(jsonFile: File, fileInfo: CoreFileInfo): BackupInfo? {
        return try {
            val loadResult = jsonSerializer.loadBackupFromFile(jsonFile)
            if (loadResult.isSuccess) {
                val backupData = loadResult.getOrThrow()
                BackupInfo(
                    id = backupData.metadata.id,
                    createdAt = backupData.metadata.timestamp,
                    description = backupData.metadata.description,
                    totalSize = fileInfo.size,
                    includesPhotos = backupData.includesPhotos(),
                    dirPath = jsonFile.parent ?: "",
                    filePath = jsonFile.absolutePath,
                    appVersion = backupData.metadata.appVersion,
                    recordCount = backupData.database.getTotalRecordCount(),
                    photoCount = backupData.photoManifest.totalPhotos
                )
            } else null
        } catch (e: Exception) {
            Timber.e(e, "Extract backup info from JSON failed")
            null
        }
    }

    private fun findMainBackupFile(backupDir: File): File? {
        val files = backupDir.listFiles()?.filter { it.isFile } ?: return null
        return listOf("qreport_backup_full.json", "database.json", "backup.json")
            .firstNotNullOfOrNull { name -> files.find { it.name == name } }
    }

    private fun sortBackups(backups: List<BackupInfo>, sortBy: BackupSortOrder): List<BackupInfo> {
        return when (sortBy) {
            BackupSortOrder.DATE_DESC -> backups.sortedByDescending { it.createdAt }
            BackupSortOrder.DATE_ASC -> backups.sortedBy { it.createdAt }
            BackupSortOrder.SIZE_DESC -> backups.sortedByDescending { it.totalSize }
            BackupSortOrder.SIZE_ASC -> backups.sortedBy { it.totalSize }
            BackupSortOrder.NAME_DESC -> backups.sortedByDescending { it.description ?: it.id }
            BackupSortOrder.NAME_ASC -> backups.sortedBy { it.description ?: it.id }
        }
    }

    // Backup manipulation helpers
    private fun createMetadataOnlyBackup(backupData: BackupData) = backupData.copy(
        database = DatabaseBackup.empty(), photoManifest = PhotoManifest.empty()
    )

    private fun createDatabaseOnlyBackup(backupData: BackupData) = backupData.copy(
        settings = SettingsBackup.empty(), photoManifest = PhotoManifest.empty()
    )

    private fun createSettingsOnlyBackup(backupData: BackupData) = backupData.copy(
        database = DatabaseBackup.empty(), photoManifest = PhotoManifest.empty()
    )

    private fun loadBackupFromDirectory(backupDir: File): Result<BackupData> {
        return try {
            // Try to find main backup file
            val mainFile = findMainBackupFile(backupDir)
                ?: return Result.failure(Exception("Main backup file not found"))

            jsonSerializer.loadBackupFromFile(mainFile)
        } catch (e: Exception) {
            Timber.e(e, "Load backup from directory failed")
            Result.failure(e)
        }
    }
}