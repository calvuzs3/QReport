package net.calvuz.qreport.checkup.status.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.status.domain.repository.CheckUpStatusMasterRepository
import timber.log.Timber
import javax.inject.Inject

/** Replaces the set of statuses reachable from a given status. */
class UpdateCheckUpStatusTransitionsUseCase @Inject constructor(
    private val repository: CheckUpStatusMasterRepository
) {
    suspend operator fun invoke(fromId: String, toIds: List<String>): QrResult<Unit, QrError> {
        return repository.setAllowedTransitions(fromId, toIds).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = {
                Timber.e(it, "Failed to update transitions from $fromId")
                QrResult.Error(QrError.App.SaveError())
            }
        )
    }
}
