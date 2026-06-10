package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.usecase.CheckClientExistsUseCase
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Returns a sorted list of facilities, optionally filtered by client.
 *
 * Sort order: primary first, then alphabetical by name.
 */
class GetFacilitiesUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val checkClientExists: CheckClientExistsUseCase
) {
    suspend operator fun invoke(clientId: String? = null): Result<List<Facility>> {

        Timber.v("GetFacilitiesUseCase clientId=${clientId ?: "none"}")

        try {
            return if (clientId != null) {
                // Verify client exists before querying
                when (checkClientExists(clientId)) {
                    is QrResult.Error -> {
                        return Result.failure(IllegalStateException("Client $clientId not found"))
                    }

                    is QrResult.Success -> Unit
                }

                facilityRepository.getFacilitiesByClient(clientId)
                    .map { it.sortedByPrimaryThenName() }
            } else {
                facilityRepository.getAllFacilities().map { it.sortedByPrimaryThenName() }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get facilities for clientId=${clientId ?: "none"}")
            return Result.failure(e)
        }
    }

    private fun List<Facility>.sortedByPrimaryThenName(): List<Facility> =
        sortedWith(compareByDescending<Facility> { it.isPrimary }.thenBy { it.name.lowercase() })
}