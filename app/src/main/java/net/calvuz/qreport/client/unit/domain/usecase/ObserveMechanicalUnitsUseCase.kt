package net.calvuz.qreport.client.unit.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * UI flow Observe islands
 */
class ObserveMechanicalUnitsUseCase @Inject constructor(
    private val repository: MechanicalUnitRepository
) {

    /**
     * Observe all islands (reactive flow)
     *
     * @param islandId Island ID if any
     * @return Mechanical Unit list Flow
     */
    operator fun invoke(islandId: String? = null): Flow<List<MechanicalUnit>> {
        Timber.d("FacilityId: ${islandId ?: "nullo"}")

        if (islandId.isNullOrBlank()) {
            return repository.getAllActiveMechanicalUnitFlow()
                .map { units ->
                    units.sortedWith(
                        compareBy<MechanicalUnit> { it.unitType }
                            .thenBy { it.name }
                            .thenBy { it.serialNumber }
                    )
                }
        } else {
            return repository.getAllActiveMechanicalUnitByIslandFlow(islandId)
                .map { units ->
                    units.sortedWith(
                        compareBy<MechanicalUnit> { it.unitType }
                            .thenBy { it.name }
                            .thenBy { it.serialNumber }
                    )
                }
        }
    }
}