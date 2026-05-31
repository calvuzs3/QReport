package net.calvuz.qreport.client.facility.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
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

    fun observeFacility(facilityId: String): Flow<Facility?> =
        facilityRepository.getFacilityByIdFlow(facilityId)

    /** Returns the display name for navigation titles; degrades gracefully. */
    suspend fun getFacilityName(facilityId: String): String {
        if (facilityId.isBlank()) return ""
        return when (val result = invoke(facilityId)) {
            is QrResult.Success -> result.data.displayName
            is QrResult.Error -> ""
        }
    }

    suspend fun existsAndActive(facilityId: String): Boolean =
        when (val result = invoke(facilityId)) {
            is QrResult.Success -> result.data.isActive
            is QrResult.Error -> false
        }

    suspend fun getClientId(facilityId: String): String? =
        when (val result = invoke(facilityId)) {
            is QrResult.Success -> result.data.clientId
            is QrResult.Error -> null
        }
}