package net.calvuz.qreport.client.facility.domain.validator

import net.calvuz.qreport.app.app.domain.model.Address
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
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

    operator fun invoke(
        clientId: String,
        name: String,
        address: Address
    ): QrResult<Unit, QrError.FacilityError> = when {
        clientId.isBlank() ->
            QrResult.Error(QrError.FacilityError.MissingClientId())

        name.isBlank() ->
            QrResult.Error(QrError.FacilityError.MissingName())

        name.length < 2 ->
            QrResult.Error(QrError.FacilityError.ValidationError.InvalidFacilityNameLength())

        name.length > 100 ->
            QrResult.Error(QrError.FacilityError.ValidationError.InvalidFacilityNameLength())

        else -> QrResult.Success(Unit)
    }
}