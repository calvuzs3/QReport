package net.calvuz.qreport.domain.model.file

import net.calvuz.qreport.data.backup.model.BackupInfo
import net.calvuz.qreport.domain.model.backup.BackupData
import net.calvuz.qreport.domain.model.backup.BackupMode
import net.calvuz.qreport.domain.model.backup.BackupValidationResult

/**
 * File manager per gestione backup su filesystem
 */
interface BackupFileManager {
    suspend fun saveBackup(backupData: BackupData, mode: BackupMode): String
    suspend fun loadBackup(backupPath: String): BackupData
    suspend fun listAvailableBackups(): List<BackupInfo>
    suspend fun deleteBackup(backupId: String): Result<Unit>
    suspend fun validateBackupFile(backupPath: String): BackupValidationResult
    fun getPhotoArchivePath(backupId: String): String
    fun getPhotoArchivePathFromBackup(backupPath: String): String
    fun getPhotosDirectory(): String
}