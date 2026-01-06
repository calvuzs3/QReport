package net.calvuz.qreport.checkup.domain.usecase

import net.calvuz.qreport.checkup.domain.model.CheckItemStatus
import net.calvuz.qreport.checkup.domain.repository.CheckItemRepository
import javax.inject.Inject

/**
 * Use Case per aggiornare lo status di un check item
 *
 * Gestisce:
 * - Aggiornamento status con timestamp
 * - Validazione del nuovo status
 * - Persistenza nel database
 */
class UpdateCheckItemStatusUseCase @Inject constructor(
    private val checkItemRepository: CheckItemRepository
) {
    suspend operator fun invoke(
        itemId: String,
        newStatus: CheckItemStatus
    ): Result<Unit> {
        return try {
            // Aggiorna lo status del check item
            checkItemRepository.updateCheckItemStatus(itemId, newStatus)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}