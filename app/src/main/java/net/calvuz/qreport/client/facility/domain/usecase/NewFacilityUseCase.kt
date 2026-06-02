package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.usecase.CheckClientExistsUseCase
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import javax.inject.Inject

/**
 * Creates a new facility after validating the client, name uniqueness,
 * and handling primary facility change.
 *
 * Replaces the old [CreateFacilityUseCase].
 */
class NewFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val checkClientExists: CheckClientExistsUseCase,
    private val checkFacilityNameUniqueness: CheckFacilityNameUniquenessUseCase,
    private val handlePrimaryFacilityChange: HandlePrimaryFacilityChangeUseCase
) {
    suspend operator fun invoke(facility: Facility): QrResult<Unit, QrError.FacilityError> {

        // 1. Verify client exists
        when (checkClientExists(facility.clientId)) {
            is QrResult.Error -> return QrResult.Error(QrError.FacilityError.MissingClientId())
            is QrResult.Success -> Unit
        }

        // 2. Check name uniqueness
        when (val unique = checkFacilityNameUniqueness(facility.clientId, facility.name)) {
            is QrResult.Error -> return unique
            is QrResult.Success -> Unit
        }

        // 3. Handle primary facility change if needed
        if (facility.isPrimary) {
            handlePrimaryFacilityChange(facility.clientId).onFailure {
                return QrResult.Error(QrError.FacilityError.UpdateError(it.message))
            }
        }

        // 4. Persist
        return facilityRepository.createFacility(facility).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.FacilityError.CreateError(it.message)) }
        )
    }
}