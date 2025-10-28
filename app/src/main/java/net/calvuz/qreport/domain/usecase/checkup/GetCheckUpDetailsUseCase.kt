package net.calvuz.qreport.domain.usecase.checkup

import net.calvuz.qreport.domain.repository.CheckUpRepository
import javax.inject.Inject

class GetCheckUpDetailsUseCase @Inject constructor(
    private val repository: CheckUpRepository
) {
    suspend operator fun invoke(checkUpId: String): Result<CheckUpDetails> {
        return try {
            val checkUp = repository.getCheckUpWithDetails(checkUpId)
                ?: return Result.failure(Exception("CheckUp not found: $checkUpId"))

            // Get all required data
            val checkItems = checkUp.checkItems // Assuming checkUp has checkItems
            val spareParts = checkUp.spareParts // Assuming checkUp has spareParts
            val statistics = repository.getCheckUpStatistics(checkUpId)
            val progress = repository.getCheckUpProgress(checkUpId)

            val details = CheckUpDetails(
                checkUp = checkUp,
                checkItems = checkItems,
                spareParts = spareParts,
                statistics = statistics,
                progress = progress
            )

            Result.success(details)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}