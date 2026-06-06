package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import timber.log.Timber
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

        Timber.d("Checking facility exists: $facilityId")

        if (facilityId.isBlank()) {
            Timber.d("Facility ID is blank")
            return QrResult.Error(QrError.FacilityError.NotFound())
        }

        return facilityRepository.getFacilityById(facilityId).fold(
            onSuccess = { facility ->
                Timber.d("Facility found: $facility")
                if (facility != null) QrResult.Success(facility)
                else QrResult.Error(QrError.FacilityError.NotFound())
            },
            onFailure = {
                Timber.d("Failed to load facility: ${it.message}")
                QrResult.Error(QrError.FacilityError.LoadError(it.message))
            }
        )
    }
}