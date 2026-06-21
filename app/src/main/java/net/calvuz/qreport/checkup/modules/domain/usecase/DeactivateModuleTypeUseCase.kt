package net.calvuz.qreport.checkup.modules.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.modules.domain.repository.ModuleTypeMasterRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Soft-deletes (deactivates) a module type. Never a hard delete: check_items keep
 * a nullable FK to module_types, so deactivating just hides it from future
 * selection dropdowns without breaking existing references.
 */
class DeactivateModuleTypeUseCase @Inject constructor(
    private val repository: ModuleTypeMasterRepository
) {
    suspend operator fun invoke(id: String): QrResult<Unit, QrError> {
        return repository.deactivateModuleType(id).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = {
                Timber.e(it, "Failed to deactivate module type $id")
                QrResult.Error(QrError.App.DeleteError())
            }
        )
    }
}
