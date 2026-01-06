package net.calvuz.qreport.checkup.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.checkup.domain.model.CheckUpHeader
import net.calvuz.qreport.checkup.domain.repository.CheckUpRepository
import javax.inject.Inject

class UpdateCheckUpHeaderUseCase @Inject constructor(
    private val repository: CheckUpRepository
) {
    suspend operator fun invoke(
        checkUpId: String,
        newHeader: CheckUpHeader
    ): Result<Unit> {
        return try {
            val checkUp = repository.getCheckUpById(checkUpId)
                ?: return Result.failure(Exception("CheckUp not found: $checkUpId"))

            val updatedCheckUp = checkUp.copy(
                header = newHeader,
                updatedAt = Clock.System.now()
            )

            repository.updateCheckUp(updatedCheckUp)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}