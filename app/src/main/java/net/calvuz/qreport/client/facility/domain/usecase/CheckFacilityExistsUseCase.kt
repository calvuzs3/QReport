package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import javax.inject.Inject

/**
 * Checks that a facility exists and returns it.
 *
 * Returns [QrError.FacilityError.NotFound] if the facility does not exist.
 */
class CheckFacilityExistsUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {
    suspend operator fun invoke(facilityId: String): QrResult<Facility, QrError.FacilityError> {
        if (facilityId.isBlank()) {
            return QrResult.Error(QrError.FacilityError.NotFound())
        }

        return facilityRepository.getFacilityById(facilityId).fold(
            onSuccess = { facility ->
                if (facility != null) QrResult.Success(facility)
                else QrResult.Error(QrError.FacilityError.NotFound())
            },
            onFailure = {
                QrResult.Error(QrError.FacilityError.LoadError(it.message))
            }
        )
    }
}