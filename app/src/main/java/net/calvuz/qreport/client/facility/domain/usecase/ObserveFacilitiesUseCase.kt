package net.calvuz.qreport.client.facility.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.client.domain.usecase.CheckClientExistsUseCase
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import timber.log.Timber
import javax.inject.Inject

/** UI flow Observe facilities */
class ObserveFacilitiesUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val checkClientExists: CheckClientExistsUseCase
) {

    /**
     * Observe all facilities (reactive flow)
     *
     * @param clientId Client ID if any
     * @return Facility list Flow
     */
    operator fun invoke(clientId: String? = null): Flow<List<Facility>> {
        Timber.d("ClientId: ${clientId ?: "nullo"}")

        if (clientId.isNullOrBlank()) {
            return facilityRepository.getAllFacilitiesFlow()
                .map { facilities ->
                    facilities.sortedWith(
                        compareByDescending<Facility> { it.isPrimary }
                            .thenBy { it.name.lowercase() }
                    )
                }
        } else {
            return facilityRepository.getFacilitiesByClientFlow(clientId)
                .map { facilities ->
                    facilities.sortedWith(
                        compareByDescending<Facility> { it.isPrimary }
                            .thenBy { it.name.lowercase() }
                    )
                }
        }
    }
}