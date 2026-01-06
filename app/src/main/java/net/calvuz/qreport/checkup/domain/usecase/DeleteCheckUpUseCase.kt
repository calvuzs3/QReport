package net.calvuz.qreport.checkup.domain.usecase

import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.domain.model.CheckUpStatus
import net.calvuz.qreport.checkup.domain.repository.CheckUpRepository
import net.calvuz.qreport.app.error.domain.model.QrError
import timber.log.Timber
import javax.inject.Inject

/**
 * AGGIORNATO: Non si possono eliminare check-up EXPORTED o ARCHIVED
 */
class DeleteCheckUpUseCase @Inject constructor(
    private val repository: CheckUpRepository,
) {
    suspend operator fun invoke(checkUpId: String): QrResult<Unit, QrError.Checkup> {
        return try {
            val checkUp = repository.getCheckUpById(checkUpId)
                ?: return QrResult.Error(QrError.Checkup.NOT_FOUND) //Exception("CheckUp not found: $checkUpId"))

            // Validation: non si possono eliminare check-up completati/esportati/archiviati
            val err = when (checkUp.status) {  // in listOf(
                CheckUpStatus.COMPLETED -> QrError.Checkup.CANNOT_DELETE_COMPLETED
                CheckUpStatus.EXPORTED -> QrError.Checkup.CANNOT_DELETE_EXPORTED
                CheckUpStatus.ARCHIVED -> QrError.Checkup.CANNOT_DELETE_ARCHIVED
                else -> null
            }
            if (err!= null)
                return QrResult.Error(err)

            val result = repository.deleteCheckUp(checkUpId)

            Timber.v("Deleted checkup: $result")
            QrResult.Success(result)

        } catch (e: Exception) {
            Timber.e("Exception: ${e.message}")
            QrResult.Error(QrError.Checkup.UNKNOWN)
        }
    }
}

