package net.calvuz.qreport.client.unit.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import net.calvuz.qreport.app.result.domain.QrResult
import timber.log.Timber
import javax.inject.Inject

/**
 * Reactive Flow of active mechanical units, optionally scoped to an island.
 */
class ObserveMechanicalUnitsUseCase @Inject constructor(
    private val repository: MechanicalUnitRepository
) {
    /**
     * Reactive Flow of active mechanical units, optionally scoped to an island.
     *
     * Sort order: unit type → name → serial number.
     *
     * Flow use cases do not return [QrResult] — errors propagate via Flow.catch()
     * in the ViewModel.
     *
     * @param islandId if provided, returns only units belonging to that island;
     *                 if null, returns all active units across all islands.
     */
    operator fun invoke(onlyActive: Boolean? = true,islandId: String? = null): Flow<List<MechanicalUnit>> {

        Timber.d("ObserveMechanicalUnitsUseCase islandId=${islandId ?: "none"}")

        val flow = if (islandId.isNullOrBlank()) {
            repository.getAMechanicalUnitFlow()
        } else {
            repository.geteMechanicalUnitByIslandFlow(islandId)
        }

        return flow.map { units ->
            units.sortedWith(
                compareBy<MechanicalUnit> { it.unitType.name }
                    .thenBy { it.name }
                    .thenBy { it.serialNumber }
            )
        }
    }
}