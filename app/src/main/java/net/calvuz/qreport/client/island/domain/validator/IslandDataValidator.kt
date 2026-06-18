package net.calvuz.qreport.client.island.domain.validator

import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.model.Island
import javax.inject.Inject

/**
 * Validates Island data before create/update operations.
 *
 * [invoke]         — structural field validation (required fields, lengths, formats)
 * [validateDates]  — date consistency checks (installation, warranty, maintenance)
 *
 * Returns [QrResult.Success(Unit)] when valid.
 * Returns a typed [QrError.IslandError] when invalid — the presentation layer
 * resolves the user-facing string via QrErrorExt.toUiText().
 *
 * Error messages in data class constructors are English technical descriptions
 * for logging only; they never reach the UI directly.
 */
class IslandDataValidator @Inject constructor() {

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

    /**
     * Validates date consistency across installation, warranty and maintenance fields.
     * Returns null when all dates are consistent (null = valid, no error).
     * Called after [invoke] succeeds, both in create and update flows.
     */
    fun validateDates(island: Island): QrResult<Unit, QrError.IslandError>? {
        val now = Clock.System.now()
        return when {
            island.installationDate?.let { it > now } == true ->
                QrResult.Error(QrError.IslandError.ValidationError.InvalidInstallationDate())

            island.warrantyExpiration?.let { exp ->
                island.installationDate?.let { install -> exp < install }
            } == true ->
                QrResult.Error(QrError.IslandError.ValidationError.InvalidWarrantyDate())

            island.lastMaintenanceDate?.let { last ->
                island.installationDate?.let { install -> last < install }
            } == true ->
                QrResult.Error(QrError.IslandError.ValidationError.InvalidMaintenanceDate())

            island.lastMaintenanceDate?.let { it > now } == true ->
                QrResult.Error(QrError.IslandError.ValidationError.InvalidMaintenanceDate())

            island.nextScheduledMaintenance?.let { next ->
                island.lastMaintenanceDate?.let { last -> next <= last }
            } == true ->
                QrResult.Error(QrError.IslandError.ValidationError.InvalidMaintenanceDate())

            else -> null
        }
    }

    /** Alphanumeric, dashes, underscores, dots. Blank is valid (field is optional). */
    private fun isValidCode(value: String): Boolean =
        value.isBlank() || value.matches(Regex("[A-Za-z0-9.\\-_]+"))
}