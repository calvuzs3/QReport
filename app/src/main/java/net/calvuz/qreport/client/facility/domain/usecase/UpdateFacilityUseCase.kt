package net.calvuz.qreport.client.facility.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import javax.inject.Inject

/**
 * Updates an existing facility, refreshing its [Facility.updatedAt] timestamp.
 *
 * Validates: existence, name, clientId immutability, name uniqueness, primary change.
 */
class UpdateFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val checkFacilityExists: CheckFacilityExistsUseCase,
    private val checkFacilityNameUniqueness: CheckFacilityNameUniquenessUseCase
) {
    suspend operator fun invoke(facility: Facility): QrResult<Unit, QrError.FacilityError> {

        // 1. Verify exists — error type matches so we can propagate directly
        val original = when (val result = checkFacilityExists(facility.id)) {
            is QrResult.Error -> return QrResult.Error(result.error)
            is QrResult.Success -> result.data
        }

        // 2. clientId must not change
        if (facility.clientId != original.clientId) {
            return QrResult.Error(
                QrError.FacilityError.UpdateError("Cannot change client of an existing facility")
            )
        }

        // 3. Validate fields
        val fieldError = validateFields(facility)
        if (fieldError != null) return fieldError

        // 4. Check name uniqueness if name changed
        if (facility.name != original.name) {
            when (val unique = checkFacilityNameUniqueness(facility.clientId, facility.name, facility.id)) {
                is QrResult.Error -> return QrResult.Error(unique.error)
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
        return facilityRepository.updateFacility(updated).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.FacilityError.UpdateError(it.message)) }
        )
    }

    // -------------------------------------------------------------------------

    private fun validateFields(facility: Facility): QrResult<Unit, QrError.FacilityError>? = when {
        facility.name.isBlank() ->
            QrResult.Error(QrError.FacilityError.MissingName())
        facility.name.length < 2 ->
            QrResult.Error(QrError.FacilityError.UpdateError("Name too short (min 2 chars)"))
        facility.name.length > 100 ->
            QrResult.Error(QrError.FacilityError.UpdateError("Name too long (max 100 chars)"))
        (facility.code?.length ?: 0) > 50 ->
            QrResult.Error(QrError.FacilityError.UpdateError("Code too long (max 50 chars)"))
        (facility.notes?.length ?: 0) > 500 ->
            QrResult.Error(QrError.FacilityError.UpdateError("Notes too long (max 500 chars)"))
        else -> null
    }

    private suspend fun handlePrimaryChange(
        facility: Facility,
        original: Facility
    ): QrResult<Unit, QrError.FacilityError>? = when {

        // Promoting to primary: delegate to repository
        facility.isPrimary && !original.isPrimary ->
            facilityRepository.setPrimaryFacility(facility.clientId, facility.id).fold(
                onSuccess = { null },
                onFailure = { QrResult.Error(QrError.FacilityError.UpdateError(it.message)) }
            )

        // Demoting from primary: another active facility must exist
        !facility.isPrimary && original.isPrimary ->
            facilityRepository.getActiveFacilitiesByClient(facility.clientId).fold(
                onSuccess = { active ->
                    val others = active.filter { it.id != facility.id }
                    when {
                        others.isEmpty() ->
                            QrResult.Error(QrError.FacilityError.UpdateError(
                                "Cannot remove primary flag: no other active facility"
                            ))
                        else ->
                            facilityRepository.setPrimaryFacility(facility.clientId, others.first().id).fold(
                                onSuccess = { null },
                                onFailure = { QrResult.Error(QrError.FacilityError.UpdateError(it.message)) }
                            )
                    }
                },
                onFailure = { QrResult.Error(QrError.FacilityError.UpdateError(it.message)) }
            )

        else -> null
    }
}