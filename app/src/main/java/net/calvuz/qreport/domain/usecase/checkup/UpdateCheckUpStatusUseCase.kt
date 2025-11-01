package net.calvuz.qreport.domain.usecase.checkup

import net.calvuz.qreport.domain.model.checkup.CheckUpStatus
import net.calvuz.qreport.domain.repository.CheckUpRepository
import javax.inject.Inject

/**
 * AGGIORNATO: Gestisce i nuovi status EXPORTED, ARCHIVED
 */
class UpdateCheckUpStatusUseCase @Inject constructor(
    private val repository: CheckUpRepository
) {
    suspend operator fun invoke(
        checkUpId: String,
        newStatus: CheckUpStatus
    ): Result<Unit> {
        return try {
            val checkUp = repository.getCheckUpById(checkUpId)
                ?: return Result.failure(Exception("CheckUp not found: $checkUpId"))

            // Validazione transizioni di status (aggiornata)
            val isValidTransition = isValidStatusTransition(checkUp.status, newStatus)
            if (!isValidTransition) {
                return Result.failure(
                    Exception("Invalid status transition: ${checkUp.status} -> $newStatus")
                )
            }

            if (newStatus == CheckUpStatus.COMPLETED) {
                repository.completeCheckUp(checkUpId)
            } else {
                repository.updateCheckUpStatus(checkUpId, newStatus)
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * AGGIORNATO: Include EXPORTED e ARCHIVED
     */
    private fun isValidStatusTransition(
        currentStatus: CheckUpStatus,
        newStatus: CheckUpStatus
    ): Boolean {
        return when (currentStatus) {
            CheckUpStatus.DRAFT -> newStatus in listOf(
                CheckUpStatus.IN_PROGRESS,
                CheckUpStatus.COMPLETED
            )
            CheckUpStatus.IN_PROGRESS -> newStatus in listOf(
                CheckUpStatus.DRAFT,
                CheckUpStatus.COMPLETED
            )
            CheckUpStatus.COMPLETED -> newStatus in listOf(
                CheckUpStatus.EXPORTED
            )
            CheckUpStatus.EXPORTED -> newStatus in listOf(
                CheckUpStatus.ARCHIVED
            )
            CheckUpStatus.ARCHIVED -> false // Non si può cambiare da archiviato
        }
    }
}