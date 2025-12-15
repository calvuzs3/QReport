package net.calvuz.qreport.domain.usecase.client.facility

import net.calvuz.qreport.domain.model.client.Facility
import net.calvuz.qreport.domain.repository.FacilityRepository
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