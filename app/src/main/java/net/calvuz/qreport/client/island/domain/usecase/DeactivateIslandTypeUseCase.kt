package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.repository.IslandTypeMasterRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Soft-deletes (deactivates) an island type. Never a hard delete: facility_islands
 * keep a nullable FK to island_types, so deactivating just hides it from future
 * selection dropdowns without breaking existing references.
 */
class DeactivateIslandTypeUseCase @Inject constructor(
    private val repository: IslandTypeMasterRepository
) {
    suspend operator fun invoke(id: String): QrResult<Unit, QrError> {
        return repository.deactivateIslandType(id).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = {
                Timber.e(it, "Failed to deactivate island type $id")
                QrResult.Error(QrError.App.DeleteError())
            }
        )
    }
}
