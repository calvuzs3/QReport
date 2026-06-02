package net.calvuz.qreport.client.island.domain.usecase

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.model.maintenanceIntervalFor
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

/**
 * Records a completed maintenance for a robotic island and schedules the next one.
 *
 * Note: maintenance notes are appended without localized strings — the caller
 * supplies the [notes] text if needed, keeping the domain free of UI strings.
 */
class UpdateMaintenanceUseCase @Inject constructor(
    private val islandRepository: IslandRepository,
    private val checkIslandExists: CheckIslandExistsUseCase
) {
    suspend operator fun invoke(
        islandId: String,
        maintenanceDate: Instant = Clock.System.now(),
        resetOperatingHours: Boolean = true,
        notes: String? = null
    ): QrResult<Unit, QrError.IslandError> {

        Timber.d("Update maintanance")

        if (islandId.isBlank()) {
            Timber.d("Island id is blank")
            return QrResult.Error(QrError.IslandError.NotFound())
        }

        // Validate date range
        val now = Clock.System.now()
        if (maintenanceDate > now + 24.days) {
            return QrResult.Error(QrError.IslandError.ValidationError.InvalidMaintenanceDate())
        }
        if (maintenanceDate < now - 365.days) {
            return QrResult.Error(QrError.IslandError.ValidationError.InvalidMaintenanceDate())
        }

        // Verify island exists
        val island = when (val r = checkIslandExists(islandId)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        // Validate maintenance logic
        if (island.lastMaintenanceDate?.let { it >= maintenanceDate } == true) {
            return QrResult.Error(QrError.IslandError.ValidationError.InvalidMaintenanceDate())
        }
        if (island.installationDate?.let { it > maintenanceDate } == true) {
            return QrResult.Error(QrError.IslandError.ValidationError.InvalidMaintenanceDate())
        }

        val nextMaintenanceDate = maintenanceDate + maintenanceIntervalFor(island.islandType).days

        // Build updated notes — caller provides the text, domain only appends it
        val updatedNotes = if (notes != null) {
            val existing = island.notes?.takeIf { it.isNotBlank() }?.plus("\n") ?: ""
            "$existing$notes"
        } else {
            island.notes
        }

        return islandRepository.updateIsland(
            island.copy(
                lastMaintenanceDate = maintenanceDate,
                nextScheduledMaintenance = nextMaintenanceDate,
                operatingHours = if (resetOperatingHours) 0 else island.operatingHours,
                notes = updatedNotes,
                updatedAt = Clock.System.now()
            )
        ).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.IslandError.UpdateError(it.message)) }
        )
    }

    suspend fun updateNextScheduledMaintenance(
        islandId: String,
        nextMaintenanceDate: Instant?
    ): QrResult<Unit, QrError.IslandError> {
        if (islandId.isBlank()) return QrResult.Error(QrError.IslandError.NotFound())

        if (nextMaintenanceDate != null && nextMaintenanceDate <= Clock.System.now()) {
            return QrResult.Error(QrError.IslandError.ValidationError.InvalidMaintenanceDate())
        }

        val island = when (val r = checkIslandExists(islandId)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        return islandRepository.updateIsland(
            island.copy(nextScheduledMaintenance = nextMaintenanceDate, updatedAt = Clock.System.now())
        ).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.IslandError.UpdateError(it.message)) }
        )
    }
}