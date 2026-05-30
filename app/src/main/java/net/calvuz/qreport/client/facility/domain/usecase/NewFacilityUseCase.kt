package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.usecase.CheckClientExistsUseCase
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import javax.inject.Inject

class NewFacilityUseCase @Inject constructor (
    private val facilityRepository: FacilityRepository,
    private val checkClientExists: CheckClientExistsUseCase,
    private val checkFacilityNameUniqueness: CheckFacilityNameUniquenessUseCase,
    private val handlePrimaryFacilityChange: HandlePrimaryFacilityChangeUseCase
) {

    suspend operator fun invoke(facility: Facility): Result<Unit> {
        return try {
            // 1. Validation
            //validateFacilityData(facility)

            // 2. Verify client exists
            when (checkClientExists(facility.clientId)) {
                is QrResult.Error -> return Result.failure(IllegalStateException("Client ${facility.clientId} not found"))
                is QrResult.Success -> Unit
            }

            // 3. Check for duplicates
            checkFacilityNameUniqueness(facility.clientId, facility.name)
                .onFailure { return Result.failure(it) }

            // 4. Handle primary facility
            if (facility.isPrimary) {
                handlePrimaryFacilityChange(facility.clientId)
                    .onFailure { return Result.failure(it) }
            }

            // 5. Save
            facilityRepository.createFacility(facility)
                .onFailure { return Result.failure(it) }

        }  catch (e: Exception) {
            Result.failure(e)
        }
    }
}