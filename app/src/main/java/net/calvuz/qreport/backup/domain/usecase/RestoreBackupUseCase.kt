package net.calvuz.qreport.backup.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.calvuz.qreport.backup.presentation.ui.model.RestoreProgress
import net.calvuz.qreport.backup.domain.model.enum.RestoreStrategy
import net.calvuz.qreport.backup.domain.repository.BackupRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Restores data from backup with specified strategy
 */
class RestoreBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    operator fun invoke(
        dirPath: String,
        backupPath: String,
        strategy: RestoreStrategy = RestoreStrategy.REPLACE_ALL
    ): Flow<RestoreProgress> = flow {

        Timber.Forest.d("Restoring backup: $backupPath with strategy: $strategy")

        try {
            // Validate backup exists
            emit(RestoreProgress.InProgress("Validating backup...", 0.0f))

            // Delegate to repository
            backupRepository.restoreFromBackup(
                dirPath = dirPath,
                backupPath = backupPath,
                strategy = strategy
            ).collect { progress ->
                emit(progress)
            }

        } catch (e: Exception) {
            Timber.Forest.e(e, "Backup restoring failed")
            emit(RestoreProgress.Error("Restore failed: ${e.message}"))
        }
    }
}