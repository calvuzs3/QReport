package net.calvuz.qreport.backup.domain.usecase

import net.calvuz.qreport.backup.domain.repository.BackupRepository
import net.calvuz.qreport.app.util.SizeUtils
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case per ottenere stima dimensione backup
 * NOT USED
 */
class GetBackupSizeEstimateUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {

    suspend operator fun invoke(
        includePhotos: Boolean = true,
        includeThumbnails: Boolean = false
    ): Double {
        return try {
            Timber.Forest.d("Calcolo stima backup: photos=$includePhotos, thumbnails=$includeThumbnails")

            val estimate = backupRepository.getEstimatedBackupSize(includePhotos)

            // Applica fattori per opzioni
            val adjustedEstimate = when {
                !includePhotos -> estimate * 0.3 // Solo database + settings -70%
                includePhotos && !includeThumbnails -> estimate * 0.8 // -20%
                else -> estimate
            }

            Timber.Forest.d("Stima backup: ${SizeUtils.getFormattedSize(adjustedEstimate.toDouble())}")
            adjustedEstimate.toDouble()

        } catch (e: Exception) {
            Timber.Forest.e(e, "Errore calcolo stima backup")
            (10.0).toDouble() // Fallback estimate
        }
    }
}
