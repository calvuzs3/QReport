package net.calvuz.qreport.client.facility.domain.validator

import net.calvuz.qreport.app.app.domain.model.Address
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.model.Facility
import javax.inject.Inject

/**
 * Validates the minimum required data for a Facility before create/update.
 *
 * Returns [QrResult.Success(Unit)] if valid.
 * Returns a typed [QrError.FacilityError] if invalid — the presentation layer
 * resolves the user-facing string via [QrErrorExt.toUiText()].
 *
 * Error messages in this class are English technical descriptions for logging
 * only; they are never shown directly to the user.
 */
class FacilityDataValidator @Inject constructor() {

    operator fun invoke(facility: Facility): QrResult<Unit, QrError.FacilityError> = when {
        facility.clientId.isBlank() ->
            QrResult.Error(QrError.FacilityError.MissingClientId())

        facility.name.isBlank() ->
            QrResult.Error(QrError.FacilityError.MissingName())

        facility.name.length < 2 ->
            QrResult.Error(QrError.FacilityError.ValidationError.InvalidFacilityNameLength())

        facility.name.length > 100 ->
            QrResult.Error(QrError.FacilityError.ValidationError.InvalidFacilityNameLength())

        else -> QrResult.Success(Unit)
    }
}