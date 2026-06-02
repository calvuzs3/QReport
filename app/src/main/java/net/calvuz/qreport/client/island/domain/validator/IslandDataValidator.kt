package net.calvuz.qreport.client.island.domain.validator

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.model.Island
import javax.inject.Inject

/**
 * Validates the minimum required data for an Island before create/update.
 *
 * Returns [QrResult.Success(Unit)] if valid.
 * Returns a typed [QrError.IslandError] if invalid — the presentation layer
 * resolves the user-facing string via QrErrorExt.toUiText().
 *
 * Error messages are English technical descriptions for logging only.
 */
class
IslandDataValidator @Inject constructor() {

    operator fun invoke(island: Island): QrResult<Unit, QrError.IslandError> = when {
        island.facilityId.isBlank() ->
            QrResult.Error(QrError.IslandError.MissingFacilityId())

        island.serialNumber.isBlank() ->
            QrResult.Error(QrError.IslandError.MissingSerialNumber())

        island.serialNumber.length < 3 ->
            QrResult.Error(QrError.IslandError.ValidationError.InvalidSerialNumberLength())

        island.serialNumber.length > 50 ->
            QrResult.Error(QrError.IslandError.ValidationError.InvalidSerialNumberLength())

        !isValidCode(island.serialNumber) ->
            QrResult.Error(QrError.IslandError.ValidationError.InvalidSerialNumber())

        island.commissioningNumber != null && !isValidCode(island.commissioningNumber) ->
            QrResult.Error(QrError.IslandError.ValidationError.InvalidCommissioningNumber())

        island.modelNumber != null && !isValidCode(island.modelNumber) ->
            QrResult.Error(QrError.IslandError.ValidationError.InvalidModelNumber())

        (island.customName?.length ?: 0) > 100 ->
            QrResult.Error(QrError.IslandError.ValidationError.InvalidCustomNameLength())

        (island.location?.length ?: 0) > 200 ->
            QrResult.Error(QrError.IslandError.ValidationError.InvalidLocationLength())

        island.operatingHours < 0 ->
            QrResult.Error(QrError.IslandError.ValidationError.InvalidOperatingHours())

        island.cycleCount < 0 ->
            QrResult.Error(QrError.IslandError.ValidationError.InvalidCycleCount())

        else -> QrResult.Success(Unit)
    }

    /** Alphanumeric, dashes, underscores. Blank is valid (field is optional). */
    private fun isValidCode(value: String): Boolean =
        value.isBlank() || value.matches("[A-Za-z0-9\\-_]+".toRegex())
}