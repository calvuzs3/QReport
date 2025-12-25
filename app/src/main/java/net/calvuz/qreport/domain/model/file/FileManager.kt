package net.calvuz.qreport.domain.model.file

import net.calvuz.qreport.data.backup.model.BackupInfo
import net.calvuz.qreport.domain.model.backup.BackupData
import net.calvuz.qreport.domain.model.backup.BackupMode
import net.calvuz.qreport.domain.model.backup.BackupValidationResult
import net.calvuz.qreport.domain.model.export.ExportResult

/**
 * File manager
 */
interface FileManager {

    // Directory Operations
    fun getPhotosDirectory(): String
    fun getExportsDirectory(): String
    fun getBackupsDirectory(): String

    // Backup Operations
    suspend fun saveBackup(backupData: BackupData, mode: BackupMode, backupPath: String): String
    suspend fun loadBackup(backupPath: String): BackupData
    suspend fun listAvailableBackups(): List<BackupInfo>
    suspend fun deleteBackup(backupId: String): Result<Unit>
    suspend fun validateBackupFile(backupPath: String): BackupValidationResult
    fun getArchivePath(backupId: String): String
    fun getArchivePathFromBackup(backupPath: String): String

    // Photo Operations
    fun createPhotoFile(checkItemId: String): String
    fun deletePhotoFile(filePath: String): Boolean
    fun getFileSize(filePath: String): Long

    // Export file management
    fun openExportedFile(exportResult: ExportResult.Success): Result<Unit>
    fun shareExportedFile(exportResult: ExportResult.Success): Result<Unit>
    fun getAppVersion(): String
}