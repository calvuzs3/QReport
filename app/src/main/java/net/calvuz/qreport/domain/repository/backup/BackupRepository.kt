package net.calvuz.qreport.domain.repository.backup

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.data.backup.model.BackupInfo
import net.calvuz.qreport.domain.model.backup.BackupMode
import net.calvuz.qreport.domain.model.backup.BackupProgress
import net.calvuz.qreport.domain.model.backup.BackupValidationResult
import net.calvuz.qreport.domain.model.backup.RestoreProgress
import net.calvuz.qreport.domain.model.backup.RestoreStrategy

/**
 * Repository principale per operazioni di backup
 */
interface BackupRepository {

    /**
     * Crea un backup completo
     */
    suspend fun createFullBackup(
        includePhotos: Boolean = true,
        includeThumbnails: Boolean = false,
        backupMode: BackupMode = BackupMode.LOCAL,
        description: String? = null
    ): Flow<BackupProgress>

    /**
     * Ripristina da backup
     */
    suspend fun restoreFromBackup(
        dirPath: String,
        backupPath: String,
        strategy: RestoreStrategy = RestoreStrategy.REPLACE_ALL
    ): Flow<RestoreProgress>

    /**
     * Lista backup disponibili
     */
    suspend fun getAvailableBackups(): List<BackupInfo>

    /**
     * Elimina backup
     */
    suspend fun deleteBackup(backupId: String): Result<Unit>

    /**
     * Valida backup
     */
    suspend fun validateBackup(backupPath: String): BackupValidationResult

    /**
     * Stima dimensione backup
     */
    suspend fun getEstimatedBackupSize(includePhotos: Boolean): Long
}