package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.repository.IslandTypeMasterRepository
import timber.log.Timber
import javax.inject.Inject

/** Reactivates a previously deactivated island type. */
class RestoreIslandTypeUseCase @Inject constructor(
    private val repository: IslandTypeMasterRepository
) {
    suspend operator fun invoke(id: String): QrResult<Unit, QrError> {
        return repository.restoreIslandType(id).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = {
                Timber.e(it, "Failed to restore island type $id")
                QrResult.Error(QrError.App.SaveError())
            }
        )
    }
}
