package net.calvuz.qreport.client.island.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Reactive Flow of active islands, optionally scoped to a facility.
 *
 * Flow use cases do not return QrResult — errors propagate via Flow.catch
 * in the ViewModel.
 */
class ObserveIslandsUseCase @Inject constructor(
    private val islandRepository: IslandRepository
) {
    operator fun invoke(facilityId: String? = null): Flow<List<Island>> {

        Timber.d("Observe islands for facility: ${facilityId ?: "none"}")

        val flow = if (facilityId.isNullOrBlank()) {
            islandRepository.getAllIslandsFlow()
        } else {
            islandRepository.getAllIslandsByFacilityFlow(facilityId)
        }
        return flow.map { islands ->
            islands.sortedWith(
                compareBy<Island> { it.islandType }
                    .thenBy { it.serialNumber }
            )
        }
    }
}