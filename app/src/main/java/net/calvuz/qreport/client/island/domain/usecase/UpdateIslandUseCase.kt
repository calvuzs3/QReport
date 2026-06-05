package net.calvuz.qreport.client.island.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import net.calvuz.qreport.client.island.domain.validator.IslandDataValidator
import timber.log.Timber
import javax.inject.Inject

/**
 * Updates an existing island, refreshing its [Island.updatedAt] timestamp.
 *
 * Validates: existence, fields, serial number uniqueness if changed,
 * date consistency, facility immutability, operating hours guard.
 */
class UpdateIslandUseCase @Inject constructor(
    private val islandRepository: IslandRepository,
    private val validateIslandData: IslandDataValidator,
    private val checkIslandExists: CheckIslandExistsUseCase,
    private val checkSerialNumberUniqueness: CheckSerialNumberUniquenessUseCase
) {
    suspend operator fun invoke(island: Island): QrResult<Unit, QrError.IslandError> {

        Timber.d("Update island: ${island.id}")

        // 1. Verify exists
        val original = when (val r = checkIslandExists(island.id)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        // 2. facilityId must not change
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
        validateIslandData.validateDates(island)?.let { return it }

        // 6. Guard: prevent operating hours decrease without a new maintenance record
        val guardedIsland = guardOperatingHours(original, island)

        // 7. Persist with refreshed timestamp
        val updated = guardedIsland.copy(updatedAt = Clock.System.now())
        return islandRepository.updateIsland(updated).fold(
            onSuccess = {
                Timber.d("Island updated: ${updated.id}")
                QrResult.Success(Unit)
            },
            onFailure = {
                Timber.e("Failed to update island: ${it.message}")
                QrResult.Error(QrError.IslandError.UpdateError(it.message))
            }
        )
    }

    // -------------------------------------------------------------------------

    /**
     * Prevents operating hours from being decreased unless a new maintenance
     * record (changed [Island.lastMaintenanceDate]) justifies the reset.
     */
    private fun guardOperatingHours(original: Island, updated: Island): Island {
        if (updated.operatingHours >= original.operatingHours) return updated
        val hasNewMaintenance = updated.lastMaintenanceDate != null &&
                updated.lastMaintenanceDate != original.lastMaintenanceDate
        return if (hasNewMaintenance) updated
        else updated.copy(operatingHours = original.operatingHours)
    }
}