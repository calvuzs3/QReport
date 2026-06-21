package net.calvuz.qreport.checkup.modules.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.modules.domain.repository.ModuleTypeMasterRepository
import timber.log.Timber
import javax.inject.Inject

/** Reactivates a previously deactivated module type. */
class RestoreModuleTypeUseCase @Inject constructor(
    private val repository: ModuleTypeMasterRepository
) {
    suspend operator fun invoke(id: String): QrResult<Unit, QrError> {
        return repository.restoreModuleType(id).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = {
                Timber.e(it, "Failed to restore module type $id")
                QrResult.Error(QrError.App.SaveError())
            }
        )
    }
}
