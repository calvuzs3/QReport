package net.calvuz.qreport.client.island.maintenance.domain.model

import kotlinx.datetime.Instant

/**
 * Computed health snapshot for a single robotic island.
 *
 * Produced by [GetIslandHealthSummaryUseCase] from aggregate Room queries.
 * Never persisted — always computed on demand from the live [MaintenanceLog] history.
 *
 * All rate values are in the range [0.0, 1.0].
 * Null values indicate insufficient data (e.g. fewer than 2 logs for interval prediction).
 */
data class IslandHealthSummary(
    val islandId: String,

    // ===== VOLUME =====
    val totalLogs: Int,

    // ===== FREQUENCY PREDICTION =====
    /** Average calendar days between any two consecutive interventions. Null if < 2 logs. */
    val avgDaysBetweenInterventions: Double?,
    /** Estimated date of the next intervention: lastPerformedAt + avgDays. Null if < 2 logs. */
    val predictedNextInterventionDate: Instant?,

    // ===== HEALTH INDICATORS =====
    /** Fraction of logs with operationType == EMERGENCY_REPAIR. High rate = poor island health. */
    val emergencyRate: Float,
    /** Fraction of logs with outcome in {DEFERRED, REQUIRES_PARTS}. High rate = reliability concern. */
    val deferredRate: Float,
    /** Average intervention duration in minutes. Null if no durationMinutes were recorded. */
    val avgDurationMinutes: Double?,

    // ===== OPERATION BREAKDOWN =====
    /** Count of logs per operation type, sorted by count descending. */
    val logsByOperationType: Map<MaintenanceOperationType, Int>,
    /** The operation type logged most frequently. Null if no logs exist. */
    val mostFrequentOperationType: MaintenanceOperationType?,

    // ===== COMPONENT RECURRENCE =====
    /**
     * Component identifiers (mechanicalUnitId or componentLabel) that appear
     * in more than one log within the last 90 days.
     * Non-empty list signals recurring problems on specific components.
     */
    val recurrentComponents: List<String>,

    // ===== POST-REVAMPING STABILITY =====
    /**
     * Number of EMERGENCY_REPAIR logs recorded after the most recent REVAMPING log.
     * Low value → good engineering quality of the revamping.
     * Null if no REVAMPING log exists in the history.
     */
    val emergenciesAfterLastRevamping: Int?,

    // ===== LAST ACTIVITY =====
    val lastLogDate: Instant?,
    val lastOutcome: MaintenanceOutcome?
) {
    companion object {

        /** Returns an empty summary when no logs exist for the island. */
        fun empty(islandId: String) = IslandHealthSummary(
            islandId = islandId,
            totalLogs = 0,
            avgDaysBetweenInterventions = null,
            predictedNextInterventionDate = null,
            emergencyRate = 0f,
            deferredRate = 0f,
            avgDurationMinutes = null,
            logsByOperationType = emptyMap(),
            mostFrequentOperationType = null,
            recurrentComponents = emptyList(),
            emergenciesAfterLastRevamping = null,
            lastLogDate = null,
            lastOutcome = null
        )
    }
}