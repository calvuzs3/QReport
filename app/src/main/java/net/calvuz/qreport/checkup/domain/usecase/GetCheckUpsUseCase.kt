package net.calvuz.qreport.checkup.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.domain.model.CheckUp
import net.calvuz.qreport.checkup.domain.model.CheckUpStatus
import net.calvuz.qreport.client.island.domain.model.IslandType
import net.calvuz.qreport.checkup.domain.repository.CheckUpRepository
import javax.inject.Inject

/**
 * Use Cases per gestione check-up
 *
 * AGGIORNATO per usare:
 * - CheckItemModules (invece di CheckItemTemplates)
 * - Strutture dati esistenti (CheckUpSingleStatistics, CheckUpProgress)
 * - CheckUpStatus con EXPORTED, ARCHIVED
 */

// ============================================================
// GET CHECKUPS USE CASES
// ============================================================

class GetCheckUpsUseCase @Inject constructor(
    private val repository: CheckUpRepository
) {
    operator fun invoke(
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