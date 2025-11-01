package net.calvuz.qreport.domain.usecase.checkup

import net.calvuz.qreport.domain.model.checkup.CheckItem
import net.calvuz.qreport.domain.model.checkup.CheckUp
import net.calvuz.qreport.domain.model.checkup.CheckUpProgress
import net.calvuz.qreport.domain.model.checkup.CheckUpStatistics
import net.calvuz.qreport.domain.model.spare.SparePart
import net.calvuz.qreport.domain.repository.CheckUpRepository
import net.calvuz.qreport.domain.repository.CheckItemRepository
import javax.inject.Inject

/**
 * Use Case per ottenere tutti i dettagli di un check-up
 *
 * Combina:
 * - Dati del check-up
 * - Check items
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
    private val checkItemRepository: CheckItemRepository
) {
    suspend operator fun invoke(checkUpId: String): Result<CheckUpDetails> {
        return try {
            // Ottieni il check-up base
            val checkUp = checkUpRepository.getCheckUpWithDetails(checkUpId)
                ?: return Result.failure(Exception("Check-up non trovato"))

            // Ottieni le statistiche
            val statistics = checkUpRepository.getCheckUpStatistics(checkUpId)

            // Ottieni il progresso
            val progress = checkUpRepository.getCheckUpProgress(checkUpId)

            // Crea l'oggetto completo
            val details = CheckUpDetails(
                checkUp = checkUp,
                checkItems = checkUp.checkItems,
                spareParts = checkUp.spareParts,
                statistics = statistics,
                progress = progress
            )

            Result.success(details)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}