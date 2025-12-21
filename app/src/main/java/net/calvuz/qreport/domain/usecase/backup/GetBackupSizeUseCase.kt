package net.calvuz.qreport.domain.usecase.backup

import net.calvuz.qreport.domain.repository.backup.BackupRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Estimates backup size based on options
 */
class GetBackupSizeUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(includePhotos: Boolean = true): Long {
        return try {
            Timber.Forest.d("Calculating backup size estimate: includePhotos=$includePhotos")

            val estimatedSize = backupRepository.getEstimatedBackupSize(includePhotos)

            Timber.Forest.d("Estimated backup size: ${estimatedSize / (1024 * 1024)}MB")
            estimatedSize

        } catch (e: Exception) {
            Timber.Forest.e(e, "Error calculating backup size")
            0L // Return 0 if cannot calculate
        }
    }
}