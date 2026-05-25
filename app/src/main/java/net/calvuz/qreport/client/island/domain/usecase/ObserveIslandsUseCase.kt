package net.calvuz.qreport.client.island.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * UI flow Observe islands
 */
class ObserveIslandsUseCase @Inject constructor(
    private val islandRepository: IslandRepository,
) {

    /**
     * Observe all facilities (reactive flow)
     *
     * @param facilityId Facility ID if any
     * @return Island list Flow
     */
    operator fun invoke(facilityId: String? = null): Flow<List<Island>> {
        Timber.d("FacilityId: ${facilityId ?: "nullo"}")

        if (facilityId.isNullOrBlank()) {
            return islandRepository.getAllActiveIslandsFlow()
                .map { islands ->
                    islands.sortedWith(
                        compareBy<Island> { it.islandType }
                            .thenBy { it.serialNumber }
                    )
                }
        } else {
            return islandRepository.getAllActiveIslandsByFacilityFlow(facilityId)
                .map { facilities ->
                    facilities.sortedWith(
                        compareBy<Island> { it.islandType }
                            .thenBy { it.serialNumber }
                    )
                }
        }
    }
}