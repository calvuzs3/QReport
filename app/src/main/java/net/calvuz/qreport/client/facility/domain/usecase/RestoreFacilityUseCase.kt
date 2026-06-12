package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Restores a facility and — if its parent client is inactive — restores the
 * client as well. Both operations run inside a single repository transaction.
 *
 * No cascade to child islands or units: only the facility itself (and its
 * parent when needed) is reactivated.
 */
class RestoreFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val getFacilityById: GetFacilityByIdUseCase
) {
    suspend operator fun invoke(facilityId: String): QrResult<Unit, QrError.FacilityError> {

        if (facilityId.isBlank()) return QrResult.Error(QrError.FacilityError.NotFound())

        // Load facility
        when (val r = getFacilityById(facilityId)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        // Restore facility — and parent if inactive
        return facilityRepository.restoreFacility(
            id = facilityId,
        ).fold(
            onSuccess = {
                Timber.d("Facility $facilityId restored")
                QrResult.Success(Unit)
            },
            onFailure = {
                Timber.e(it, "Failed to restore facility $facilityId")
                QrResult.Error(QrError.FacilityError.DeleteError(it.message))
            }
        )
    }
}