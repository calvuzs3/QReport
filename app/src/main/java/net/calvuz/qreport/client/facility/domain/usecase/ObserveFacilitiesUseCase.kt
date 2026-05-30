package net.calvuz.qreport.client.facility.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.client.domain.usecase.CheckClientExistsUseCase
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Reactive Flow of facilities, optionally filtered by client.
 *
 * Sort order: primary first, then alphabetical by name.
 * Flow use cases do not validate client existence synchronously —
 * an invalid clientId produces an empty flow, which the ViewModel
 * handles via its empty-state UI.
 */
class ObserveFacilitiesUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
) {
    operator fun invoke(clientId: String? = null): Flow<List<Facility>> {
        Timber.d("ObserveFacilitiesUseCase clientId=${clientId ?: "none"}")

        val flow = if (clientId.isNullOrBlank()) {
            facilityRepository.getAllFacilitiesFlow()
        } else {
            facilityRepository.getFacilitiesByClientFlow(clientId)
        }

        return flow.map { facilities ->
            facilities.sortedWith(
                compareByDescending<Facility> { it.isPrimary }
                    .thenBy { it.name.lowercase() }
            )
        }
    }
}