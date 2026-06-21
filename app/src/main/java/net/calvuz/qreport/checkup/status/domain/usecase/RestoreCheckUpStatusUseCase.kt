package net.calvuz.qreport.checkup.status.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.status.domain.repository.CheckUpStatusMasterRepository
import timber.log.Timber
import javax.inject.Inject

/** Reactivates a previously deactivated checkup status. */
class RestoreCheckUpStatusUseCase @Inject constructor(
    private val repository: CheckUpStatusMasterRepository
) {
    suspend operator fun invoke(id: String): QrResult<Unit, QrError> {
        return repository.restoreCheckUpStatus(id).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = {
                Timber.e(it, "Failed to restore checkup status $id")
                QrResult.Error(QrError.App.SaveError())
            }
        )
    }
}
