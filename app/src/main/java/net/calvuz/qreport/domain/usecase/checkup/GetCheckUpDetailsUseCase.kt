package net.calvuz.qreport.domain.usecase.checkup

import net.calvuz.qreport.domain.model.checkup.CheckItem
import net.calvuz.qreport.domain.model.checkup.CheckUp
import net.calvuz.qreport.domain.model.checkup.CheckUpProgress
import net.calvuz.qreport.domain.model.checkup.CheckUpStatistics
import net.calvuz.qreport.domain.model.spare.SparePart
import net.calvuz.qreport.domain.repository.CheckUpRepository
import net.calvuz.qreport.domain.repository.CheckItemRepository
import net.calvuz.qreport.data.local.dao.PhotoDao
import net.calvuz.qreport.data.mapper.toDomain
import timber.log.Timber
import javax.inject.Inject

/**
 * Use Case per ottenere tutti i dettagli di un check-up
 *
 * Combina:
 * - Dati del check-up
 * - Check items (✅ CON FOTO!)
 * - Spare parts
 * - Statistiche
 * - Progresso
 */

/**
 * Data class per i dettagli completi del check-up
 */
data class CheckUpDetails(
    val checkUp: CheckUp,
    val checkItems: List<CheckItem>,
    val spareParts: List<SparePart>,
    val statistics: CheckUpStatistics,
    val progress: CheckUpProgress
)

class GetCheckUpDetailsUseCase @Inject constructor(
    private val checkUpRepository: CheckUpRepository,
    private val checkItemRepository: CheckItemRepository,
    private val photoDao: PhotoDao  // ✅ AGGIUNTO: Per caricare foto
) {
    suspend operator fun invoke(checkUpId: String): Result<CheckUpDetails> {
        return try {
            Timber.d("Loading CheckUp details for: $checkUpId")

            // 1. Ottieni il check-up base (senza foto)
            val checkUp = checkUpRepository.getCheckUpWithDetails(checkUpId)
                ?: return Result.failure(Exception("Check-up non trovato"))

            // 2. ✅ LOAD PHOTOS FOR EACH CHECK ITEM
            val checkItemsWithPhotos = checkUp.checkItems.map { checkItem ->
                try {
                    // Carica foto per questo check item specifico
                    val photoEntities = photoDao.getPhotosByCheckItemId(checkItem.id)
                    val photos = photoEntities.map { it.toDomain() }

                    // Crea nuovo CheckItem con foto caricate
                    checkItem.copy(photos = photos)

                } catch (e: Exception) {
                    Timber.w(e, "Failed to load photos for CheckItem: ${checkItem.id}")
                    // In caso di errore, restituisci CheckItem senza foto
                    checkItem.copy(photos = emptyList())
                }
            }

            // 3. Debug logging per verifica
            val totalPhotos = checkItemsWithPhotos.sumOf { it.photos.size }
            Timber.i("✅ CheckUp loaded: ${checkItemsWithPhotos.size} items, $totalPhotos photos total")

            checkItemsWithPhotos.forEach { item ->
                if (item.photos.isNotEmpty()) {
                    Timber.d("  Item '${item.description}': ${item.photos.size} photos")
                }
            }

            // 4. Ottieni le statistiche
            val statistics = checkUpRepository.getCheckUpStatistics(checkUpId)

            // 5. Ottieni il progresso
            val progress = checkUpRepository.getCheckUpProgress(checkUpId)

            // 6. Crea l'oggetto completo CON FOTO
            val details = CheckUpDetails(
                checkUp = checkUp.copy(checkItems = checkItemsWithPhotos), // ✅ CheckUp con foto
                checkItems = checkItemsWithPhotos,  // ✅ CheckItems con foto
                spareParts = checkUp.spareParts,
                statistics = statistics,
                progress = progress
            )

            Result.success(details)

        } catch (e: Exception) {
            Timber.e(e, "Failed to load CheckUp details for: $checkUpId")
            Result.failure(e)
        }
    }
}