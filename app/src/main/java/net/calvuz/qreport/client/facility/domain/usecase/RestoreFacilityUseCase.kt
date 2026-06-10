package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Restore a facility
 */
class RestoreFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val islandRepository: IslandRepository,
    private val getFacilityById: GetFacilityByIdUseCase
) {

    /**
     * Deactivate a facility, optionally cascading to its islands.
     */
    suspend operator fun invoke(
        facilityId: String,
    ): QrResult<Unit, QrError.FacilityError> {

        Timber.v("Restore facility $facilityId")

        // Check input
        if (facilityId.isBlank()) {
            Timber.d("Facility ID is blank")
            return QrResult.Error(QrError.FacilityError.NotFound())
        }

        // Get data
        when (val result = getFacilityById(facilityId)) {
            is QrResult.Error -> return QrResult.Error(result.error)
            is QrResult.Success -> result.data
        }

        // Deactivate facility
        facilityRepository.restoreFacility(facilityId).fold(onSuccess = {
            Timber.d("Successfully restored facility $facilityId")
            return QrResult.Success(Unit)
        }, onFailure = {
            Timber.e(it, "Failed to restore facility $facilityId")
            return QrResult.Error(QrError.FacilityError.DeleteError(it.message))
        })
    }
}