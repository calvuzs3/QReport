package net.calvuz.qreport.client.island.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.usecase.CheckFacilityExistsUseCase
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.maintenanceIntervalFor
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import net.calvuz.qreport.client.island.domain.validator.IslandDataValidator
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

/**
 * Creates a new robotic island.
 *
 * Steps:
 * 1. Validate fields via [IslandDataValidator]
 * 2. Verify the facility exists
 * 3. Check serial number uniqueness
 * 4. Validate maintenance/warranty date consistency
 * 5. Auto-compute next maintenance if not provided
 * 6. Persist
 */
class CreateIslandUseCase @Inject constructor(
    private val islandRepository: IslandRepository,
    private val validateIslandData: IslandDataValidator,
    private val checkFacilityExists: CheckFacilityExistsUseCase,
    private val checkSerialNumberUniqueness: CheckSerialNumberUniquenessUseCase
) {
    suspend operator fun invoke(island: Island): QrResult<Unit, QrError.IslandError> {

        // 1. Validate fields
        when (val v = validateIslandData(island)) {
            is QrResult.Error -> return v
            is QrResult.Success -> Unit
        }

        // 2. Verify facility exists
        when (checkFacilityExists(island.facilityId)) {
            is QrResult.Error -> return QrResult.Error(QrError.IslandError.FacilityNotFound())
            is QrResult.Success -> Unit
        }

        // 3. Check serial number uniqueness
        when (val sn = checkSerialNumberUniqueness(island.serialNumber)) {
            is QrResult.Error -> return sn
            is QrResult.Success -> Unit
        }

        // 4. Validate date consistency
        val dateError = validateMaintenanceDates(island)
        if (dateError != null) return dateError

        // 5. Auto-compute next maintenance
        val finalIsland = autoScheduleNextMaintenance(island)

        // 6. Persist
        return islandRepository.createIsland(finalIsland).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.IslandError.CreateError(it.message)) }
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
                QrResult.Error(QrError.IslandError.InvalidWarrantyDate("Warranty expiration before installation date"))

            island.lastMaintenanceDate?.let { last ->
                island.installationDate?.let { install -> last < install }
            } == true ->
                QrResult.Error(QrError.IslandError.InvalidMaintenanceDate("Last maintenance before installation date"))

            island.lastMaintenanceDate?.let { it > now } == true ->
                QrResult.Error(QrError.IslandError.InvalidMaintenanceDate("Last maintenance date cannot be in the future"))

            island.nextScheduledMaintenance?.let { next ->
                island.lastMaintenanceDate?.let { last -> next <= last }
            } == true ->
                QrResult.Error(QrError.IslandError.InvalidMaintenanceDate("Next maintenance must be after last maintenance"))

            else -> null
        }
    }

    private fun autoScheduleNextMaintenance(island: Island): Island {
        if (island.nextScheduledMaintenance != null) return island
        val base = island.lastMaintenanceDate ?: island.installationDate ?: Clock.System.now()
        return island.copy(nextScheduledMaintenance = base + maintenanceIntervalFor(island.islandType).days)
    }
}