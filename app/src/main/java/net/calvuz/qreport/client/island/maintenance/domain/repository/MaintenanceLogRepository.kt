package net.calvuz.qreport.client.island.maintenance.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceLog
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceOperationType

/**
 * Repository interface for [MaintenanceLog] persistence.
 *
 * Layer contract:
 *  - Returns [kotlin.Result<T>] — not QrResult.
 *  - Error translation to QrResult<D, QrError> happens in use cases.
 *  - Flow variants are used by the UI for reactive list observation.
 */
interface MaintenanceLogRepository {

    // ===== REACTIVE =====

    /** Emits the active log list for an island whenever the table changes. */
    fun getLogsForIslandFlow(islandId: String): Flow<List<MaintenanceLog>>

    /** Emits the active log list for a specific catalogued unit. */
    fun getLogsForUnitFlow(unitId: String): Flow<List<MaintenanceLog>>

    // ===== SUSPEND — SINGLE =====

    suspend fun getLogById(id: String): Result<MaintenanceLog?>

    // ===== SUSPEND — LIST =====

    suspend fun getLogsForIsland(islandId: String): Result<List<MaintenanceLog>>

    suspend fun getRecentLogsForIsland(islandId: String, limit: Int): Result<List<MaintenanceLog>>

    // ===== SUSPEND — AGGREGATE (for IslandHealthSummaryUseCase) =====

    suspend fun countLogsForIsland(islandId: String): Result<Int>

    suspend fun countEmergencyLogsForIsland(islandId: String): Result<Int>

    suspend fun countDeferredLogsForIsland(islandId: String): Result<Int>

    suspend fun avgDurationForIsland(islandId: String): Result<Double?>

    suspend fun lastPerformedAtForIsland(islandId: String): Result<Instant?>

    /** All performed_at timestamps for the island, ordered ASC. */
    suspend fun allPerformedAtForIsland(islandId: String): Result<List<Instant>>

    /** Count per operation type. Returns empty map if no logs exist. */
    suspend fun operationTypeCountsForIsland(
        islandId: String
    ): Result<Map<MaintenanceOperationType, Int>>

    /**
     * Component identifiers (unit ID or free-text label) that appear in more than
     * one log within [since]. Used for recurrence detection.
     */
    suspend fun recurrentComponentsForIsland(
        islandId: String,
        since: Instant
    ): Result<List<String>>

    suspend fun lastRevampingAtForIsland(islandId: String): Result<Instant?>

    suspend fun countEmergenciesAfter(islandId: String, since: Instant): Result<Int>

    // ===== WRITE =====

    suspend fun createLog(log: MaintenanceLog): Result<Unit>

    // ===== LIFECYCLE =====

    suspend fun deactivateLog(id: String): Result<Unit>

    suspend fun markLogDeleted(id: String): Result<Unit>

    // ===== BACKUP =====

    suspend fun getAllForBackup(): Result<List<MaintenanceLog>>

    suspend fun insertAllFromBackup(logs: List<MaintenanceLog>): Result<Unit>
}