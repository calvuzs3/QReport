package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.data.local.entity.IslandTypeEntity
import net.calvuz.qreport.client.island.domain.repository.IslandTypeMasterRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Updates an existing island type after validating required fields and code uniqueness
 * (excluding the record being edited).
 */
class UpdateIslandTypeUseCase @Inject constructor(
    private val repository: IslandTypeMasterRepository
) {
    suspend operator fun invoke(type: IslandTypeEntity): QrResult<Unit, QrError> {

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

        return repository.updateIslandType(type).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = {
                Timber.e(it, "Failed to update island type ${type.id}")
                QrResult.Error(QrError.App.SaveError())
            }
        )
    }
}
