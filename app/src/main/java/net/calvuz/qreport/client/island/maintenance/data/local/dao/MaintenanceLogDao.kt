package net.calvuz.qreport.client.island.maintenance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.island.maintenance.data.local.entity.MaintenanceLogEntity

/**
 * DAO for the maintenance_logs table.
 *
 * Read contract: all normal queries filter WHERE is_deleted = 0.
 * Write contract: insert only — logs are content-immutable after creation.
 *   Lifecycle fields (is_active, is_deleted, updated_at) are updated via
 *   dedicated targeted queries, not via a generic @Update.
 *
 * Aggregate queries power [GetIslandHealthSummaryUseCase] — all are offline,
 * no external dependencies.
 */
@Dao
interface MaintenanceLogDao {

    // ===== REACTIVE QUERIES =====

    /** All active logs for an island, newest first. Used by MaintenanceTab list. */
    @Query("""
        SELECT * FROM maintenance_logs
        WHERE island_id = :islandId AND is_deleted = 0
        ORDER BY performed_at DESC
    """)
    fun getLogsForIslandFlow(islandId: String): Flow<List<MaintenanceLogEntity>>

    /** All active logs targeting a specific catalogued unit, newest first. */
    @Query("""
        SELECT * FROM maintenance_logs
        WHERE mechanical_unit_id = :unitId AND is_deleted = 0
        ORDER BY performed_at DESC
    """)
    fun getLogsForUnitFlow(unitId: String): Flow<List<MaintenanceLogEntity>>

    // ===== SUSPEND QUERIES =====

    @Query("SELECT * FROM maintenance_logs WHERE id = :id AND is_deleted = 0")
    suspend fun getLogById(id: String): MaintenanceLogEntity?

    @Query("""
        SELECT * FROM maintenance_logs
        WHERE island_id = :islandId AND is_deleted = 0
        ORDER BY performed_at DESC
    """)
    suspend fun getLogsForIsland(islandId: String): List<MaintenanceLogEntity>

    @Query("""
        SELECT * FROM maintenance_logs
        WHERE island_id = :islandId AND is_deleted = 0
        ORDER BY performed_at DESC
        LIMIT :limit
    """)
    suspend fun getRecentLogsForIsland(islandId: String, limit: Int): List<MaintenanceLogEntity>

    // ===== AGGREGATE QUERIES (IslandHealthSummary) =====

    @Query("""
        SELECT COUNT(*) FROM maintenance_logs
        WHERE island_id = :islandId AND is_deleted = 0
    """)
    suspend fun countLogsForIsland(islandId: String): Int

    @Query("""
        SELECT COUNT(*) FROM maintenance_logs
        WHERE island_id = :islandId AND operation_type = 'EMERGENCY_REPAIR' AND is_deleted = 0
    """)
    suspend fun countEmergencyLogsForIsland(islandId: String): Int

    @Query("""
        SELECT COUNT(*) FROM maintenance_logs
        WHERE island_id = :islandId
          AND outcome IN ('DEFERRED', 'REQUIRES_PARTS')
          AND is_deleted = 0
    """)
    suspend fun countDeferredLogsForIsland(islandId: String): Int

    @Query("""
        SELECT AVG(duration_minutes) FROM maintenance_logs
        WHERE island_id = :islandId AND duration_minutes IS NOT NULL AND is_deleted = 0
    """)
    suspend fun avgDurationForIsland(islandId: String): Double?

    @Query("""
        SELECT performed_at FROM maintenance_logs
        WHERE island_id = :islandId AND is_deleted = 0
        ORDER BY performed_at DESC
        LIMIT 1
    """)
    suspend fun lastPerformedAtForIsland(islandId: String): Long?

    /**
     * All performed_at timestamps for the island ordered ASC.
     * Used by [GetIslandHealthSummaryUseCase] to compute the average interval
     * between consecutive interventions.
     */
    @Query("""
        SELECT performed_at FROM maintenance_logs
        WHERE island_id = :islandId AND is_deleted = 0
        ORDER BY performed_at ASC
    """)
    suspend fun allPerformedAtForIsland(islandId: String): List<Long>

    /** Count per operation type for the island, ordered by count descending. */
    @Query("""
        SELECT operation_type, COUNT(*) as count
        FROM maintenance_logs
        WHERE island_id = :islandId AND is_deleted = 0
        GROUP BY operation_type
        ORDER BY count DESC
    """)
    suspend fun operationTypeCountsForIsland(islandId: String): List<OperationTypeCount>

    /**
     * MechanicalUnit IDs (FK) that appear in more than one log
     * within the given time window — recurrence detection.
     */
    @Query("""
        SELECT mechanical_unit_id as componentKey, COUNT(*) as count
        FROM maintenance_logs
        WHERE island_id = :islandId
          AND mechanical_unit_id IS NOT NULL
          AND performed_at >= :sinceEpochMs
          AND is_deleted = 0
        GROUP BY mechanical_unit_id
        HAVING count > 1
    """)
    suspend fun recurrentUnitIdsForIsland(
        islandId: String,
        sinceEpochMs: Long
    ): List<ComponentCount>

    /**
     * Free-text component labels that appear in more than one log
     * within the given time window — recurrence detection for uncatalogued components.
     */
    @Query("""
        SELECT component_label as componentKey, COUNT(*) as count
        FROM maintenance_logs
        WHERE island_id = :islandId
          AND mechanical_unit_id IS NULL
          AND component_label IS NOT NULL
          AND performed_at >= :sinceEpochMs
          AND is_deleted = 0
        GROUP BY component_label
        HAVING count > 1
    """)
    suspend fun recurrentComponentLabelsForIsland(
        islandId: String,
        sinceEpochMs: Long
    ): List<ComponentCount>

    /** Timestamp of the most recent REVAMPING log for the island, if any. */
    @Query("""
        SELECT performed_at FROM maintenance_logs
        WHERE island_id = :islandId AND operation_type = 'REVAMPING' AND is_deleted = 0
        ORDER BY performed_at DESC
        LIMIT 1
    """)
    suspend fun lastRevampingAtForIsland(islandId: String): Long?

    /** Count of EMERGENCY_REPAIR logs recorded on or after [sinceEpochMs]. */
    @Query("""
        SELECT COUNT(*) FROM maintenance_logs
        WHERE island_id = :islandId
          AND operation_type = 'EMERGENCY_REPAIR'
          AND performed_at >= :sinceEpochMs
          AND is_deleted = 0
    """)
    suspend fun countEmergenciesAfter(islandId: String, sinceEpochMs: Long): Int

    // ===== CRUD =====

    /** Insert a new log. ABORT on conflict — log IDs must be unique. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLog(log: MaintenanceLogEntity)

    // ===== LIFECYCLE (two-stage delete) =====

    /** Stage 1: deactivate a log (soft hide). */
    @Query("""
        UPDATE maintenance_logs
        SET is_active = 0, updated_at = :timestamp
        WHERE id = :id
    """)
    suspend fun deactivateLog(id: String, timestamp: Long)

    /** Stage 2: mark as deleted for server sync / purge. */
    @Query("""
        UPDATE maintenance_logs
        SET is_deleted = 1, updated_at = :timestamp
        WHERE id = :id
    """)
    suspend fun markLogDeleted(id: String, timestamp: Long)

    // ===== BACKUP =====

    /** Returns all logs regardless of lifecycle state — used for full backup export. */
    @Query("SELECT * FROM maintenance_logs ORDER BY performed_at ASC")
    suspend fun getAllForBackup(): List<MaintenanceLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(logs: List<MaintenanceLogEntity>)

    @Query("DELETE FROM maintenance_logs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM maintenance_logs")
    suspend fun count(): Int
}

// ===== DAO PROJECTION DATA CLASSES =====

/** Projection used by [MaintenanceLogDao.operationTypeCountsForIsland]. */
data class OperationTypeCount(
    val operation_type: String,
    val count: Int
)

/** Projection used by recurrent component queries. */
data class ComponentCount(
    val componentKey: String,
    val count: Int
)