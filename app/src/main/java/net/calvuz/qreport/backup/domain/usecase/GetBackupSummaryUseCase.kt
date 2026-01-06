package net.calvuz.qreport.backup.domain.usecase

import net.calvuz.qreport.backup.data.model.BackupSummary
import net.calvuz.qreport.backup.domain.repository.BackupRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Gets summary information about backup system status
 */
class GetBackupSummaryUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): BackupSummary {
        return try {
            Timber.Forest.d("Getting backup system summary")

            val availableBackups = backupRepository.getAvailableBackups()
            val totalSize = availableBackups.sumOf { it.totalSize }
            val lastBackup = availableBackups.maxByOrNull { it.createdAt }
            val hasPhotos = availableBackups.any { it.includesPhotos }

            BackupSummary(
                totalBackups = availableBackups.size,
                totalSize = totalSize,
                lastBackupTimestamp = lastBackup?.createdAt,
                hasPhotoBackups = hasPhotos,
                oldestBackupTimestamp = availableBackups.minByOrNull { it.createdAt }?.createdAt
            )

        } catch (e: Exception) {
            Timber.Forest.e(e, "Error getting backup summary")
            BackupSummary.Companion.empty()
        }
    }
}