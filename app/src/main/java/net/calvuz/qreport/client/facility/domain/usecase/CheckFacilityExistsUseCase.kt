package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
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
    suspend operator fun invoke(facilityId: String): QrResult<Boolean, QrError.FacilityError> {

        Timber.v("Checking facility exists: $facilityId")

        // Check input
        if (facilityId.isBlank()) {
            Timber.d("Facility ID is blank")
            return QrResult.Error(QrError.FacilityError.NotFound())
        }

        // Load data
        return facilityRepository.getFacilityById(facilityId).fold(onSuccess = { facility ->
            if (facility != null && facility.isActive) {
                Timber.v("Facility $facilityId exists")
                QrResult.Success(true)
            } else if (facility != null && !facility.isActive) {
                Timber.v("Facility $facilityId exists but is inactive")
                QrResult.Success(true)
            } else {
                Timber.d("Facility $facilityId not found or inactive")
                QrResult.Error(QrError.FacilityError.NotFound())
            }
        }, onFailure = {
            Timber.e(it, "Failed to load facility $facilityId")
            QrResult.Error(QrError.FacilityError.LoadError(it.message))
        })
    }
}