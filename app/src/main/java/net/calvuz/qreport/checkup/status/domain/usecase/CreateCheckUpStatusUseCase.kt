package net.calvuz.qreport.checkup.status.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster
import net.calvuz.qreport.checkup.status.domain.repository.CheckUpStatusMasterRepository
import timber.log.Timber
import javax.inject.Inject

/** Creates a new checkup status after validating required fields and code uniqueness. */
class CreateCheckUpStatusUseCase @Inject constructor(
    private val repository: CheckUpStatusMasterRepository
) {
    suspend operator fun invoke(status: CheckUpStatusMaster): QrResult<Unit, QrError> {

        if (status.code.isBlank() || status.label.isBlank()) {
            return QrResult.Error(QrError.ValidationError.EmptyField())
        }

        val existing = repository.getByCode(status.code).getOrElse {
            Timber.e(it, "Failed to check code uniqueness for ${status.code}")
            return QrResult.Error(QrError.App.LoadError())
        }
        if (existing != null) {
            return QrResult.Error(QrError.ValidationError.InvalidOperation())
        }

        return repository.createCheckUpStatus(status).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = {
                Timber.e(it, "Failed to create checkup status ${status.id}")
                QrResult.Error(QrError.App.SaveError())
            }
        )
    }
}
