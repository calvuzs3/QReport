# QReport — 4_7 MaintenanceLog Feature Reference

**Version:** 1.0
**Date:** June 2026
**Scope:** `client/island/maintenance` — domain models, Room schema, repository, use cases, UI structure, predictive analysis

---

## 1. OVERVIEW

The MaintenanceLog feature adds a **full intervention history** to each Island.
It sits alongside the existing Island scheduling fields (`lastMaintenanceDate`,
`nextScheduledMaintenance`) — those represent the *plan*, MaintenanceLog represents
the *reality*.

```
Client → Facilities → Islands → MaintenanceLogs
                             → Mechanical Units
```

### Design decisions

| Decision | Choice | Rationale |
|---|---|---|
| Operation type | `enum MaintenanceOperationType` | Single developer/technician — extend via code commit, no in-app CRUD needed |
| Unknown ops | `OTHER` + `customOperationLabel: String?` | Escape hatch for rare or new operations |
| Component target | `mechanicalUnitId: String?` + `componentLabel: String?` | FK when unit is registered, free text when not yet catalogued |
| Delete policy | **Immutable** — no soft-delete | A performed intervention is a historical fact; errors are corrected with a follow-up log |
| Predictive analysis | Aggregate Room queries (no AI, no connectivity) | Works fully offline; good enough for interval-based and rate-based predictions |

---

## 2. DOMAIN MODELS

### 2.1 MaintenanceLog

```kotlin
// client/island/maintenance/domain/model/MaintenanceLog.kt

/**
 * A single maintenance or technical intervention performed on a robotic island.
 *
 * Immutable once created — no update use case.
 * Errors are corrected by creating a follow-up log entry.
 *
 * Two parallel fields exist for targeting a specific component:
 *  - [mechanicalUnitId] when the component is registered as a [MechanicalUnit]
 *  - [componentLabel]   free text when the component is not yet catalogued
 * If [mechanicalUnitId] is non-null, [componentLabel] is ignored.
 *
 * [customOperationLabel] is only meaningful when [operationType] is [MaintenanceOperationType.OTHER].
 */
data class MaintenanceLog(
    val id: String,
    val islandId: String,

    // ===== OPERATION =====
    val operationType: MaintenanceOperationType,
    val customOperationLabel: String? = null,   // meaningful only when operationType == OTHER

    // ===== COMPONENT TARGET =====
    val mechanicalUnitId: String? = null,       // FK → MechanicalUnit (if catalogued)
    val componentLabel: String? = null,         // free text (if not catalogued)

    // ===== DESCRIPTION =====
    val description: String,                    // always required

    // ===== TECHNICIAN SNAPSHOT =====
    val technicianName: String,
    val technicianCompany: String? = null,

    // ===== MACHINE STATE SNAPSHOT =====
    val operatingHoursAtEvent: Int? = null,     // Island.operatingHours at intervention time
    val cycleCountAtEvent: Long? = null,        // Island.cycleCount at intervention time

    // ===== OUTCOME =====
    val outcome: MaintenanceOutcome,
    val durationMinutes: Int? = null,
    val notes: String? = null,

    // ===== META =====
    val performedAt: Instant,                   // actual date/time of the intervention
    val createdAt: Instant,                     // when the record was entered in the app
    val updatedAt: Instant,                     // last write — used for sync
    val isActive: Boolean = true,               // false = first delete stage
    val isDeleted: Boolean = false              // true = second delete stage / pending purge
) {
    /**
     * Returns the effective label for the operation.
     * Falls back to [customOperationLabel] when type is OTHER.
     */
    fun effectiveOperationLabel(typeLabel: String, customLabel: String?): String =
        if (operationType == MaintenanceOperationType.OTHER && !customLabel.isNullOrBlank())
            customLabel
        else typeLabel

    /**
     * Returns the effective component description.
     * Prefers the registered MechanicalUnit name (resolved by the UI);
     * falls back to [componentLabel].
     */
    fun effectiveComponentLabel(unitName: String?): String? =
        unitName ?: componentLabel
}
```

---

### 2.2 MaintenanceOperationType

```kotlin
// client/island/maintenance/domain/model/MaintenanceOperationType.kt

/**
 * Predefined categories for maintenance operations.
 *
 * Extend this enum via a code commit when new operation types emerge in the field.
 * Use [OTHER] + [MaintenanceLog.customOperationLabel] for rare or one-off operations.
 */
@Serializable
enum class MaintenanceOperationType(val labelResId: Int) {

    // ===== SCHEDULED MAINTENANCE =====
    ROUTINE_INSPECTION(R.string.maint_op_routine_inspection),
    OIL_CHANGE(R.string.maint_op_oil_change),
    FILTER_REPLACEMENT(R.string.maint_op_filter_replacement),
    LUBRICATION(R.string.maint_op_lubrication),
    CALIBRATION(R.string.maint_op_calibration),

    // ===== COMPONENT REPLACEMENT =====
    COMPONENT_REPLACEMENT(R.string.maint_op_component_replacement),
    ENCODER_REPLACEMENT(R.string.maint_op_encoder_replacement),
    MOTOR_REPLACEMENT(R.string.maint_op_motor_replacement),
    REDUCER_REPLACEMENT(R.string.maint_op_reducer_replacement),
    SENSOR_REPLACEMENT(R.string.maint_op_sensor_replacement),
    CABLE_REPLACEMENT(R.string.maint_op_cable_replacement),

    // ===== ELECTRICAL / SOFTWARE =====
    ELECTRICAL_REPAIR(R.string.maint_op_electrical_repair),
    SOFTWARE_UPDATE(R.string.maint_op_software_update),
    PARAMETER_TUNING(R.string.maint_op_parameter_tuning),

    // ===== EXTRAORDINARY =====
    EMERGENCY_REPAIR(R.string.maint_op_emergency_repair),
    REVAMPING(R.string.maint_op_revamping),
    INSTALLATION(R.string.maint_op_installation),

    OTHER(R.string.maint_op_other);

    companion object
}
```

> **Extending the enum:** when a new operation type appears repeatedly as `OTHER + customOperationLabel`,
> add a dedicated value to this enum and update string resources. No Room migration required —
> existing `OTHER` records are unaffected.

---

### 2.3 MaintenanceOutcome

```kotlin
// client/island/maintenance/domain/model/MaintenanceOutcome.kt

/**
 * Result of a maintenance intervention.
 * Drives the emergency/deferred rate metrics in [IslandHealthSummary].
 */
@Serializable
enum class MaintenanceOutcome(val labelResId: Int) {
    COMPLETED(R.string.maint_outcome_completed),           // fully resolved
    PARTIAL(R.string.maint_outcome_partial),               // partially resolved
    DEFERRED(R.string.maint_outcome_deferred),             // postponed
    REQUIRES_PARTS(R.string.maint_outcome_requires_parts)  // waiting for spare parts
}
```

---

### 2.4 Aggregates (read-only, not persisted)

```kotlin
// client/island/maintenance/domain/model/IslandHealthSummary.kt

/**
 * Computed health snapshot for a single island.
 * Produced by [GetIslandHealthSummaryUseCase] from aggregate Room queries.
 * Never persisted — always computed on demand.
 */
data class IslandHealthSummary(
    val islandId: String,

    // ===== VOLUME =====
    val totalLogs: Int,

    // ===== FREQUENCY PREDICTION =====
    /** Average calendar days between any two consecutive interventions. */
    val avgDaysBetweenInterventions: Double?,
    /** lastPerformedAt + avgDaysBetweenInterventions. Null if < 2 logs exist. */
    val predictedNextInterventionDate: Instant?,

    // ===== HEALTH INDICATORS =====
    /** Fraction of logs with operationType == EMERGENCY_REPAIR. Range 0.0–1.0. */
    val emergencyRate: Float,
    /** Fraction of logs with outcome in {DEFERRED, REQUIRES_PARTS}. Range 0.0–1.0. */
    val deferredRate: Float,
    /** Average intervention duration in minutes. Null if no durationMinutes recorded. */
    val avgDurationMinutes: Double?,

    // ===== BREAKDOWN =====
    /** Count of logs per operation type, sorted by count descending. */
    val logsByOperationType: Map<MaintenanceOperationType, Int>,
    /** The most frequently logged operation type. */
    val mostFrequentOperationType: MaintenanceOperationType?,

    // ===== COMPONENT RECURRENCE =====
    /**
     * Components (unitId or componentLabel) that appear in more than one log
     * within the last 90 days — signals a recurring problem.
     */
    val recurrentComponents: List<String>,

    // ===== POST-REVAMPING STABILITY =====
    /**
     * Number of EMERGENCY_REPAIR logs recorded within 30 days (or 500 operating hours)
     * after the most recent REVAMPING log. Null if no REVAMPING log exists.
     * Low value → good engineering quality.
     */
    val emergenciesAfterLastRevamping: Int?,

    // ===== LAST ACTIVITY =====
    val lastLogDate: Instant?,
    val lastOutcome: MaintenanceOutcome?
)
```

---

## 3. DATABASE SCHEMA (ROOM)

### 3.1 MaintenanceLogEntity

```kotlin
// client/island/maintenance/data/local/entity/MaintenanceLogEntity.kt

/**
 * Room entity for the maintenance_logs table.
 *
 * Lifecycle fields follow the project-wide convention:
 *  - [isActive]   false = log is logically deactivated (first delete stage)
 *  - [isDeleted]  true  = log is marked for purge / server sync (second delete stage)
 *  - [updatedAt]  updated on every write; used for sync conflict resolution
 *
 * All normal queries filter WHERE is_deleted = 0.
 *
 * Component targeting:
 *  - [mechanicalUnitId] references mechanical_units.id (nullable, no FK constraint
 *    enforced at DB level to survive unit deactivation/deletion without orphan issues)
 *  - [componentLabel] is the fallback when the component is not catalogued
 */
@Entity(
    tableName = "maintenance_logs",
    foreignKeys = [
        ForeignKey(
            entity = IslandEntity::class,
            parentColumns = ["id"],
            childColumns = ["island_id"],
            onDelete = ForeignKey.CASCADE          // logs deleted with the island
        )
        // mechanical_units FK intentionally omitted — see class KDoc
    ],
    indices = [
        Index(value = ["island_id"]),
        Index(value = ["mechanical_unit_id"]),
        Index(value = ["operation_type"]),
        Index(value = ["outcome"]),
        Index(value = ["performed_at"]),
        Index(value = ["is_deleted"]),
    ]
)
data class MaintenanceLogEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "island_id")
    val islandId: String,

    // ===== OPERATION =====
    @ColumnInfo(name = "operation_type")
    val operationType: String,                      // MaintenanceOperationType.name

    @ColumnInfo(name = "custom_operation_label")
    val customOperationLabel: String? = null,

    // ===== COMPONENT TARGET =====
    @ColumnInfo(name = "mechanical_unit_id")
    val mechanicalUnitId: String? = null,

    @ColumnInfo(name = "component_label")
    val componentLabel: String? = null,

    // ===== DESCRIPTION =====
    @ColumnInfo(name = "description")
    val description: String,

    // ===== TECHNICIAN SNAPSHOT =====
    @ColumnInfo(name = "technician_name")
    val technicianName: String,

    @ColumnInfo(name = "technician_company")
    val technicianCompany: String? = null,

    // ===== MACHINE STATE SNAPSHOT =====
    @ColumnInfo(name = "operating_hours_at_event")
    val operatingHoursAtEvent: Int? = null,

    @ColumnInfo(name = "cycle_count_at_event")
    val cycleCountAtEvent: Long? = null,

    // ===== OUTCOME =====
    @ColumnInfo(name = "outcome")
    val outcome: String,                            // MaintenanceOutcome.name

    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    // ===== META =====
    @ColumnInfo(name = "performed_at")
    val performedAt: Long,                          // epoch milliseconds

    @ColumnInfo(name = "created_at")
    val createdAt: Long,                            // epoch milliseconds

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,                            // epoch milliseconds — updated on every write

    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Boolean = true,                   // false = first delete stage

    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false                  // true = second delete stage / pending purge
)
```

> **No FK on `mechanical_unit_id`:** avoids orphan constraint violations if a unit
> is deactivated or deleted after the log was created. Referential integrity is
> enforced at the use case level.

---

### 3.2 MaintenanceLogDao

```kotlin
// client/island/maintenance/data/local/dao/MaintenanceLogDao.kt

@Dao
interface MaintenanceLogDao {

    // ===== REACTIVE QUERIES =====

    /** All logs for an island, newest first. Used by MaintenanceTab. */
    @Query("""
        SELECT * FROM maintenance_logs
        WHERE island_id = :islandId AND is_deleted = 0
        ORDER BY performed_at DESC
    """)
    fun getLogsForIslandFlow(islandId: String): Flow<List<MaintenanceLogEntity>>

    /** Logs for a specific catalogued unit, newest first. */
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

    // ===== AGGREGATE QUERIES (for IslandHealthSummary) =====

    @Query("SELECT COUNT(*) FROM maintenance_logs WHERE island_id = :islandId AND is_deleted = 0")
    suspend fun countLogsForIsland(islandId: String): Int

    @Query("""
        SELECT COUNT(*) FROM maintenance_logs
        WHERE island_id = :islandId AND operation_type = 'EMERGENCY_REPAIR' AND is_deleted = 0
    """)
    suspend fun countEmergencyLogsForIsland(islandId: String): Int

    @Query("""
        SELECT COUNT(*) FROM maintenance_logs
        WHERE island_id = :islandId AND outcome IN ('DEFERRED', 'REQUIRES_PARTS') AND is_deleted = 0
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

    /** Returns all performed_at timestamps ordered ASC — used to compute avg interval. */
    @Query("""
        SELECT performed_at FROM maintenance_logs
        WHERE island_id = :islandId AND is_deleted = 0
        ORDER BY performed_at ASC
    """)
    suspend fun allPerformedAtForIsland(islandId: String): List<Long>

    /** Count per operation_type for the island. */
    @Query("""
        SELECT operation_type, COUNT(*) as count
        FROM maintenance_logs
        WHERE island_id = :islandId AND is_deleted = 0
        GROUP BY operation_type
        ORDER BY count DESC
    """)
    suspend fun operationTypeCountsForIsland(islandId: String): List<OperationTypeCount>

    /**
     * Components (mechanical_unit_id or component_label) that appear more than once
     * in logs within the last [sinceEpochMs] — recurrence detection.
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
    suspend fun recurrentUnitIdsForIsland(islandId: String, sinceEpochMs: Long): List<ComponentCount>

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
    suspend fun recurrentComponentLabelsForIsland(islandId: String, sinceEpochMs: Long): List<ComponentCount>

    /** Timestamp of the most recent REVAMPING log, if any. */
    @Query("""
        SELECT performed_at FROM maintenance_logs
        WHERE island_id = :islandId AND operation_type = 'REVAMPING' AND is_deleted = 0
        ORDER BY performed_at DESC
        LIMIT 1
    """)
    suspend fun lastRevampingAtForIsland(islandId: String): Long?

    /** Emergency logs after a given timestamp — used for post-revamping stability. */
    @Query("""
        SELECT COUNT(*) FROM maintenance_logs
        WHERE island_id = :islandId
          AND operation_type = 'EMERGENCY_REPAIR'
          AND performed_at >= :sinceEpochMs
          AND is_deleted = 0
    """)
    suspend fun countEmergenciesAfter(islandId: String, sinceEpochMs: Long): Int

    // ===== CRUD =====

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLog(log: MaintenanceLogEntity)

    // No update — logs are immutable.
    // No delete — logs are permanent historical records.
}

// ===== INTERMEDIATE DATA CLASSES (DAO projections) =====

/** Used by operationTypeCountsForIsland. */
data class OperationTypeCount(
    val operation_type: String,
    val count: Int
)

/** Used by recurrent component queries. */
data class ComponentCount(
    val componentKey: String,
    val count: Int
)
```

---

## 4. REPOSITORY

### 4.1 MaintenanceLogRepository interface

```kotlin
// client/island/maintenance/domain/repository/MaintenanceLogRepository.kt

interface MaintenanceLogRepository {

    // ===== REACTIVE =====
    fun getLogsForIslandFlow(islandId: String): Flow<List<MaintenanceLog>>
    fun getLogsForUnitFlow(unitId: String): Flow<List<MaintenanceLog>>

    // ===== SUSPEND =====
    suspend fun getLogById(id: String): Result<MaintenanceLog?>
    suspend fun getLogsForIsland(islandId: String): Result<List<MaintenanceLog>>
    suspend fun getRecentLogsForIsland(islandId: String, limit: Int): Result<List<MaintenanceLog>>

    // ===== AGGREGATE =====
    suspend fun countLogsForIsland(islandId: String): Result<Int>
    suspend fun countEmergencyLogsForIsland(islandId: String): Result<Int>
    suspend fun countDeferredLogsForIsland(islandId: String): Result<Int>
    suspend fun avgDurationForIsland(islandId: String): Result<Double?>
    suspend fun lastPerformedAtForIsland(islandId: String): Result<Instant?>
    suspend fun allPerformedAtForIsland(islandId: String): Result<List<Instant>>
    suspend fun operationTypeCountsForIsland(islandId: String): Result<Map<MaintenanceOperationType, Int>>
    suspend fun recurrentComponentsForIsland(islandId: String, since: Instant): Result<List<String>>
    suspend fun lastRevampingAtForIsland(islandId: String): Result<Instant?>
    suspend fun countEmergenciesAfter(islandId: String, since: Instant): Result<Int>

    // ===== WRITE =====
    suspend fun createLog(log: MaintenanceLog): Result<Unit>
}
```

> The repository layer uses `kotlin.Result<T>`.
> Error translation to `QrResult<D, QrError>` happens in use cases.

---

## 5. ERROR MODEL

```kotlin
// app/error/domain/model/QrError.kt  (addendum)

sealed class QrError {

    // ... existing errors ...

    sealed class MaintenanceLogError : QrError() {

        // ── Validation ────────────────────────────────────────────────────────
        data class MissingDescription(val message: String? = null) : MaintenanceLogError()
        data class MissingTechnicianName(val message: String? = null) : MaintenanceLogError()
        data class MissingCustomLabel(val message: String? = null) : MaintenanceLogError()
        /** performedAt is in the future. */
        data class InvalidPerformedAt(val message: String? = null) : MaintenanceLogError()

        // ── Business rules ────────────────────────────────────────────────────
        /** The referenced island does not exist or is inactive. */
        data class IslandNotFound(val message: String? = null) : MaintenanceLogError()
        /** The referenced MechanicalUnit does not belong to the given island. */
        data class UnitNotInIsland(val message: String? = null) : MaintenanceLogError()

        // ── Persistence ───────────────────────────────────────────────────────
        data class CreateError(val message: String? = null) : MaintenanceLogError()
        data class LoadError(val message: String? = null) : MaintenanceLogError()
    }
}
```

---

## 6. USE CASES

### 6.1 Full list

```
CreateMaintenanceLogUseCase
GetLogsForIslandUseCase              // returns Flow
GetLogsForUnitUseCase                // returns Flow
GetLogByIdUseCase
GetRecentLogsForIslandUseCase        // last N logs
GetIslandHealthSummaryUseCase        // predictive analysis
```

---

### 6.2 CreateMaintenanceLogUseCase

```kotlin
// client/island/maintenance/domain/usecase/CreateMaintenanceLogUseCase.kt

class CreateMaintenanceLogUseCase @Inject constructor(
    private val logRepository: MaintenanceLogRepository,
    private val checkIslandExists: CheckIslandExistsUseCase,
    private val checkUnitExists: CheckUnitExistsUseCase
) {
    suspend operator fun invoke(
        log: MaintenanceLog
    ): QrResult<Unit, QrError.MaintenanceLogError> {

        // 1. Description required
        if (log.description.isBlank())
            return QrResult.Error(QrError.MaintenanceLogError.MissingDescription())

        // 2. Technician name required
        if (log.technicianName.isBlank())
            return QrResult.Error(QrError.MaintenanceLogError.MissingTechnicianName())

        // 3. OTHER must have a custom label
        if (log.operationType == MaintenanceOperationType.OTHER &&
            log.customOperationLabel.isNullOrBlank()
        ) return QrResult.Error(QrError.MaintenanceLogError.MissingCustomLabel())

        // 4. performedAt must not be in the future
        if (log.performedAt > Clock.System.now())
            return QrResult.Error(QrError.MaintenanceLogError.InvalidPerformedAt())

        // 5. Island must exist
        when (checkIslandExists(log.islandId)) {
            is QrResult.Error -> return QrResult.Error(QrError.MaintenanceLogError.IslandNotFound())
            is QrResult.Success -> Unit
        }

        // 6. If a unit FK is provided, verify it belongs to this island
        if (log.mechanicalUnitId != null) {
            val unitResult = checkUnitExists(log.mechanicalUnitId)
            val unit = when (unitResult) {
                is QrResult.Error -> return QrResult.Error(QrError.MaintenanceLogError.UnitNotInIsland())
                is QrResult.Success -> unitResult.data
            }
            if (unit.islandId != log.islandId)
                return QrResult.Error(QrError.MaintenanceLogError.UnitNotInIsland())
        }

        // 7. Persist
        return logRepository.createLog(log).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.MaintenanceLogError.CreateError(it.message)) }
        )
    }
}
```

---

### 6.3 GetIslandHealthSummaryUseCase

```kotlin
// client/island/maintenance/domain/usecase/GetIslandHealthSummaryUseCase.kt

/**
 * Computes a predictive health summary for an island from its maintenance log history.
 * All calculations are performed with aggregate Room queries — no connectivity required.
 */
class GetIslandHealthSummaryUseCase @Inject constructor(
    private val logRepository: MaintenanceLogRepository
) {
    /** Recurrence window: logs within this period count toward recurrent components. */
    private val recurrenceWindowDays = 90L

    suspend operator fun invoke(
        islandId: String
    ): QrResult<IslandHealthSummary, QrError.MaintenanceLogError> {

        val total = logRepository.countLogsForIsland(islandId)
            .getOrElse { return QrResult.Error(QrError.MaintenanceLogError.LoadError(it.message)) }

        if (total == 0) {
            return QrResult.Success(IslandHealthSummary.empty(islandId))
        }

        val emergencyCount = logRepository.countEmergencyLogsForIsland(islandId).getOrDefault(0)
        val deferredCount  = logRepository.countDeferredLogsForIsland(islandId).getOrDefault(0)
        val avgDuration    = logRepository.avgDurationForIsland(islandId).getOrDefault(null)
        val lastDate       = logRepository.lastPerformedAtForIsland(islandId).getOrDefault(null)
        val allTimestamps  = logRepository.allPerformedAtForIsland(islandId).getOrDefault(emptyList())
        val typeCounts     = logRepository.operationTypeCountsForIsland(islandId).getOrDefault(emptyMap())

        val since = Clock.System.now().minus(recurrenceWindowDays.days)
        val recurrent = logRepository.recurrentComponentsForIsland(islandId, since).getOrDefault(emptyList())

        val lastRevamping = logRepository.lastRevampingAtForIsland(islandId).getOrDefault(null)
        val emergenciesAfterRevamping = lastRevamping?.let {
            logRepository.countEmergenciesAfter(islandId, it).getOrDefault(null)
        }

        // Average interval between consecutive interventions
        val avgDays = if (allTimestamps.size >= 2) {
            val intervals = allTimestamps.zipWithNext { a, b ->
                (b - a).milliseconds.inWholeDays.toDouble()
            }
            intervals.average()
        } else null

        val predicted = if (avgDays != null && lastDate != null) {
            lastDate.plus(avgDays.days)
        } else null

        val mostFrequent = typeCounts.entries.maxByOrNull { it.value }?.key

        return QrResult.Success(
            IslandHealthSummary(
                islandId = islandId,
                totalLogs = total,
                avgDaysBetweenInterventions = avgDays,
                predictedNextInterventionDate = predicted,
                emergencyRate = if (total > 0) emergencyCount.toFloat() / total else 0f,
                deferredRate  = if (total > 0) deferredCount.toFloat()  / total else 0f,
                avgDurationMinutes = avgDuration,
                logsByOperationType = typeCounts,
                mostFrequentOperationType = mostFrequent,
                recurrentComponents = recurrent,
                emergenciesAfterLastRevamping = emergenciesAfterRevamping,
                lastLogDate = lastDate,
                lastOutcome = null   // resolved by caller from getRecentLogs
            )
        )
    }
}
```

---

## 7. UI STRUCTURE

### 7.1 Screens

```
IslandDetailScreen → MaintenanceTab
  ├── MaintenanceLogListContent     ← embedded in tab, no separate screen
  │     list of MaintenanceLogCard (FULL / COMPACT)
  │     FAB → MaintenanceLogFormScreen
  └── IslandHealthScreen            ← separate screen, navigated from tab header button
        IslandHealthViewModel

MaintenanceLogFormScreen            ← create only (no edit — logs are immutable)
  MaintenanceLogFormViewModel
```

### 7.2 MaintenanceTab integration

The existing `MaintenanceTab` in `IslandDetailScreen` (currently showing scheduled maintenance
fields from `Island`) is extended to:

```
MaintenanceTab
├── ScheduledSection                // Island.nextScheduledMaintenance (existing)
├── [Button] "Analisi salute isola" → IslandHealthScreen
└── LogSection
      ├── MaintenanceLogCard × N    (COMPACT by default, tap → FULL expansion)
      └── FAB "Registra intervento" → MaintenanceLogFormScreen
```

### 7.3 MaintenanceLogFormScreen — field order

```
1. Data / ora intervento    (DateTimePicker, default = now)
2. Tipo operazione          (dropdown: MaintenanceOperationType)
   └── [if OTHER] Descrizione tipo   (TextField)
3. Componente               (optional)
   ├── Seleziona unità      (dropdown from island's MechanicalUnits)
   └── oppure: nome libero  (TextField, enabled when no unit selected)
4. Descrizione              (multiline TextField, required)
5. Esito                    (segmented button: MaintenanceOutcome)
6. Durata (minuti)          (optional numeric field)
7. Ore macchina / cicli     (optional, pre-filled from Island if available)
8. Note                     (optional multiline)
9. Tecnico                  (pre-filled from TechnicianSettingsDataStore, editable)
```

### 7.4 MaintenanceLogCard variants

| Variant | Contents |
|---|---|
| COMPACT | Date, operation type label, outcome badge, component (if any) |
| FULL | All COMPACT fields + description, technician, duration, operating hours, notes |

### 7.5 IslandHealthScreen — sections

```
IslandHealthScreen
├── Summary header          (total logs, last intervention date)
├── Prediction card         (avgDays between interventions, predictedNextDate)
├── Health indicators       (emergencyRate bar, deferredRate bar)
├── Top operations chart    (horizontal bar per MaintenanceOperationType)
├── Recurrent components    (list — signals recurring problems)
└── Post-revamping card     (emergenciesAfterLastRevamping — engineering quality)
```

---

## 8. MAPPER

```kotlin
// client/island/maintenance/data/mapper/MaintenanceLogMapper.kt

// entity → domain
fun MaintenanceLogEntity.toDomain(): MaintenanceLog = MaintenanceLog(
    id = id,
    islandId = islandId,
    operationType = MaintenanceOperationType.valueOf(operationType),
    customOperationLabel = customOperationLabel,
    mechanicalUnitId = mechanicalUnitId,
    componentLabel = componentLabel,
    description = description,
    technicianName = technicianName,
    technicianCompany = technicianCompany,
    operatingHoursAtEvent = operatingHoursAtEvent,
    cycleCountAtEvent = cycleCountAtEvent,
    outcome = MaintenanceOutcome.valueOf(outcome),
    durationMinutes = durationMinutes,
    notes = notes,
    performedAt = Instant.fromEpochMilliseconds(performedAt),
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    isActive = isActive,
    isDeleted = isDeleted
)

// domain → entity
fun MaintenanceLog.toEntity(): MaintenanceLogEntity = MaintenanceLogEntity(
    id = id,
    islandId = islandId,
    operationType = operationType.name,
    customOperationLabel = customOperationLabel,
    mechanicalUnitId = mechanicalUnitId,
    componentLabel = componentLabel,
    description = description,
    technicianName = technicianName,
    technicianCompany = technicianCompany,
    operatingHoursAtEvent = operatingHoursAtEvent,
    cycleCountAtEvent = cycleCountAtEvent,
    outcome = outcome.name,
    durationMinutes = durationMinutes,
    notes = notes,
    performedAt = performedAt.toEpochMilliseconds(),
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
    isActive = isActive,
    isDeleted = isDeleted
)
```

---

## 9. IMPLEMENTATION MILESTONES

| # | Milestone | Files |
|---|---|---|
| **M1** | Domain model + entity + DAO + mapper + repository | `MaintenanceLog`, `MaintenanceOperationType`, `MaintenanceOutcome`, `IslandHealthSummary`, `MaintenanceLogEntity`, `MaintenanceLogDao`, `MaintenanceLogMapper`, `MaintenanceLogRepository`, `MaintenanceLogRepositoryImpl` |
| **M2** | Error model + use cases (create + get) | `QrError.MaintenanceLogError`, `CreateMaintenanceLogUseCase`, `GetLogsForIslandUseCase`, `GetRecentLogsForIslandUseCase` |
| **M3** | Form screen + ViewModel | `MaintenanceLogFormScreen`, `MaintenanceLogFormViewModel` (pre-fills technician from `TechnicianSettingsDataStore`) |
| **M4** | List integration in IslandDetailScreen | `MaintenanceLogCard`, `MaintenanceLogListViewModel`, `MaintenanceTab` extension |
| **M5** | Predictive analysis | `GetIslandHealthSummaryUseCase`, `IslandHealthScreen`, `IslandHealthViewModel` |

---

## 10. KEY ARCHITECTURAL RULES (this feature)

- **Lifecycle fields are project-wide standard:** `isActive` / `isDeleted` / `updatedAt` are present on every entity. All normal DAO queries filter `WHERE is_deleted = 0`. `updatedAt` is set to `Clock.System.now()` on every write.
- **Logs are effectively immutable for content** — description, type, outcome are not edited after creation. `isActive` / `isDeleted` are the only writable fields post-insert, via dedicated DAO update queries when needed.
- **No FK constraint on `mechanical_unit_id`** at DB level — prevents orphan errors when units are deactivated. Integrity enforced in `CreateMaintenanceLogUseCase`.
- **Technician is a snapshot:** `technicianName` / `technicianCompany` are copied from `TechnicianSettingsDataStore` at creation time — not a FK. If settings change, past logs are unaffected.
- **`IslandHealthSummary` is never persisted** — always recomputed on demand.
- **`OTHER` requires `customOperationLabel`** — enforced in use case, not at DB level.
- **No UiText in domain or data layer** — `labelResId` on enums is the only resource reference allowed.
- **Existing `Island` fields are not replaced** — `lastMaintenanceDate` / `nextScheduledMaintenance` remain for scheduling; `MaintenanceLog` is the historical record.