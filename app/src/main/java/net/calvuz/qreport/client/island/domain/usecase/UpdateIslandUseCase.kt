package net.calvuz.qreport.client.island.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import net.calvuz.qreport.client.island.domain.validator.IslandDataValidator
import javax.inject.Inject

/**
 * Updates an existing island, refreshing its [Island.updatedAt] timestamp.
 *
 * Validates: existence, fields, serial number uniqueness, date consistency,
 * facility immutability.
 */
class UpdateIslandUseCase @Inject constructor(
    private val islandRepository: IslandRepository,
    private val validateIslandData: IslandDataValidator,
    private val checkIslandExists: CheckIslandExistsUseCase,
    private val checkSerialNumberUniqueness: CheckSerialNumberUniquenessUseCase
) {
    suspend operator fun invoke(island: Island): QrResult<Unit, QrError.IslandError> {

        // 1. Verify exists
        val original = when (val r = checkIslandExists(island.id)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        // 2. Facility must not change
        if (island.facilityId != original.facilityId) {
            return QrResult.Error(QrError.IslandError.CannotChangeFacility())
        }

        // 3. Validate fields
        when (val v = validateIslandData(island)) {
            is QrResult.Error -> return v
            is QrResult.Success -> Unit
        }

        // 4. Check serial number uniqueness if changed
        if (island.serialNumber != original.serialNumber) {
            when (val sn = checkSerialNumberUniqueness(island.serialNumber)) {
                is QrResult.Error -> return sn
                is QrResult.Success -> Unit
            }
        }

        // 5. Validate date consistency
        val dateError = validateMaintenanceDates(island)
        if (dateError != null) return dateError

        // 6. Guard operating hours decrease without new maintenance
        val guardedIsland = guardOperatingHours(original, island)

        // 7. Persist with updated timestamp
        val updated = guardedIsland.copy(updatedAt = Clock.System.now())
        return islandRepository.updateIsland(updated).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.IslandError.UpdateError(it.message)) }
        )
    }

    // -------------------------------------------------------------------------

    private fun validateMaintenanceDates(island: Island): QrResult<Unit, QrError.IslandError>? {
        val now = Clock.System.now()
        return when {
            island.installationDate?.let { it > now } == true ->
                QrResult.Error(QrError.IslandError.InvalidInstallationDate("Installation date cannot be in the future"))

            island.warrantyExpiration?.let { exp ->
                island.installationDate?.let { install -> exp < install }
            } == true ->
                QrResult.Error(QrError.IslandError.InvalidWarrantyDate("Warranty before installation"))

            island.lastMaintenanceDate?.let { last ->
                island.installationDate?.let { install -> last < install }
            } == true ->
                QrResult.Error(QrError.IslandError.InvalidMaintenanceDate("Last maintenance before installation"))

            island.lastMaintenanceDate?.let { it > now } == true ->
                QrResult.Error(QrError.IslandError.InvalidMaintenanceDate("Last maintenance cannot be in the future"))

            island.nextScheduledMaintenance?.let { next ->
                island.lastMaintenanceDate?.let { last -> next <= last }
            } == true ->
                QrResult.Error(QrError.IslandError.InvalidMaintenanceDate("Next maintenance must be after last maintenance"))

            else -> null
        }
    }

    /**
     * Prevents operating hours from being decreased unless a new maintenance
     * record justifies the reset.
     */
    private fun guardOperatingHours(original: Island, updated: Island): Island {
        if (updated.operatingHours >= original.operatingHours) return updated
        val hasNewMaintenance = updated.lastMaintenanceDate != null &&
                updated.lastMaintenanceDate != original.lastMaintenanceDate
        return if (hasNewMaintenance) updated
        else updated.copy(operatingHours = original.operatingHours)
    }
}