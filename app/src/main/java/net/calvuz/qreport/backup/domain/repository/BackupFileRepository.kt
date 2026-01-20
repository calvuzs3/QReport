package net.calvuz.qreport.backup.domain.repository

import net.calvuz.qreport.backup.domain.model.BackupData
import net.calvuz.qreport.backup.domain.model.BackupInfo
import net.calvuz.qreport.backup.domain.model.BackupValidationResult
import net.calvuz.qreport.backup.domain.model.enum.BackupMode
import net.calvuz.qreport.backup.domain.model.enum.BackupSortOrder
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult

interface BackupFileRepository {

    // ===== COMPATIBILITÀ DIRETTA con FileManager =====

    /**
     * ✅ DIRECT MAPPING: fileManager.generateBackupPath()
     */
    suspend fun generateBackupPath(backupId: String): QrResult<String, QrError>

    /**
     * ✅ DIRECT MAPPING: fileManager.loadBackup()
     */
    suspend fun loadBackup(backupPath: String): QrResult<BackupData, QrError>

    /**
     * ✅ DIRECT MAPPING: fileManager.saveBackup()
     */
    suspend fun saveBackup(
        backupData: BackupData,
        mode: BackupMode,
        customPath: String? = null
    ): QrResult<String, QrError>

    /**
     * ✅ DIRECT MAPPING: fileManager.listAvailableBackups()
     */
    suspend fun listBackups(sortBy: BackupSortOrder = BackupSortOrder.DATE_DESC): QrResult<List<BackupInfo>, QrError>

    /**
     * ✅ DIRECT MAPPING: fileManager.deleteBackup()
     * Note: FileManager prende backupId, noi convertiamo a path interno
     */
    suspend fun deleteBackupById(backupId: String): QrResult<Unit, QrError>

    /**
     * ✅ DIRECT MAPPING: fileManager.validateBackupFile()
     */
    suspend fun validateBackup(backupPath: String): QrResult<BackupValidationResult, QrError>

    // ===== DIRECTORY ACCESS for PhotoArchive =====
    suspend fun getPhotosDirectory(): QrResult<String, QrError>

    // ===== DIRECTORY ACCESS for SignatureArchive =====
    suspend fun getSignaturesDirectory(): QrResult<String, QrError>
}