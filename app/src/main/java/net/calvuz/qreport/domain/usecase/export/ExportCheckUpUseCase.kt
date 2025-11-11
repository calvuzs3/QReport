package net.calvuz.qreport.domain.usecase.export

import net.calvuz.qreport.domain.model.checkup.CheckUpStatus
import net.calvuz.qreport.domain.repository.CheckUpRepository
import net.calvuz.qreport.domain.usecase.checkup.UpdateCheckUpStatusUseCase
import javax.inject.Inject

/**
 * Use case per export e cambio status
 */
class ExportCheckUpUseCase @Inject constructor(
    private val repository: CheckUpRepository,
    private val updateCheckUpStatusUseCase: UpdateCheckUpStatusUseCase
) {
    suspend operator fun invoke(checkUpId: String): Result<Unit> {
        return try {
            val checkUp = repository.getCheckUpById(checkUpId)
                ?: return Result.failure(Exception("CheckUp not found: $checkUpId"))

            when (checkUp.status) {
                CheckUpStatus.DRAFT, CheckUpStatus.IN_PROGRESS -> {
                    return Result.failure(
                        Exception("Can only export completed check-ups")
                    )
                }
                else -> {
                    updateCheckUpStatusUseCase(checkUpId, CheckUpStatus.EXPORTED)

                }

            }

            // TODO: Implementare logica di export (Word, PDF, etc.)
            // Per ora cambiamo solo lo status


        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}