package net.calvuz.qreport.client.facility.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import net.calvuz.qreport.client.facility.domain.validator.FacilityDataValidator
import timber.log.Timber
import javax.inject.Inject

/**
 * Updates an existing facility, refreshing its [Facility.updatedAt] timestamp.
 *
 * Validates: existence, name, clientId immutability, name uniqueness, primary change.
 */
class UpdateFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val getFacilityById: GetFacilityByIdUseCase,
    private val checkFacilityNameUniqueness: CheckFacilityNameUniquenessUseCase,
    private val validateFields: FacilityDataValidator
) {
    suspend operator fun invoke(facility: Facility): QrResult<Unit, QrError.FacilityError> {

        Timber.v("Updating facility: $facility")

        // 1. Verify exists — error type matches so we can propagate directly
        val original = when (val result = getFacilityById(facility.id)) {
            is QrResult.Error -> {
                Timber.d("Facility ${facility.id} not found")
                return QrResult.Error(result.error)
            }

            is QrResult.Success -> result.data
        }

        // 2. clientId must not change
        if (facility.clientId != original.clientId) {
            Timber.d("Cannot change client of an existing facility")
            return QrResult.Error(
                QrError.FacilityError.UpdateError()
            )
        }

        // 3. Validate fields
        when (val fieldError = validateFields(facility)) {
            is QrResult.Error -> return fieldError
            is QrResult.Success -> Unit
        }

        // 4. Check name uniqueness if name changed
        if (facility.name != original.name) {
            when (val unique =
                checkFacilityNameUniqueness(facility.clientId, facility.name, facility.id)) {
                is QrResult.Error -> {
                    Timber.d("Facility name not unique: $unique")
                    return QrResult.Error(unique.error)
                }

                is QrResult.Success -> Unit
            }
        }

        // 5. Handle primary change
        if (facility.isPrimary != original.isPrimary) {
            val primaryError = handlePrimaryChange(facility, original)
            if (primaryError != null) return primaryError
        }

        // 6. Persist with refreshed timestamp
        val updated = facility.copy(updatedAt = Clock.System.now())
        facilityRepository.updateFacility(updated)
            .fold(onSuccess = { return QrResult.Success(Unit) }, onFailure = {
                Timber.d("Failed to update facility: ${it.message}")
                return QrResult.Error(QrError.FacilityError.UpdateError(it.message))
            })
    }

    private suspend fun handlePrimaryChange(
        facility: Facility, original: Facility
    ): QrResult<Unit, QrError.FacilityError>? = when {

        // Promoting to primary: delegate to repository
        facility.isPrimary && !original.isPrimary -> facilityRepository.setPrimaryFacility(
            facility.clientId,
            facility.id
        ).fold(
            onSuccess = { null },
            onFailure = { QrResult.Error(QrError.FacilityError.UpdateError(it.message)) })

        // Demoting from primary: another active facility must exist
        !facility.isPrimary && original.isPrimary -> facilityRepository.getActiveFacilitiesByClient(
            facility.clientId
        ).fold(onSuccess = { active ->
            val others = active.filter { it.id != facility.id }
            when {
                others.isEmpty() -> QrResult.Error(
                    QrError.FacilityError.UpdateError(
                        "Cannot remove primary flag: no other active facility"
                    )
                )

                else -> facilityRepository.setPrimaryFacility(
                    facility.clientId,
                    others.first().id
                ).fold(
                    onSuccess = { null },
                    onFailure = { QrResult.Error(QrError.FacilityError.UpdateError(it.message)) })
            }
        }, onFailure = { QrResult.Error(QrError.FacilityError.UpdateError(it.message)) })

        else -> null
    }
}