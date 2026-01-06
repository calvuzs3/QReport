package net.calvuz.qreport.backup.domain.usecase

import net.calvuz.qreport.backup.domain.repository.BackupRepository
import net.calvuz.qreport.app.util.SizeUtils.getFormattedSize
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
            val estimatedSize = backupRepository.getEstimatedBackupSize(includePhotos)

            Timber.Forest.d("Estimated backup size (photos included: ${includePhotos}): ${estimatedSize.getFormattedSize()}")
            estimatedSize

        } catch (e: Exception) {
            Timber.Forest.e(e, "Error calculating backup size")
            0L // Return 0 if cannot calculate
        }
    }
}