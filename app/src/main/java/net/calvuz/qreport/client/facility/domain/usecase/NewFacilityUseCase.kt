package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.usecase.CheckClientExistsUseCase
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import timber.log.Timber
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
    private val revokePrimaryFacilityChangeUseCase: RevokePrimaryFacilityChangeUseCase
) {
    suspend operator fun invoke(facility: Facility): QrResult<Unit, QrError.FacilityError> {

        Timber.v("Creating new facility $facility")

        // 1. Verify client exists
        when (checkClientExists(facility.clientId)) {
            is QrResult.Error -> {
                return QrResult.Error(QrError.FacilityError.MissingClientId())
            }

            is QrResult.Success -> Unit
        }

        // 2. Check name uniqueness
        when (val unique = checkFacilityNameUniqueness(facility.clientId, facility.name)) {
            is QrResult.Error -> {
                return unique
            }

            is QrResult.Success -> Unit
        }

        // 3. Handle primary facility change if needed
        if (facility.isPrimary) {
            revokePrimaryFacilityChangeUseCase(facility.clientId).onFailure {
                return QrResult.Error(QrError.FacilityError.UpdateError(it.message))
            }
        }

        // 4. Persist
        facilityRepository.createFacility(facility).fold(onSuccess = {
            Timber.d("Successfully created new facility")
            return QrResult.Success(Unit)
        }, onFailure = {
            Timber.d(it, "Failed to create new facility")
            return QrResult.Error(QrError.FacilityError.CreateError(it.message))
        })
    }
}