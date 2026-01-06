package net.calvuz.qreport.checkup.domain.usecase

import net.calvuz.qreport.checkup.domain.model.CheckItem
import net.calvuz.qreport.checkup.domain.model.CheckUp
import net.calvuz.qreport.checkup.domain.model.CheckUpProgress
import net.calvuz.qreport.checkup.domain.model.CheckUpSingleStatistics
import net.calvuz.qreport.checkup.domain.model.spare.SparePart
import net.calvuz.qreport.checkup.domain.repository.CheckUpRepository
import net.calvuz.qreport.photo.data.local.dao.PhotoDao
import net.calvuz.qreport.photo.data.local.mapper.toDomain
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.app.error.domain.model.QrError
import timber.log.Timber
import javax.inject.Inject

/**
 * Get a CheckUp details
 *
 * Combine:
 * - Check-up data
 * - Check items (with photos)
 * - Spare parts
 * - Stats
 * - Progress
 */

data class CheckUpDetails(
    val checkUp: CheckUp,
    val checkItems: List<CheckItem>,
    val spareParts: List<SparePart>,
    val statistics: CheckUpSingleStatistics,
    val progress: CheckUpProgress
)

class GetCheckUpDetailsUseCase @Inject constructor(
    private val checkUpRepository: CheckUpRepository,
    private val photoDao: PhotoDao  // In order to load photos
) {
    suspend operator fun invoke(checkUpId: String): QrResult<CheckUpDetails, QrError.Checkup> {
        return try {
            Timber.d("Loading check-up details for: $checkUpId")

            // 1. Get base Check-up (without photos)
            val checkUp = checkUpRepository.getCheckUpWithDetails(checkUpId)
                ?: return QrResult.Error(QrError.Checkup.NOT_FOUND)

            // 2. LOAD PHOTOS FOR EACH CHECK ITEM
            val checkItemsWithPhotos = checkUp.checkItems.map { checkItem ->
                try {
                    // Load photos for this check item
                    val photoEntities = photoDao.getPhotosByCheckItemId(checkItem.id)
                    val photos = photoEntities.map { it.toDomain() }

                    // Create new Check up with photos
                    checkItem.copy(photos = photos)

                } catch (e: Exception) {
                    Timber.w(e, "Check-up item load failed {${checkItem.id}}")
                    // In caso di errore, restituisci CheckItem senza foto
                    checkItem.copy(photos = emptyList())
                }
            }

            // 3. Debug logging
            val totalPhotos = checkItemsWithPhotos.sumOf { it.photos.size }
            Timber.v("Check-up details loaded {items=${checkItemsWithPhotos.size}, photos=$totalPhotos}")

            checkItemsWithPhotos.forEach { item ->
                if (item.photos.isNotEmpty()) {
                    Timber.v("  item: ${item.description}, photos: ${item.photos.size}")
                }
            }

            // 4. Get stats
            val statistics = checkUpRepository.getCheckUpStatistics(checkUpId)

            // 5. Get progress
            val progress = checkUpRepository.getCheckUpProgress(checkUpId)

            // 6. Final object
            val details = CheckUpDetails(
                checkUp = checkUp.copy(checkItems = checkItemsWithPhotos), // CheckUp with photos
                checkItems = checkItemsWithPhotos,  // CheckItems with photos
                spareParts = checkUp.spareParts,
                statistics = statistics,
                progress = progress
            )

            QrResult.Success( details  )

        } catch (e: Exception) {
            Timber.e(e, "CheckUp details load failed {$checkUpId}")
            QrResult.Error((QrError.Checkup.LOAD))
        }
    }
}
