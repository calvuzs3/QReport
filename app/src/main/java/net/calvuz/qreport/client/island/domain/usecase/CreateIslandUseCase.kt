package net.calvuz.qreport.client.island.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.usecase.CheckFacilityExistsUseCase
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.maintenanceIntervalFor
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import net.calvuz.qreport.client.island.domain.validator.IslandDataValidator
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

/**
 * Creates a new robotic island.
 *
 * Steps:
 * 1. Validate fields via [IslandDataValidator.invoke]
 * 2. Verify the facility exists
 * 3. Check serial number uniqueness
 * 4. Validate date consistency via [IslandDataValidator.validateDates]
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

        Timber.d("Create island")

        // 1. Validate fields
        when (val v = validateIslandData(island)) {
            is QrResult.Error -> {
                Timber.d("Island fields invalid: ${v.error}")
                return v
            }
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
        validateIslandData.validateDates(island)?.let { return it }

        // 5. Auto-compute next maintenance if not provided
        val finalIsland = autoScheduleNextMaintenance(island)
        Timber.d("Next maintenance scheduled: ${finalIsland.nextScheduledMaintenance}")

        // 6. Persist
        return islandRepository.createIsland(finalIsland).fold(
            onSuccess = {
                Timber.d("Island created: ${finalIsland.id}")
                QrResult.Success(Unit)
            },
            onFailure = {
                Timber.e("Failed to create island: ${it.message}")
                QrResult.Error(QrError.IslandError.CreateError(it.message))
            }
        )
    }

    // -------------------------------------------------------------------------

    private fun autoScheduleNextMaintenance(island: Island): Island {
        if (island.nextScheduledMaintenance != null) return island
        val base = island.lastMaintenanceDate ?: island.installationDate ?: Clock.System.now()
        return island.copy(nextScheduledMaintenance = base + maintenanceIntervalFor(island.islandType).days)
    }
}