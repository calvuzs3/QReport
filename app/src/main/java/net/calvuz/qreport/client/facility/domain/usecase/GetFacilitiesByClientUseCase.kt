package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.model.FacilityType
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import javax.inject.Inject

/**
 * Returns facilities for a given client, sorted primary-first then by name.
 *
 * All secondary methods follow the same QrResult pattern.
 * Presentation-only helpers (stats by type, summary) are kept as domain
 * operations since they only use domain types.
 */
class GetFacilitiesByClientUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {
    suspend operator fun invoke(clientId: String): QrResult<List<Facility>, QrError.FacilityError> {
        if (clientId.isBlank()) {
            return QrResult.Error(QrError.FacilityError.LoadError("Client ID is required"))
        }

        return facilityRepository.getFacilitiesByClient(clientId).fold(
            onSuccess = { facilities ->
                QrResult.Success(facilities.sortedByPrimaryThenName())
            },
            onFailure = {
                QrResult.Error(QrError.FacilityError.LoadError(it.message))
            }
        )
    }

    suspend fun getActiveFacilitiesByClient(clientId: String): QrResult<List<Facility>, QrError.FacilityError> {
        if (clientId.isBlank()) {
            return QrResult.Error(QrError.FacilityError.LoadError("Client ID is required"))
        }
        return facilityRepository.getActiveFacilitiesByClient(clientId).fold(
            onSuccess = { QrResult.Success(it.sortedByPrimaryThenName()) },
            onFailure = { QrResult.Error(QrError.FacilityError.LoadError(it.message)) }
        )
    }

    suspend fun getPrimaryFacility(clientId: String): QrResult<Facility?, QrError.FacilityError> {
        if (clientId.isBlank()) {
            return QrResult.Error(QrError.FacilityError.LoadError("Client ID is required"))
        }
        return facilityRepository.getPrimaryFacility(clientId).fold(
            onSuccess = { QrResult.Success(it) },
            onFailure = { QrResult.Error(QrError.FacilityError.LoadError(it.message)) }
        )
    }

    suspend fun getFacilitiesByType(
        clientId: String,
        facilityType: FacilityType
    ): QrResult<List<Facility>, QrError.FacilityError> =
        when (val result = invoke(clientId)) {
            is QrResult.Error -> result
            is QrResult.Success -> QrResult.Success(result.data.filter { it.facilityType == facilityType })
        }

    suspend fun getFacilitiesCount(clientId: String): QrResult<Int, QrError.FacilityError> {
        if (clientId.isBlank()) {
            return QrResult.Error(QrError.FacilityError.LoadError("Client ID is required"))
        }
        return facilityRepository.getFacilitiesCountByClient(clientId).fold(
            onSuccess = { QrResult.Success(it) },
            onFailure = { QrResult.Error(QrError.FacilityError.LoadError(it.message)) }
        )
    }

    // -------------------------------------------------------------------------

    private fun List<Facility>.sortedByPrimaryThenName(): List<Facility> =
        sortedWith(
            compareByDescending<Facility> { it.isPrimary }
                .thenByDescending { it.isActive }
                .thenBy { it.name.lowercase() }
        )
}