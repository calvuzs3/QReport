package net.calvuz.qreport.client.island.maintenance.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.maintenance.domain.model.IslandHealthSummary
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceOperationType
import net.calvuz.qreport.client.island.maintenance.domain.repository.MaintenanceLogRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

/**
 * Computes a predictive health summary for a robotic island from its
 * maintenance log history.
 *
 * All calculations use aggregate Room queries — no connectivity required.
 * The result is never persisted; it is always recomputed on demand.
 *
 * Key metrics produced:
 *  - Average interval between interventions → predicted next date
 *  - Emergency rate → island health indicator
 *  - Deferred rate → reliability indicator
 *  - Operation type breakdown → most frequent work category
 *  - Recurrent components (last 90 days) → recurring problem detection
 *  - Emergencies after last revamping → engineering quality indicator
 */
class GetIslandHealthSummaryUseCase @Inject constructor(
    private val logRepository: MaintenanceLogRepository
) {
    /** Window for recurrent component detection. */
    private val recurrenceWindow = 90.days

    suspend operator fun invoke(
        islandId: String
    ): QrResult<IslandHealthSummary, QrError.MaintenanceLogError> {

        // Total active log count
        val total = logRepository.countLogsForIsland(islandId).getOrElse {
            return QrResult.Error(QrError.MaintenanceLogError.LoadError(it.message))
        }

        // Early exit — no data
        if (total == 0) return QrResult.Success(IslandHealthSummary.empty(islandId))

        // ── Aggregate queries ─────────────────────────────────────────────────

        val emergencyCount = logRepository.countEmergencyLogsForIsland(islandId)
            .getOrDefault(0)

        val deferredCount = logRepository.countDeferredLogsForIsland(islandId)
            .getOrDefault(0)

        val avgDuration = logRepository.avgDurationForIsland(islandId)
            .getOrDefault(null)

        val lastDate = logRepository.lastPerformedAtForIsland(islandId)
            .getOrDefault(null)

        val allTimestamps = logRepository.allPerformedAtForIsland(islandId)
            .getOrDefault(emptyList())

        val typeCounts = logRepository.operationTypeCountsForIsland(islandId)
            .getOrDefault(emptyMap())

        val since = Clock.System.now().minus(recurrenceWindow)
        val recurrent = logRepository.recurrentComponentsForIsland(islandId, since)
            .getOrDefault(emptyList())

        val lastRevamping = logRepository.lastRevampingAtForIsland(islandId)
            .getOrDefault(null)

        val emergenciesAfterRevamping: Int? = lastRevamping?.let {
            logRepository.countEmergenciesAfter(islandId, it).getOrDefault(null)
        }

        // ── Derived metrics ───────────────────────────────────────────────────

        // Average days between consecutive interventions
        val avgDays: Double? = if (allTimestamps.size >= 2) {
            val intervals = allTimestamps.zipWithNext { a, b ->
                (b - a).inWholeDays.toDouble()
            }
            intervals.average().takeIf { it.isFinite() }
        } else null

        // Predicted next intervention date
        val predicted = if (avgDays != null && lastDate != null) {
            lastDate.plus(avgDays.days)
        } else null

        // Most frequent operation type
        val mostFrequent: MaintenanceOperationType? =
            typeCounts.entries.maxByOrNull { it.value }?.key

        // ── Assemble summary ──────────────────────────────────────────────────

        return QrResult.Success(
            IslandHealthSummary(
                islandId = islandId,
                totalLogs = total,
                avgDaysBetweenInterventions = avgDays,
                predictedNextInterventionDate = predicted,
                emergencyRate = emergencyCount.toFloat() / total,
                deferredRate = deferredCount.toFloat() / total,
                avgDurationMinutes = avgDuration,
                logsByOperationType = typeCounts,
                mostFrequentOperationType = mostFrequent,
                recurrentComponents = recurrent,
                emergenciesAfterLastRevamping = emergenciesAfterRevamping,
                lastLogDate = lastDate,
                lastOutcome = null   // resolved by the ViewModel from getRecentLogs
            )
        )
    }
}