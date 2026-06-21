package net.calvuz.qreport.checkup.checkup.domain.usecase

import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.checkup.domain.repository.CheckUpRepository
import net.calvuz.qreport.checkup.status.domain.repository.CheckUpStatusMasterRepository
import net.calvuz.qreport.app.error.domain.model.QrError
import timber.log.Timber
import javax.inject.Inject

/**
 * You can forcefully delete checkups in the UI, even ones whose current status
 * normally blocks deletion (`blocksDeletion` on
 * [net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster], editable
 * from Settings — not a hardcoded set of statuses).
 */
class DeleteCheckUpUseCase @Inject constructor(
    private val repository: CheckUpRepository,
    private val statusMasterRepository: CheckUpStatusMasterRepository
) {
    suspend operator fun invoke(checkUpId: String, force: Boolean = false): QrResult<Unit, QrError.Checkup> {
        return try {
            // Check input
            val checkUp = repository.getCheckUpById(checkUpId)
                ?: return QrResult.Error(QrError.Checkup.NotFound())

            // Validation
            if (!force) {
                val blocksDeletion = statusMasterRepository.getById(checkUp.status)
                    .getOrNull()?.blocksDeletion == true
                if (blocksDeletion) return QrResult.Error(QrError.Checkup.CannotDeleteBlockedStatus())
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

