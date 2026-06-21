package net.calvuz.qreport.checkup.criticality.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.criticality.domain.repository.CriticalityMasterRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Soft-deletes (deactivates) a criticality level. Never a hard delete: check_items
 * keep a nullable FK to criticality_levels, so deactivating just hides it from
 * future selection dropdowns without breaking existing references.
 */
class DeactivateCriticalityLevelUseCase @Inject constructor(
    private val repository: CriticalityMasterRepository
) {
    suspend operator fun invoke(id: String): QrResult<Unit, QrError> {
        return repository.deactivateCriticalityLevel(id).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = {
                Timber.e(it, "Failed to deactivate criticality level $id")
                QrResult.Error(QrError.App.DeleteError())
            }
        )
    }
}
