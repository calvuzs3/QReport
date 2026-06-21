package net.calvuz.qreport.checkup.status.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.status.domain.repository.CheckUpStatusMasterRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Soft-deletes (deactivates) a checkup status. Never a hard delete: checkups keep a
 * plain string reference to checkup_statuses, so deactivating just hides it from
 * future selection (chips/filters/dropdowns) without breaking existing references.
 */
class DeactivateCheckUpStatusUseCase @Inject constructor(
    private val repository: CheckUpStatusMasterRepository
) {
    suspend operator fun invoke(id: String): QrResult<Unit, QrError> {
        return repository.deactivateCheckUpStatus(id).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = {
                Timber.e(it, "Failed to deactivate checkup status $id")
                QrResult.Error(QrError.App.DeleteError())
            }
        )
    }
}
