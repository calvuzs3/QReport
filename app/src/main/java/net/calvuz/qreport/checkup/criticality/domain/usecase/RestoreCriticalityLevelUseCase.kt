package net.calvuz.qreport.checkup.criticality.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.criticality.domain.repository.CriticalityMasterRepository
import timber.log.Timber
import javax.inject.Inject

/** Reactivates a previously deactivated criticality level. */
class RestoreCriticalityLevelUseCase @Inject constructor(
    private val repository: CriticalityMasterRepository
) {
    suspend operator fun invoke(id: String): QrResult<Unit, QrError> {
        return repository.restoreCriticalityLevel(id).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = {
                Timber.e(it, "Failed to restore criticality level $id")
                QrResult.Error(QrError.App.SaveError())
            }
        )
    }
}
