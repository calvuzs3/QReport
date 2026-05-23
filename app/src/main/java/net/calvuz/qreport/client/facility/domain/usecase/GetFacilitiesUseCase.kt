package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.client.domain.usecase.CheckClientExistsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Get Facilities Use Case
 */
class GetFacilitiesUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val checkClientExists: CheckClientExistsUseCase
) {

    /**
     * Get all facilities
     *
     * @param clientId Client ID if any
     * @return Ordered facility list, by primary set and name
     */
    suspend operator fun invoke(clientId: String? = null): Result<List<Facility>> {
        Timber.d("ClientId: ${clientId ?: "nullo"}")

        return try {

            // If we have a client
            if (clientId != null) {

                // Check if client exists
                checkClientExists(clientId).onFailure { return Result.failure(it) }

                // Get facilities by Client id
                facilityRepository.getFacilitiesByClient(clientId)
                    .map { facilities ->
                        facilities.sortedWith(
                            compareByDescending<Facility> { it.isPrimary } // Primary first
                                .thenBy { it.name.lowercase() } // Then by name
                        )
                    }
            } else {

                // Get all facilities
                facilityRepository.getAllFacilities()
                    .map { facilities ->
                        facilities.sortedWith(
                            compareByDescending<Facility> { it.isPrimary } // Primary first
                                .thenBy { it.name.lowercase() } // Then by name
                        )
                    }
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}