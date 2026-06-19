package net.calvuz.qreport.client.island.domain.usecase

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandOperationalStatus
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Returns rich statistics for a single island.
 *
 * [MaintenanceStatus] and [WarrantyStatus] carry [labelResId] so the
 * presentation layer resolves user-facing strings without any domain strings.
 *
 * CheckUp fields (totalCheckUps, lastCheckUpDate, issuesCount) are stubbed
 * to 0 / null until the CheckUp feature is implemented.
 */
class GetIslandStatisticsUseCase @Inject constructor(
    private val islandRepository: IslandRepository
) {
    suspend operator fun invoke(islandId: String): QrResult<SingleIslandStatistics, QrError.IslandError> {

        Timber.d("Get island statistics: $islandId")

        if (islandId.isBlank()) {
            return QrResult.Error(QrError.IslandError.NotFound())
        }

        val island = islandRepository.getIslandById(islandId).fold(
            onSuccess = { it ?: return QrResult.Error(QrError.IslandError.NotFound()) },
            onFailure = { return QrResult.Error(QrError.IslandError.LoadError(it.message)) }
        )

        val now = Clock.System.now()
        return QrResult.Success(
            SingleIslandStatistics(
                islandId = island.id,
                serialNumber = island.serialNumber,
                islandType = island.islandType,
                operationalStats = calculateOperationalStats(island, now),
                maintenanceStats = calculateMaintenanceStats(island, now),
                warrantyStats = calculateWarrantyStats(island, now),
                totalCheckUps = 0,       // TODO: wire CheckUpRepository when available
                lastCheckUpDate = null,  // TODO: wire CheckUpRepository when available
                issuesCount = 0,         // TODO: wire CheckUpRepository when available
                generatedAt = now
            )
        )
    }

    // -------------------------------------------------------------------------

    private fun calculateOperationalStats(island: Island, now: Instant): OperationalStats {
        val ageInDays = island.installationDate?.let { ((now - it).inWholeDays).toInt() } ?: 0
        val avgHoursPerDay = if (ageInDays > 0) island.operatingHours / ageInDays else 0
        val avgCyclesPerHour = if (island.operatingHours > 0) (island.cycleCount / island.operatingHours).toInt() else 0
        val expectedHours = ageInDays * 16
        val uptime = if (expectedHours > 0) (island.operatingHours * 100 / expectedHours).coerceAtMost(100) else 100
        val performanceScore = when (avgCyclesPerHour) {
            in 0..20 -> 20; in 21..40 -> 40; in 41..60 -> 60; in 61..80 -> 80; else -> 100
        }
        return OperationalStats(
            operatingHours = island.operatingHours,
            cycleCount = island.cycleCount,
            ageInDays = ageInDays,
            averageHoursPerDay = avgHoursPerDay,
            averageCyclesPerHour = avgCyclesPerHour,
            uptime = uptime,
            performanceScore = performanceScore,
            isActive = island.isActive
        )
    }

    private fun calculateMaintenanceStats(island: Island, now: Instant): MaintenanceStats {
        val daysToNext = island.daysToNextMaintenance()
        val isOverdue = island.needsMaintenance()
        val daysSinceLast = island.lastMaintenanceDate?.let { ((now - it).inWholeDays).toInt() }
        val status = when {
            isOverdue -> MaintenanceStatus.OVERDUE
            daysToNext != null && daysToNext <= 7 -> MaintenanceStatus.DUE_SOON
            daysToNext != null && daysToNext <= 30 -> MaintenanceStatus.SCHEDULED
            island.lastMaintenanceDate == null -> MaintenanceStatus.NO_HISTORY
            else -> MaintenanceStatus.UP_TO_DATE
        }
        return MaintenanceStats(
            status = status,
            daysToNext = daysToNext,
            daysSinceLast = daysSinceLast,
            lastMaintenanceDate = island.lastMaintenanceDate,
            nextScheduledDate = island.nextScheduledMaintenance
        )
    }

    private fun calculateWarrantyStats(island: Island, now: Instant): WarrantyStats {
        val exp = island.warrantyExpiration
        val (status, daysRemaining) = when {
            exp == null -> WarrantyStatus.NO_INFO to null
            exp <= now -> WarrantyStatus.EXPIRED to 0L
            else -> {
                val days = (exp - now).inWholeDays
                when {
                    days <= 30  -> WarrantyStatus.EXPIRING_SOON to days
                    days <= 90  -> WarrantyStatus.EXPIRING_THIS_QUARTER to days
                    else        -> WarrantyStatus.ACTIVE to days
                }
            }
        }
        return WarrantyStats(
            status = status,
            expirationDate = exp,
            daysRemaining = daysRemaining,
            isActive = status in listOf(
                WarrantyStatus.ACTIVE,
                WarrantyStatus.EXPIRING_SOON,
                WarrantyStatus.EXPIRING_THIS_QUARTER
            )
        )
    }
}

// =============================================================================
// DATA CLASSES
// =============================================================================

data class SingleIslandStatistics(
    val islandId: String,
    val serialNumber: String,
    val islandType: String,
    val operationalStats: OperationalStats,
    val maintenanceStats: MaintenanceStats,
    val warrantyStats: WarrantyStats,
    val totalCheckUps: Int,
    val lastCheckUpDate: Instant?,
    val issuesCount: Int,
    val generatedAt: Instant
) {
    val healthScore: Int
        get() {
            var score = (operationalStats.performanceScore * 0.4).toInt()
            score += when (maintenanceStats.status) {
                MaintenanceStatus.UP_TO_DATE  -> 35
                MaintenanceStatus.SCHEDULED   -> 30
                MaintenanceStatus.DUE_SOON    -> 15
                MaintenanceStatus.OVERDUE     -> 0
                MaintenanceStatus.NO_HISTORY  -> 20
            }
            score += when (warrantyStats.status) {
                WarrantyStatus.ACTIVE                 -> 15
                WarrantyStatus.EXPIRING_THIS_QUARTER  -> 12
                WarrantyStatus.EXPIRING_SOON          -> 8
                WarrantyStatus.EXPIRED                -> 5
                WarrantyStatus.NO_INFO                -> 7
            }
            if (operationalStats.isActive) score += 10
            return score.coerceIn(0, 100)
        }
}

data class OperationalStats(
    val operatingHours: Int,
    val cycleCount: Long,
    val ageInDays: Int,
    val averageHoursPerDay: Int,
    val averageCyclesPerHour: Int,
    val uptime: Int,
    val performanceScore: Int,
    val isActive: Boolean
)

data class MaintenanceStats(
    val status: MaintenanceStatus,
    val daysToNext: Long?,
    val daysSinceLast: Int?,
    val lastMaintenanceDate: Instant?,
    val nextScheduledDate: Instant?
)

data class WarrantyStats(
    val status: WarrantyStatus,
    val expirationDate: Instant?,
    val daysRemaining: Long?,
    val isActive: Boolean
)

/**
 * Maintenance status — [labelResId] resolved via stringResource() in the UI.
 */
enum class MaintenanceStatus(val labelResId: Int) {
    UP_TO_DATE(R.string.island_maint_status_up_to_date),
    SCHEDULED(R.string.island_maint_status_scheduled),
    DUE_SOON(R.string.island_maint_status_due_soon),
    OVERDUE(R.string.island_maint_status_overdue),
    NO_HISTORY(R.string.island_maint_status_no_history)
}

/**
 * Warranty status — [labelResId] resolved via stringResource() in the UI.
 */
enum class WarrantyStatus(val labelResId: Int) {
    ACTIVE(R.string.island_warranty_active),
    EXPIRING_THIS_QUARTER(R.string.island_warranty_expiring_quarter),
    EXPIRING_SOON(R.string.island_warranty_expiring_soon),
    EXPIRED(R.string.island_warranty_expired),
    NO_INFO(R.string.island_warranty_no_info)
}