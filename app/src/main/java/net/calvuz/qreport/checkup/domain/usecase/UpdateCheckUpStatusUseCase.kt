package net.calvuz.qreport.checkup.domain.usecase

import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.domain.model.CheckUpStatus
import net.calvuz.qreport.checkup.domain.repository.CheckUpRepository
import net.calvuz.qreport.app.error.domain.model.QrError
import javax.inject.Inject

/**
 * Handle checkup status' updates
 */
class UpdateCheckUpStatusUseCase @Inject constructor(
    private val repository: CheckUpRepository
) {
    suspend operator fun invoke(
        checkUpId: String,
        newStatus: CheckUpStatus
    ): QrResult<Unit, QrError.Checkup> {
        return try {
            val checkUp = repository.getCheckUpById(checkUpId)
                ?: return QrResult.Error(QrError.Checkup.NOT_FOUND)
//            Result.failure(Exception("CheckUp not found: $checkUpId"))

            // Validazione transizioni di status (aggiornata)
            val isValidTransition = isValidStatusTransition(checkUp.status, newStatus)
            if (!isValidTransition) {
                return QrResult.Error(QrError.Checkup.INVALID_STATUS_TRANSITION)
//                Result.failure(
//                    Exception("Invalid status transition: ${checkUp.status} -> $newStatus")
//                )
            }

            if (newStatus == CheckUpStatus.COMPLETED) {
                repository.completeCheckUp(checkUpId)
            } else {
                repository.updateCheckUpStatus(checkUpId, newStatus)
            }

            QrResult.Success(Unit)
//            Result.success(Unit)

        } catch (e: Exception) {
            QrResult.Error(QrError.Checkup.UNKNOWN)
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