package net.calvuz.qreport.client.facility.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Returns a single [Facility] by ID.
 *
 * Presentation concerns (statusBadge, type string) have been removed.
 * The reactive [observeFacility] variant is kept for Flow-based UIs.
 */
class GetFacilityByIdUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {
    suspend operator fun invoke(facilityId: String): QrResult<Facility, QrError.FacilityError> {

        Timber.v("Getting facility $facilityId")

        if (facilityId.isBlank()) {
            Timber.d("Facility ID is blank")
            return QrResult.Error(QrError.FacilityError.NotFound())
        }

        facilityRepository.getFacilityById(facilityId).fold(
            onSuccess = { facility ->
                if (facility != null) {
                    Timber.d("Loaded facility $facility")
                    return QrResult.Success(facility)
                } else {
                    Timber.d("Facility not found")
                    return QrResult.Error(QrError.FacilityError.NotFound())
                }
            },
            onFailure = {
                return QrResult.Error(QrError.FacilityError.LoadError(it.message))
            }
        )
    }

    fun observeFacility(facilityId: String): Flow<Facility?> =
        facilityRepository.getFacilityByIdFlow(facilityId)
}