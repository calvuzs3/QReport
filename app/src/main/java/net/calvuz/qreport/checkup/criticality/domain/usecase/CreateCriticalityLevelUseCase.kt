package net.calvuz.qreport.checkup.criticality.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.criticality.domain.model.CriticalityMaster
import net.calvuz.qreport.checkup.criticality.domain.repository.CriticalityMasterRepository
import timber.log.Timber
import javax.inject.Inject

/** Creates a new criticality level after validating required fields and code uniqueness. */
class CreateCriticalityLevelUseCase @Inject constructor(
    private val repository: CriticalityMasterRepository
) {
    suspend operator fun invoke(level: CriticalityMaster): QrResult<Unit, QrError> {

        if (level.code.isBlank() || level.label.isBlank()) {
            return QrResult.Error(QrError.ValidationError.EmptyField())
        }

        val existing = repository.getByCode(level.code).getOrElse {
            Timber.e(it, "Failed to check code uniqueness for ${level.code}")
            return QrResult.Error(QrError.App.LoadError())
        }
        if (existing != null) {
            return QrResult.Error(QrError.ValidationError.InvalidOperation())
        }

        return repository.createCriticalityLevel(level).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = {
                Timber.e(it, "Failed to create criticality level ${level.id}")
                QrResult.Error(QrError.App.SaveError())
            }
        )
    }
}
