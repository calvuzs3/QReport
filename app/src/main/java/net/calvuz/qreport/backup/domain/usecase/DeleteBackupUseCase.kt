package net.calvuz.qreport.backup.domain.usecase

import net.calvuz.qreport.backup.domain.repository.BackupRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case per eliminare backup specifico
 */
class DeleteBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {

    suspend operator fun invoke(backupId: String): Result<Unit> {
        return try {
            Timber.d("Deleting backup: $backupId")

            val result = backupRepository.deleteBackup(backupId)

            if (result.isSuccess) {
                Timber.d("Backup deleted successfully: $backupId")
            } else {
                Timber.e("Failed to delete backup: $backupId")
            }

            result

        } catch (e: Exception) {
            Timber.e(e, "Error deleting backup $backupId")
            Result.failure(e)
        }
    }
}