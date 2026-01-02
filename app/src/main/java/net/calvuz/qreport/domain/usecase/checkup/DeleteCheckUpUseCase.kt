package net.calvuz.qreport.domain.usecase.checkup

import net.calvuz.qreport.domain.core.QrResult
import net.calvuz.qreport.domain.model.checkup.CheckUpStatus
import net.calvuz.qreport.domain.repository.CheckUpRepository
import net.calvuz.qreport.presentation.core.model.DataError
import timber.log.Timber
import javax.inject.Inject

/**
 * AGGIORNATO: Non si possono eliminare check-up EXPORTED o ARCHIVED
 */
class DeleteCheckUpUseCase @Inject constructor(
    private val repository: CheckUpRepository,
) {
    suspend operator fun invoke(checkUpId: String): QrResult<Unit, DataError.CheckupError> {
        return try {
            val checkUp = repository.getCheckUpById(checkUpId)
                ?: return QrResult.Error(DataError.CheckupError.NOT_FOUND) //Exception("CheckUp not found: $checkUpId"))

            // Validation: non si possono eliminare check-up completati/esportati/archiviati
            val err = when (checkUp.status) {  // in listOf(
                CheckUpStatus.COMPLETED -> DataError.CheckupError.CANNOT_DELETE_COMPLETED
                CheckUpStatus.EXPORTED -> DataError.CheckupError.CANNOT_DELETE_EXPORTED
                CheckUpStatus.ARCHIVED -> DataError.CheckupError.CANNOT_DELETE_ARCHIVED
                else -> null
            }
            if (err!= null)
                return QrResult.Error(err)

            val result = repository.deleteCheckUp(checkUpId)

            Timber.v("Deleted checkup: $result")
            QrResult.Success(result)

        } catch (e: Exception) {
            Timber.e("Exception: ${e.message}")
            QrResult.Error(DataError.CheckupError.UNKNOWN)
        }
    }
}

