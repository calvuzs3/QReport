package net.calvuz.qreport.checkup.checkup.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUp
import net.calvuz.qreport.checkup.checkup.domain.repository.CheckUpRepository
import javax.inject.Inject

// ============================================================
// GET CHECKUPS USE CASES
// ============================================================

class GetCheckUpsUseCase @Inject constructor(
    private val repository: CheckUpRepository
) {
    operator fun invoke(
        status: String? = null,
        islandType: String? = null
    ): Flow<List<CheckUp>> {
        return when {
            status != null -> repository.getCheckUpsByStatus(status)
            islandType != null -> repository.getCheckUpsByIslandType(islandType)
            else -> repository.getAllCheckUps()
        }
    }
}