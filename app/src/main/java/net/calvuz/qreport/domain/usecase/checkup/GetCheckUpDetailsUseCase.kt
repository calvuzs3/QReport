package net.calvuz.qreport.domain.usecase.checkup

import net.calvuz.qreport.domain.model.checkup.CheckItem
import net.calvuz.qreport.domain.model.checkup.CheckUp
import net.calvuz.qreport.domain.model.checkup.CheckUpProgress
import net.calvuz.qreport.domain.model.checkup.CheckUpSingleStatistics
import net.calvuz.qreport.domain.model.spare.SparePart
import net.calvuz.qreport.domain.repository.CheckUpRepository
import net.calvuz.qreport.data.local.dao.PhotoDao
import net.calvuz.qreport.data.mapper.toDomain
import net.calvuz.qreport.presentation.core.model.QReportState
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

/**
 * Complete Checkup details Data class
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
    suspend operator fun invoke(checkUpId: String): Result<CheckUpDetails> {
        return try {
            Timber.d("Loading Checkup details for: $checkUpId")

            // 1. Get base Check-up (without photos)
            val checkUp = checkUpRepository.getCheckUpWithDetails(checkUpId)
                ?: return Result.failure(Exception(QReportState.ERR_CHECKUP_NOT_FOUND.name))

            // 2. LOAD PHOTOS FOR EACH CHECK ITEM
            val checkItemsWithPhotos = checkUp.checkItems.map { checkItem ->
                try {
                    // Load photos for this check item
                    val photoEntities = photoDao.getPhotosByCheckItemId(checkItem.id)
                    val photos = photoEntities.map { it.toDomain() }

                    // Create new Check up with photos
                    checkItem.copy(photos = photos)

                } catch (e: Exception) {
                    Timber.w(e, "CheckItem load failed {${checkItem.id}}")
                    // In caso di errore, restituisci CheckItem senza foto
                    checkItem.copy(photos = emptyList())
                }
            }

            // 3. Debug logging
            val totalPhotos = checkItemsWithPhotos.sumOf { it.photos.size }
            Timber.d("CheckUp details loaded {items: ${checkItemsWithPhotos.size}, photos: $totalPhotos")

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

            Result.success( details  )

        } catch (e: Exception) {
            Timber.e(e, "CheckUp details load failed {$checkUpId}")
            Result.failure(Exception(QReportState.ERR_CHECKUP_LOAD_CHEKUP.name))
        }
    }
}
