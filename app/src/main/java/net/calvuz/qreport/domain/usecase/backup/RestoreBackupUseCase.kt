package net.calvuz.qreport.domain.usecase.backup

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.calvuz.qreport.domain.model.backup.RestoreProgress
import net.calvuz.qreport.domain.model.backup.RestoreStrategy
import net.calvuz.qreport.domain.repository.backup.BackupRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Restores data from backup with specified strategy
 */
class RestoreBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    operator fun invoke(
        backupPath: String,
        strategy: RestoreStrategy = RestoreStrategy.REPLACE_ALL
    ): Flow<RestoreProgress> = flow {

        Timber.Forest.d("Restoring backup: $backupPath with strategy: $strategy")

        try {
            // Validate backup exists
            emit(RestoreProgress.InProgress("Validating backup...", 0.0f))

            // Delegate to repository
            backupRepository.restoreFromBackup(
                backupPath = backupPath,
                strategy = strategy
            ).collect { progress ->
                emit(progress)
            }

        } catch (e: Exception) {
            Timber.Forest.e(e, "Error in RestoreBackupUseCase")
            emit(RestoreProgress.Error("Restore failed: ${e.message}"))
        }
    }
}