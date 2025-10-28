package net.calvuz.qreport.domain.usecase.checkup

import net.calvuz.qreport.domain.model.CheckUpStatus
import net.calvuz.qreport.domain.repository.CheckUpRepository
import javax.inject.Inject

/**
 * AGGIORNATO: Non si possono eliminare check-up EXPORTED o ARCHIVED
 */
class DeleteCheckUpUseCase @Inject constructor(
    private val repository: CheckUpRepository
) {
    suspend operator fun invoke(checkUpId: String): Result<Unit> {
        return try {
            val checkUp = repository.getCheckUpById(checkUpId)
                ?: return Result.failure(Exception("CheckUp not found: $checkUpId"))

            // Validazione: non si possono eliminare check-up completati/esportati/archiviati
            if (checkUp.status in listOf(
                    CheckUpStatus.COMPLETED,
                    CheckUpStatus.EXPORTED,
                    CheckUpStatus.ARCHIVED
                )) {
                return Result.failure(
                    Exception("Cannot delete ${checkUp.status.displayName} check-up")
                )
            }

            repository.deleteCheckUp(checkUpId)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}