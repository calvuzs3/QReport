package net.calvuz.qreport.checkup.modules.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.modules.domain.repository.ModuleTypeMasterRepository
import timber.log.Timber
import javax.inject.Inject

/** Replaces the set of modules associated with a given island type. */
class UpdateModuleIslandTypeLinksUseCase @Inject constructor(
    private val repository: ModuleTypeMasterRepository
) {
    suspend operator fun invoke(islandTypeId: String, moduleTypeIds: List<String>): QrResult<Unit, QrError> {
        return repository.setModuleTypesForIslandType(islandTypeId, moduleTypeIds).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = {
                Timber.e(it, "Failed to update module links for island type $islandTypeId")
                QrResult.Error(QrError.App.SaveError())
            }
        )
    }
}
