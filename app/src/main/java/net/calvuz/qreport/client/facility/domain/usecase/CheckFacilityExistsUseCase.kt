package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import javax.inject.Inject

/**
 * Verifica che la facility esista e la restituisce
 */
class CheckFacilityExistsUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {

    suspend operator fun invoke(facilityId: String): Result<Facility> {
        return facilityRepository.getFacilityById(facilityId)
            .mapCatching { facility ->
                facility
                    ?: throw NoSuchElementException("Stabilimento con ID '$facilityId' non trovato")
            }
    }
}