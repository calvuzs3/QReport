package net.calvuz.qreport.checkup.checkup.domain.usecase

import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUpStatus
import net.calvuz.qreport.checkup.checkup.domain.repository.CheckUpRepository
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.error.presentation.asUiText
import timber.log.Timber
import javax.inject.Inject

/**
 * You can forcefully delete checkups in the UI, even EXPORTED, ARCHIVED or COMPLETED
 */
class DeleteCheckUpUseCase @Inject constructor(
    private val repository: CheckUpRepository,
) {
    suspend operator fun invoke(checkUpId: String, force: Boolean = false): QrResult<Unit, QrError.Checkup> {
        return try {
            // Check input
            val checkUp = repository.getCheckUpById(checkUpId)
                ?: return QrResult.Error(QrError.Checkup.NotFound())

            // Validation
            if (!force) {
                val err = when (checkUp.status) {  // in listOf(
                    CheckUpStatus.COMPLETED -> QrError.Checkup.CannotDeleteCompleted()
                    CheckUpStatus.EXPORTED -> QrError.Checkup.CannotDeleteExported()
                    CheckUpStatus.ARCHIVED -> QrError.Checkup.CannotDeleteArchived()
                    else -> null
                }
                if (err != null) return QrResult.Error(err)
            }
            
            // Delete
            val result = repository.deleteCheckUp(checkUpId)

            Timber.d("Deleted checkup: $result")
            QrResult.Success(result)

        } catch (e: Exception) {
            Timber.e("Exception: ${e.message}")
            QrResult.Error(QrError.Checkup.Unknown())
        }
    }
}

