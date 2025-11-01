package net.calvuz.qreport.domain.usecase.checkup

import net.calvuz.qreport.domain.model.checkup.CheckUpStatistics
import net.calvuz.qreport.domain.repository.CheckUpRepository
import javax.inject.Inject

class GetCheckUpStatsUseCase @Inject constructor(
    private val repository: CheckUpRepository
) {
    suspend operator fun invoke(checkUpId: String): Result<CheckUpStatistics> {
        return try {
            val stats = repository.getCheckUpStatistics(checkUpId)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}