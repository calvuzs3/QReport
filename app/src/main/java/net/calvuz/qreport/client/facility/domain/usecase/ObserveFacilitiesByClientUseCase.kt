package net.calvuz.qreport.client.facility.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import timber.log.Timber
import javax.inject.Inject

class ObserveFacilitiesByClientUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
) {

    /**
     * UI reactive flow
     * Observe all facilities by a given client
     *
     * @param clientId Client ID
     * @return Facility list flow
     */
    operator fun invoke(clientId: String): Flow<List<Facility>> {

        Timber.v("Observig facilities for client $clientId")


        return facilityRepository.getFacilitiesByClientFlow(clientId).map { facilities ->
                facilities.sortedWith(compareByDescending<Facility> { it.isPrimary }.thenByDescending { it.isActive }
                    .thenBy { it.name.lowercase() })
            }
    }
}