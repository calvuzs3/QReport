package net.calvuz.qreport.domain.usecase.checkup

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.domain.model.checkup.CheckUp
import net.calvuz.qreport.domain.model.checkup.CheckUpStatus
import net.calvuz.qreport.domain.model.island.IslandType
import net.calvuz.qreport.domain.repository.CheckUpRepository
import javax.inject.Inject

/**
 * Use Cases per gestione check-up
 *
 * AGGIORNATO per usare:
 * - CheckItemModules (invece di CheckItemTemplates)
 * - Strutture dati esistenti (CheckUpStatistics, CheckUpProgress)
 * - CheckUpStatus con EXPORTED, ARCHIVED
 */

// ============================================================
// GET CHECKUPS USE CASES
// ============================================================

class GetCheckUpsUseCase @Inject constructor(
    private val repository: CheckUpRepository
) {
    suspend operator fun invoke(
        status: CheckUpStatus? = null,
        islandType: IslandType? = null
    ): Flow<List<CheckUp>> {
        return when {
            status != null -> repository.getCheckUpsByStatus(status)
            islandType != null -> repository.getCheckUpsByIslandType(islandType)
            else -> repository.getAllCheckUps()
        }
    }
}