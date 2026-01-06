package net.calvuz.qreport.backup.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.backup.domain.model.BackupInfo
import net.calvuz.qreport.backup.domain.model.enum.BackupMode
import net.calvuz.qreport.backup.presentation.ui.model.BackupProgress
import net.calvuz.qreport.backup.domain.model.BackupValidationResult
import net.calvuz.qreport.backup.presentation.ui.model.RestoreProgress
import net.calvuz.qreport.backup.domain.model.enum.RestoreStrategy

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