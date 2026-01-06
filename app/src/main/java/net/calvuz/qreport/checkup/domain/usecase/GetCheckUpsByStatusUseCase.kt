package net.calvuz.qreport.checkup.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.checkup.domain.model.CheckUpStatus
import net.calvuz.qreport.checkup.domain.repository.CheckUpRepository
import javax.inject.Inject

class GetCheckUpsByStatusUseCase @Inject constructor(
    private val repository: CheckUpRepository
) {
    suspend operator fun invoke(status: CheckUpStatus): Flow<List<CheckUpWithStats>> {
        return repository.getCheckUpsByStatus(status)
            .map { checkUps ->
                checkUps.map { checkUp ->
                    val stats = repository.getCheckUpStatistics(checkUp.id)
                    CheckUpWithStats(
                        checkUp = checkUp,
                        statistics = stats
                    )
                }
            }
    }
}