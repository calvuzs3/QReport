package net.calvuz.qreport.checkup.checkup.domain.usecase

import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.checkup.domain.repository.CheckUpRepository
import net.calvuz.qreport.checkup.status.domain.repository.CheckUpStatusMasterRepository
import net.calvuz.qreport.app.error.domain.model.QrError
import javax.inject.Inject

/**
 * Handles checkup status transitions. The transition graph and the
 * "does this status mark completion" rule are both data-driven (`checkup_statuses` +
 * `checkup_status_transitions`, see [net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster]) —
 * editable from Settings, not hardcoded here.
 */
class UpdateCheckUpStatusUseCase @Inject constructor(
    private val repository: CheckUpRepository,
    private val statusMasterRepository: CheckUpStatusMasterRepository
) {
    suspend operator fun invoke(
        checkUpId: String,
        newStatus: String
    ): QrResult<Unit, QrError.Checkup> {
        return try {
            val checkUp = repository.getCheckUpById(checkUpId)
                ?: return QrResult.Error(QrError.Checkup.NotFound())

            val newStatusMaster = statusMasterRepository.getById(newStatus).getOrNull()
            if (newStatusMaster == null || !newStatusMaster.isActive) {
                return QrResult.Error(QrError.Checkup.InvalidStatusTransition())
            }

            val isValidTransition = statusMasterRepository.isTransitionAllowed(checkUp.status, newStatus)
            if (!isValidTransition) {
                return QrResult.Error(QrError.Checkup.InvalidStatusTransition())
            }

            if (newStatusMaster.marksCompletion) {
                repository.completeCheckUp(checkUpId, newStatus)
            } else {
                repository.updateCheckUpStatus(checkUpId, newStatus)
            }

            QrResult.Success(Unit)

        } catch (e: Exception) {
            QrResult.Error(QrError.Checkup.Unknown())
        }
    }
}