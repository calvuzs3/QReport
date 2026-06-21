package net.calvuz.qreport.checkup.modules.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster
import net.calvuz.qreport.checkup.modules.domain.repository.ModuleTypeMasterRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Updates an existing module type after validating required fields and code uniqueness
 * (excluding the record being edited).
 */
class UpdateModuleTypeUseCase @Inject constructor(
    private val repository: ModuleTypeMasterRepository
) {
    suspend operator fun invoke(type: ModuleTypeMaster): QrResult<Unit, QrError> {

        if (type.code.isBlank() || type.label.isBlank()) {
            return QrResult.Error(QrError.ValidationError.EmptyField())
        }

        val existing = repository.getByCode(type.code).getOrElse {
            Timber.e(it, "Failed to check code uniqueness for ${type.code}")
            return QrResult.Error(QrError.App.LoadError())
        }
        if (existing != null && existing.id != type.id) {
            return QrResult.Error(QrError.ValidationError.InvalidOperation())
        }

        return repository.updateModuleType(type).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = {
                Timber.e(it, "Failed to update module type ${type.id}")
                QrResult.Error(QrError.App.SaveError())
            }
        )
    }
}
