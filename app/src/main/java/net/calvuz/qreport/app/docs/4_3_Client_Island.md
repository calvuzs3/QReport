# QReport — 4_3 Island Feature Reference

**Version:** 2.0
**Date:** June 2026
**Scope:** `client/island` — domain models, Room schema, repository, use cases, UI structure

---

## 1. OVERVIEW

The Island feature is the second child level of the data hierarchy:

```
Client → Facilities → Islands → Mechanical Units
```

An `Island` represents a robotic island (POLY family) installed in a `Facility`.
All MechanicalUnit entities depend on it via foreign keys.

---

## 2. DOMAIN MODELS

### 2.1 Island

```kotlin
// domain/model/Island.kt

/**
 * Robotic island belonging to a [Facility].
 *
 * Pure domain: no UI strings, no color codes, no Compose dependencies.
 * Maintenance status text is a presentation concern — the UI resolves
 * [islandOperationalStatus] and [daysToNextMaintenance] into localized strings.
 */
@Serializable
data class Island(
    val id: String,
    val facilityId: String,

    // ===== COMMISSIONING =====
    val commissioningNumber: String? = null,
    val customName: String? = null,

    // ===== ISLAND TYPE =====
    val islandType: IslandType,

    // ===== TECHNICAL DETAILS =====
    val serialNumber: String,
    val installationDate: Instant? = null,
    val warrantyExpiration: Instant? = null,

    // ===== MAINTENANCE =====
    val operatingHours: Int = 0,
    val cycleCount: Long = 0L,
    val lastMaintenanceDate: Instant? = null,
    val nextScheduledMaintenance: Instant? = null,

    // ===== CONFIGURATION =====
    val location: String? = null,
    val notes: String? = null,

    // ===== META =====
    val isActive: Boolean = true,       // false = deactivated (first delete stage)
    val createdAt: Instant,
    val updatedAt: Instant
) {
    /**
     * Returns true if scheduled maintenance is overdue or due today.
     */
    fun needsMaintenance(): Boolean =
        nextScheduledMaintenance?.let { it <= Clock.System.now() } ?: false

    /**
     * Returns true if the island is currently under warranty.
     */
    fun isUnderWarranty(): Boolean =
        warrantyExpiration?.let { it > Clock.System.now() } ?: false

    /**
     * Computed operational status — pure domain logic, no strings.
     * The UI resolves [IslandOperationalStatus.labelResId] for display.
     */
    val islandOperationalStatus: IslandOperationalStatus
        get() = when {
            !isActive                -> IslandOperationalStatus.INACTIVE
            needsMaintenance()       -> IslandOperationalStatus.MAINTENANCE_DUE
            else                     -> IslandOperationalStatus.OPERATIONAL
        }

    /**
     * Days until the next scheduled maintenance.
     * Positive = days remaining, negative = days overdue, null = not scheduled.
     */
    fun daysToNextMaintenance(): Long? =
        nextScheduledMaintenance?.let { (it - Clock.System.now()).inWholeDays }
}
```

> **Note:** No `displayName` property — the domain layer is display-agnostic.
> Build display strings in the UI layer: e.g. `customName ?: serialNumber`.

---

### 2.2 IslandType

```kotlin
// domain/model/IslandType.kt
@Serializable
enum class IslandType(
    val code: String,
    val labelResId: Int,
    val descriptionResId: Int
) {
    POLY_MOVE(    "MOVE",    R.string.island_type_poly_move,    R.string.island_type_poly_move_desc),
    POLY_CAST(    "CAST",    R.string.island_type_poly_cast,    R.string.island_type_poly_cast_desc),
    POLY_EBT(     "EBT",     R.string.island_type_poly_ebt,     R.string.island_type_poly_ebt_desc),
    POLY_TAG_BLE( "TAG_BLE", R.string.island_type_poly_tag_ble, R.string.island_type_poly_tag_ble_desc),
    POLY_TAG_FC(  "TAG_FC",  R.string.island_type_poly_tag_fc,  R.string.island_type_poly_tag_fc_desc),
    POLY_TAG_V(   "TAG_V",   R.string.island_type_poly_tag_v,   R.string.island_type_poly_tag_v_desc),
    POLY_SAMPLE(  "SAMPLE",  R.string.island_type_poly_sample,  R.string.island_type_poly_sample_desc);

    companion object
}
```

---

### 2.3 IslandOperationalStatus

```kotlin
// domain/model/IslandOperationalStatus.kt
@Serializable
enum class IslandOperationalStatus(val labelResId: Int) {
    OPERATIONAL(R.string.island_status_operational),
    MAINTENANCE_DUE(R.string.island_status_maintenance_due),
    INACTIVE(R.string.island_status_inactive)
}
```

---

### 2.4 Maintenance helpers

```kotlin
// domain/model/MaintenanceInterval.kt

/**
 * Default maintenance interval in days for each island type.
 * Used by [CreateIslandUseCase] to auto-schedule [Island.nextScheduledMaintenance].
 */
fun maintenanceIntervalFor(type: IslandType): Int = when (type) {
    IslandType.POLY_MOVE    -> 180
    IslandType.POLY_CAST    -> 180
    IslandType.POLY_EBT     -> 180
    IslandType.POLY_TAG_BLE -> 90
    IslandType.POLY_TAG_FC  -> 180
    IslandType.POLY_TAG_V   -> 180
    IslandType.POLY_SAMPLE  -> 180
}
```

---

### 2.5 Aggregates (read-only, not persisted)

```kotlin
// domain/model/IslandWithFacilityAndClient.kt

/** Flat projection used by the island list screen for full context display. */
data class IslandWithContext(
    val island: Island,
    val facilityName: String,
    val companyName: String
)

/** Per-type count used by statistics screens. */
data class IslandTypeStatistics(
    val islandType: IslandType,
    val count: Int
)
```

---

## 3. DATABASE SCHEMA (ROOM)

### 3.1 IslandEntity

```kotlin
// data/local/entity/IslandEntity.kt

/**
 * Room entity for the facility_islands table.
 *
 * Sync fields:
 *  - [updatedAt]  updated on every local write (create / edit / deactivate / mark-deleted)
 *  - [syncedAt]   set to [updatedAt] after a successful push to the server;
 *                 null means the record has never been synced
 *  - [isDeleted]  second delete stage; row excluded from all normal queries and
 *                 pushed to server so other devices can mirror the deletion
 *
 * Delete lifecycle:
 *  isActive=true,  isDeleted=false  →  normal
 *  isActive=false, isDeleted=false  →  deactivated  (first stage, cascade to MechanicalUnits)
 *  isActive=false, isDeleted=true   →  marked deleted (second stage, cascade to MechanicalUnits)
 *
 * Note: FacilityDao also contains bulk UPDATE queries targeting this table
 * to cascade deactivation/deletion from a parent Facility in a single @Transaction.
 */
@Entity(
    tableName = "facility_islands",
    foreignKeys = [
        ForeignKey(
            entity = FacilityEntity::class,
            parentColumns = ["id"],
            childColumns = ["facility_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["facility_id"]),
        Index(value = ["serial_number"], unique = true),
        Index(value = ["island_type"]),
        Index(value = ["is_active"]),
        Index(value = ["next_scheduled_maintenance"]),
        Index(value = ["is_deleted"]),
        Index(value = ["updated_at"]),
    ]
)
data class IslandEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "facility_id")
    val facilityId: String,

    // ===== COMMISSIONING =====
    @ColumnInfo(name = "commissioning_number")
    val commissioningNumber: String? = null,

    // ===== ISLAND TYPE =====
    @ColumnInfo(name = "island_type_id")
    val islandTypeId: String,             // IslandTypeMaster.id

    // ===== TECHNICAL DETAILS =====
    @ColumnInfo(name = "serial_number")
    val serialNumber: String,

    @ColumnInfo(name = "installation_date")
    val installationDate: Long? = null, // Epoch milliseconds

    @ColumnInfo(name = "warranty_expiration")
    val warrantyExpiration: Long? = null, // Epoch milliseconds

    // ===== MAINTENANCE =====
    @ColumnInfo(name = "operating_hours")
    val operatingHours: Int = 0,

    @ColumnInfo(name = "cycle_count")
    val cycleCount: Long = 0L,

    @ColumnInfo(name = "last_maintenance_date")
    val lastMaintenanceDate: Long? = null, // Epoch milliseconds

    @ColumnInfo(name = "next_scheduled_maintenance")
    val nextScheduledMaintenance: Long? = null, // Epoch milliseconds

    // ===== CONFIGURATION =====
    @ColumnInfo(name = "custom_name")
    val customName: String? = null,

    @ColumnInfo(name = "location")
    val location: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    // ===== META =====
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,                // Epoch milliseconds

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,                // Epoch milliseconds — updated on every local write

    // ===== SYNC =====
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null,         // null = never synced; set after successful server push

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false      // Second delete stage; see class-level KDoc
)
```

---

### 3.2 IslandDao

```kotlin
// data/local/dao/IslandDao.kt
@Dao
interface IslandDao {

    // ===== REACTIVE QUERIES =====

    @Query("SELECT * FROM facility_islands WHERE is_deleted = 0 ORDER BY serial_number ASC")
    fun getAllIslandsFlow(): Flow<List<IslandEntity>>

    @Query("SELECT * FROM facility_islands WHERE is_active = 1 AND is_deleted = 0 ORDER BY serial_number ASC")
    fun getAllActiveIslandsFlow(): Flow<List<IslandEntity>>

    @Query("""
        SELECT * FROM facility_islands
        WHERE facility_id = :facilityId AND is_active = 1 AND is_deleted = 0
        ORDER BY custom_name ASC, serial_number ASC
    """)
    fun getActiveIslandsForFacilityFlow(facilityId: String): Flow<List<IslandEntity>>

    @Query("SELECT * FROM facility_islands WHERE id = :id AND is_deleted = 0")
    fun getIslandByIdFlow(id: String): Flow<IslandEntity?>

    // ===== SUSPEND QUERIES =====

    @Query("SELECT * FROM facility_islands WHERE is_active = 1 AND is_deleted = 0 ORDER BY serial_number ASC")
    suspend fun getAllActiveIslands(): List<IslandEntity>

    @Query("""
        SELECT * FROM facility_islands
        WHERE facility_id = :facilityId AND is_active = 1 AND is_deleted = 0
        ORDER BY custom_name ASC, serial_number ASC
    """)
    suspend fun getIslandsForFacility(facilityId: String): List<IslandEntity>

    @Query("SELECT * FROM facility_islands WHERE id = :id AND is_deleted = 0")
    suspend fun getIslandById(id: String): IslandEntity?

    @Query("SELECT * FROM facility_islands WHERE id IN (:ids) AND is_active = 1 AND is_deleted = 0")
    suspend fun getIslandsByIds(ids: List<String>): List<IslandEntity>

    @Query("SELECT * FROM facility_islands WHERE serial_number = :serialNumber AND is_deleted = 0")
    suspend fun getIslandBySerialNumber(serialNumber: String): IslandEntity?

    // ===== CRUD =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIsland(island: IslandEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIslands(islands: List<IslandEntity>)

    @Update
    suspend fun updateIsland(island: IslandEntity)

    // ===== DELETE — TWO-STAGE =====

    /**
     * Stage 1 (standalone): deactivate a single island and cascade to its MechanicalUnits.
     * Called by [IslandRepositoryImpl.deactivateIsland].
     * Note: FacilityDao contains the bulk version for facility-level cascade.
     */
    @Query("UPDATE facility_islands SET is_active = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun deactivateIsland(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE mechanical_units SET is_active = 0, updated_at = :timestamp WHERE island_id = :islandId")
    suspend fun deactivateMechanicalUnitsByIsland(islandId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Stage 2 (standalone): mark a single island and its MechanicalUnits as deleted.
     * Called by [IslandRepositoryImpl.markIslandDeleted].
     */
    @Query("UPDATE facility_islands SET is_deleted = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun markIslandDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE mechanical_units SET is_deleted = 1, updated_at = :timestamp WHERE island_id = :islandId")
    suspend fun markMechanicalUnitsDeletedByIsland(islandId: String, timestamp: Long = System.currentTimeMillis())

    // ===== SEARCH =====

    @Query("""
        SELECT fi.* FROM facility_islands fi
        INNER JOIN facilities f ON fi.facility_id = f.id
        INNER JOIN clients c ON f.client_id = c.id
        WHERE fi.is_active = 1 AND fi.is_deleted = 0
          AND f.is_active = 1 AND c.is_active = 1
          AND (fi.serial_number LIKE '%' || :query || '%'
               OR fi.custom_name  LIKE '%' || :query || '%'
               OR fi.location     LIKE '%' || :query || '%'
               OR f.name          LIKE '%' || :query || '%'
               OR c.company_name  LIKE '%' || :query || '%')
        ORDER BY fi.serial_number ASC
    """)
    suspend fun searchIslands(query: String): List<IslandEntity>

    @Query("SELECT * FROM facility_islands WHERE island_type = :islandType AND is_active = 1 AND is_deleted = 0 ORDER BY serial_number ASC")
    suspend fun getIslandsByType(islandType: String): List<IslandEntity>

    // ===== CLIENT-BASED QUERIES =====

    @Query("""
        SELECT fi.* FROM facility_islands fi
        INNER JOIN facilities f ON fi.facility_id = f.id
        WHERE f.client_id = :clientId AND fi.is_active = 1 AND fi.is_deleted = 0 AND f.is_active = 1
        ORDER BY f.name ASC, fi.serial_number ASC
    """)
    suspend fun getIslandsForClient(clientId: String): List<IslandEntity>

    // ===== MAINTENANCE QUERIES =====

    @Query("""
        SELECT * FROM facility_islands
        WHERE is_active = 1 AND is_deleted = 0
          AND next_scheduled_maintenance IS NOT NULL
          AND next_scheduled_maintenance <= :currentTimestamp
        ORDER BY next_scheduled_maintenance ASC
    """)
    suspend fun getIslandsDueMaintenance(currentTimestamp: Long): List<IslandEntity>

    @Query("""
        SELECT * FROM facility_islands
        WHERE is_active = 1 AND is_deleted = 0
          AND next_scheduled_maintenance IS NOT NULL
          AND next_scheduled_maintenance BETWEEN :startTimestamp AND :endTimestamp
        ORDER BY next_scheduled_maintenance ASC
    """)
    suspend fun getIslandsMaintenanceInPeriod(startTimestamp: Long, endTimestamp: Long): List<IslandEntity>

    @Query("""
        SELECT * FROM facility_islands
        WHERE facility_id = :facilityId AND is_active = 1 AND is_deleted = 0
          AND next_scheduled_maintenance IS NOT NULL
          AND next_scheduled_maintenance <= :currentTimestamp
        ORDER BY next_scheduled_maintenance ASC
    """)
    suspend fun getIslandsDueMaintenanceForFacility(facilityId: String, currentTimestamp: Long): List<IslandEntity>

    @Query("UPDATE facility_islands SET last_maintenance_date = :maintenanceDate, updated_at = :timestamp WHERE id = :islandId")
    suspend fun updateLastMaintenanceDate(islandId: String, maintenanceDate: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE facility_islands SET next_scheduled_maintenance = :nextMaintenance, updated_at = :timestamp WHERE id = :islandId")
    suspend fun updateNextScheduledMaintenance(islandId: String, nextMaintenance: Long?, timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun performMaintenanceUpdate(islandId: String, currentTimestamp: Long, nextMaintenanceTimestamp: Long?) {
        updateLastMaintenanceDate(islandId, currentTimestamp)
        updateNextScheduledMaintenance(islandId, nextMaintenanceTimestamp)
        touchIsland(islandId, currentTimestamp)
    }

    // ===== WARRANTY QUERIES =====

    @Query("""
        SELECT * FROM facility_islands
        WHERE is_active = 1 AND is_deleted = 0
          AND warranty_expiration IS NOT NULL
          AND warranty_expiration > :currentTimestamp
        ORDER BY warranty_expiration ASC
    """)
    suspend fun getIslandsUnderWarranty(currentTimestamp: Long): List<IslandEntity>

    @Query("""
        SELECT * FROM facility_islands
        WHERE is_active = 1 AND is_deleted = 0
          AND warranty_expiration IS NOT NULL
          AND warranty_expiration BETWEEN :currentTimestamp AND :warningTimestamp
        ORDER BY warranty_expiration ASC
    """)
    suspend fun getIslandsWarrantyExpiringSoon(currentTimestamp: Long, warningTimestamp: Long): List<IslandEntity>

    // ===== STATISTICS =====

    @Query("SELECT COUNT(*) FROM facility_islands WHERE facility_id = :facilityId AND is_active = 1 AND is_deleted = 0")
    suspend fun getIslandsCountForFacility(facilityId: String): Int

    @Query("SELECT COUNT(*) FROM facility_islands WHERE is_active = 1 AND is_deleted = 0")
    suspend fun getActiveIslandsCount(): Int

    @Query("SELECT COUNT(*) FROM facility_islands WHERE island_type = :islandType AND is_active = 1 AND is_deleted = 0")
    suspend fun getIslandsCountByType(islandType: String): Int

    @Query("""
        SELECT COUNT(*) FROM facility_islands fi
        INNER JOIN facilities f ON fi.facility_id = f.id
        WHERE f.client_id = :clientId AND fi.is_active = 1 AND fi.is_deleted = 0 AND f.is_active = 1
    """)
    suspend fun getIslandsCountForClient(clientId: String): Int

    @Query("""
        SELECT island_type, COUNT(*) as count
        FROM facility_islands
        WHERE is_active = 1 AND is_deleted = 0
        GROUP BY island_type
        ORDER BY count DESC, island_type ASC
    """)
    suspend fun getIslandTypeStatistics(): List<IslandTypeStatisticsResult>

    // ===== VALIDATION =====

    @Query("SELECT COUNT(*) > 0 FROM facility_islands WHERE serial_number = :serialNumber AND id != :excludeId AND is_deleted = 0")
    suspend fun isSerialNumberTaken(serialNumber: String, excludeId: String = ""): Boolean

    @Query("""
        SELECT COUNT(*) > 0 FROM facility_islands
        WHERE facility_id = :facilityId AND custom_name = :customName
          AND id != :excludeId AND is_active = 1 AND is_deleted = 0
    """)
    suspend fun isCustomNameTakenForFacility(facilityId: String, customName: String, excludeId: String = ""): Boolean

    // ===== COMPLEX PROJECTION =====

    /**
     * Full context projection for the island list screen.
     * Maps to [IslandWithFacilityAndClientResult].
     */
    @Query("""
        SELECT fi.id,
               fi.facility_id      AS facilityId,
               fi.island_type      AS islandType,
               fi.serial_number    AS serialNumber,
               fi.installation_date    AS installationDate,
               fi.warranty_expiration  AS warrantyExpiration,
               fi.is_active            AS isActive,
               fi.operating_hours      AS operatingHours,
               fi.cycle_count          AS cycleCount,
               fi.last_maintenance_date       AS lastMaintenanceDate,
               fi.next_scheduled_maintenance  AS nextScheduledMaintenance,
               fi.custom_name   AS customName,
               fi.location,
               fi.notes,
               fi.created_at    AS createdAt,
               fi.updated_at    AS updatedAt,
               f.name           AS facilityName,
               c.company_name   AS companyName
        FROM facility_islands fi
        INNER JOIN facilities f ON fi.facility_id = f.id
        INNER JOIN clients c ON f.client_id = c.id
        WHERE fi.is_active = 1 AND fi.is_deleted = 0
          AND f.is_active = 1 AND c.is_active = 1
        ORDER BY c.company_name ASC, f.name ASC, fi.serial_number ASC
    """)
    suspend fun getIslandsWithContext(): List<IslandWithFacilityAndClientResult>

    // ===== MAINTENANCE UTILITIES =====

    @Query("UPDATE facility_islands SET updated_at = :timestamp WHERE id = :id")
    suspend fun touchIsland(id: String, timestamp: Long = System.currentTimeMillis())

    // ===== BACKUP =====

    @Query("SELECT * FROM facility_islands ORDER BY created_at ASC")
    suspend fun getAllForBackup(): List<IslandEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(islands: List<IslandEntity>)

    @Query("DELETE FROM facility_islands")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM facility_islands")
    suspend fun count(): Int
}

/** Projection result for [IslandDao.getIslandsWithContext]. */
data class IslandWithFacilityAndClientResult(
    val id: String,
    val facilityId: String,
    val islandType: String,
    val serialNumber: String,
    val installationDate: Long?,
    val warrantyExpiration: Long?,
    val isActive: Boolean,
    val operatingHours: Int,
    val cycleCount: Long,
    val lastMaintenanceDate: Long?,
    val nextScheduledMaintenance: Long?,
    val customName: String?,
    val location: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val facilityName: String,
    val companyName: String
)

/** Projection result for [IslandDao.getIslandTypeStatistics]. */
data class IslandTypeStatisticsResult(
    val island_type: String,    // snake_case to match SQL column name
    val count: Int
)
```

---

## 4. REPOSITORY

### 4.1 Layer contract

The repository layer uses `kotlin.Result<T>` (not `QrResult`).
Error translation to `QrResult<D, QrError>` happens in use cases.

### 4.2 IslandRepository interface

```kotlin
// domain/repository/IslandRepository.kt
interface IslandRepository {

    // ===== REACTIVE =====
    fun getAllActiveIslandsFlow(): Flow<List<Island>>
    fun getActiveIslandsByFacilityFlow(facilityId: String): Flow<List<Island>>
    fun getIslandByIdFlow(id: String): Flow<Island?>

    // ===== CRUD =====
    suspend fun getAllActiveIslands(): Result<List<Island>>
    suspend fun getIslandById(id: String): Result<Island?>
    suspend fun getIslandsByIds(ids: List<String>): Result<List<Island>>
    suspend fun getIslandsByFacility(facilityId: String): Result<List<Island>>
    suspend fun getActiveIslandsByFacility(facilityId: String): Result<List<Island>>
    suspend fun createIsland(island: Island): Result<Unit>
    suspend fun updateIsland(island: Island): Result<Unit>

    // ===== DELETE — TWO-STAGE =====
    /**
     * Stage 1: sets isActive=false on the island and all its MechanicalUnits
     * inside a single transaction.
     */
    suspend fun deactivateIsland(id: String): Result<Unit>

    /**
     * Stage 2: sets isDeleted=true on the island and all its MechanicalUnits
     * inside a single transaction. Only valid if isActive is already false.
     */
    suspend fun markIslandDeleted(id: String): Result<Unit>

    // ===== SEARCH & FILTER =====
    suspend fun searchIslands(query: String): Result<List<Island>>
    suspend fun getIslandsByType(islandType: IslandType): Result<List<Island>>
    suspend fun getIslandBySerialNumber(serialNumber: String): Result<Island?>

    // ===== CLIENT AGGREGATION =====
    suspend fun getIslandsByClient(clientId: String): Result<List<Island>>
    suspend fun getIslandsCountByClient(clientId: String): Result<Int>

    // ===== MAINTENANCE =====
    suspend fun getIslandsRequiringMaintenance(currentTime: Instant? = null): Result<List<Island>>
    suspend fun getIslandsUnderWarranty(currentTime: Instant? = null): Result<List<Island>>
    suspend fun updateMaintenanceDate(islandId: String, maintenanceDate: Instant): Result<Unit>
    suspend fun updateOperatingHours(islandId: String, operatingHours: Int): Result<Unit>
    suspend fun updateCycleCount(islandId: String, cycleCount: Long): Result<Unit>

    // ===== VALIDATION =====
    suspend fun isSerialNumberTaken(serialNumber: String, excludeId: String = ""): Result<Boolean>

    // ===== STATISTICS =====
    suspend fun getActiveIslandsCount(): Result<Int>
    suspend fun getIslandsCountByFacility(facilityId: String): Result<Int>
    suspend fun getIslandsCountByType(islandType: IslandType): Result<Int>
    suspend fun getIslandTypeStats(): Result<Map<IslandType, Int>>

    // ===== BULK =====
    suspend fun createIslands(islands: List<Island>): Result<Unit>
}
```

### 4.3 Two-stage delete — Repository implementation

```kotlin
// data/repository/IslandRepositoryImpl.kt  (relevant methods only)

@Transaction
override suspend fun deactivateIsland(id: String): Result<Unit> = runCatching {
    val ts = System.currentTimeMillis()
    islandDao.deactivateMechanicalUnitsByIsland(id, ts)
    islandDao.deactivateIsland(id, ts)
}

@Transaction
override suspend fun markIslandDeleted(id: String): Result<Unit> = runCatching {
    val ts = System.currentTimeMillis()
    islandDao.markMechanicalUnitsDeletedByIsland(id, ts)
    islandDao.markIslandDeleted(id, ts)
}
```

> Children are updated before the parent so that a partial failure leaves
> the parent still visible rather than orphaning children.
>
> Note: these methods handle standalone island deletion. When a Facility is
> deleted, the cascade runs entirely inside `FacilityDao` via bulk UPDATE
> queries — `IslandRepositoryImpl` is not involved in that path.

---

## 5. ERROR HANDLING

```kotlin
// presentation/core/model/QrError.kt  (add to the sealed interface)
sealed interface IslandError : QrError {

    // ── CRUD ─────────────────────────────────────────────────────────────────

    data class LoadError(val message: String? = null) : IslandError
    data class NotFound(val message: String? = null) : IslandError
    data class CreateError(val message: String? = null) : IslandError
    data class UpdateError(val message: String? = null) : IslandError
    data class DeleteError(val message: String? = null) : IslandError

    // ── Validation ───────────────────────────────────────────────────────────

    /** Serial number is missing or blank. */
    data class MissingSerialNumber(val message: String? = null) : IslandError

    /** Serial number already exists on another island. */
    data class SerialNumberTaken(val message: String? = null) : IslandError

    /** facilityId is blank or does not match an existing facility. */
    data class FacilityNotFound(val message: String? = null) : IslandError

    /** Attempted to change the facilityId of an existing island. */
    data class CannotChangeFacility(val message: String? = null) : IslandError

    /** Attempted second-stage delete on an island that is still active. */
    data class CannotMarkDeletedWhileActive(val message: String? = null) : IslandError

    // ── Business rules ───────────────────────────────────────────────────────

    /** Cannot deactivate: scheduled maintenance is overdue. */
    data class CannotDeleteMaintenanceOverdue(val message: String? = null) : IslandError

    // ── Date validation ───────────────────────────────────────────────────────

    sealed interface ValidationError : IslandError {
        /** installationDate is in the future. */
        data class InvalidInstallationDate(val message: String? = null) : ValidationError

        /** warrantyExpiration is before installationDate. */
        data class InvalidWarrantyDate(val message: String? = null) : ValidationError

        /** lastMaintenanceDate is before installationDate, in the future,
         *  or nextScheduledMaintenance is not after lastMaintenanceDate. */
        data class InvalidMaintenanceDate(val message: String? = null) : ValidationError
    }
}
```

Use cases return `QrResult<D, QrError.IslandError>`.

---

## 6. USE CASES

### 6.1 Full list

```
CheckIslandExistsUseCase
CheckSerialNumberUniquenessUseCase
CreateIslandUseCase
UpdateIslandUseCase
DeleteIslandUseCase                 ← two-stage, see §6.4
GetIslandByIdUseCase
GetIslandsByFacilityUseCase
GetIslandStatisticsUseCase
GetIslandWithUnitsUseCase
ObserveIslandsUseCase
SearchIslandsUseCase
UpdateMaintenanceUseCase
```

### 6.2 CreateIslandUseCase

```kotlin
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

        // 5. Auto-compute next maintenance if not provided
        val finalIsland = autoScheduleNextMaintenance(island)

        // 6. Persist
        return islandRepository.createIsland(finalIsland).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.IslandError.CreateError(it.message)) }
        )
    }

    private fun validateMaintenanceDates(island: Island): QrResult<Unit, QrError.IslandError>? {
        val now = Clock.System.now()
        return when {
            island.installationDate?.let { it > now } == true ->
                QrResult.Error(QrError.IslandError.ValidationError.InvalidInstallationDate())
            island.warrantyExpiration?.let { exp ->
                island.installationDate?.let { install -> exp < install }
            } == true ->
                QrResult.Error(QrError.IslandError.ValidationError.InvalidWarrantyDate())
            island.lastMaintenanceDate?.let { last ->
                island.installationDate?.let { install -> last < install }
            } == true ->
                QrResult.Error(QrError.IslandError.ValidationError.InvalidMaintenanceDate())
            island.lastMaintenanceDate?.let { it > now } == true ->
                QrResult.Error(QrError.IslandError.ValidationError.InvalidMaintenanceDate())
            island.nextScheduledMaintenance?.let { next ->
                island.lastMaintenanceDate?.let { last -> next <= last }
            } == true ->
                QrResult.Error(QrError.IslandError.ValidationError.InvalidMaintenanceDate())
            else -> null
        }
    }

    private fun autoScheduleNextMaintenance(island: Island): Island {
        if (island.nextScheduledMaintenance != null) return island
        val base = island.lastMaintenanceDate ?: island.installationDate ?: Clock.System.now()
        return island.copy(
            nextScheduledMaintenance = base + maintenanceIntervalFor(island.islandType).days
        )
    }
}
```

### 6.3 UpdateIslandUseCase

```kotlin
class UpdateIslandUseCase @Inject constructor(
    private val islandRepository: IslandRepository,
    private val validateIslandData: IslandDataValidator,
    private val checkIslandExists: CheckIslandExistsUseCase,
    private val checkSerialNumberUniqueness: CheckSerialNumberUniquenessUseCase
) {
    suspend operator fun invoke(island: Island): QrResult<Unit, QrError.IslandError> {

        // 1. Verify exists
        val original = when (val r = checkIslandExists(island.id)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        // 2. facilityId must not change
        if (island.facilityId != original.facilityId)
            return QrResult.Error(QrError.IslandError.CannotChangeFacility())

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
        val dateError = validateMaintenanceDates(island)
        if (dateError != null) return dateError

        // 6. Guard: prevent operating hours decrease without a new maintenance record
        val guardedIsland = guardOperatingHours(original, island)

        // 7. Persist with refreshed timestamp
        val updated = guardedIsland.copy(updatedAt = Clock.System.now())
        return islandRepository.updateIsland(updated).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.IslandError.UpdateError(it.message)) }
        )
    }

    private fun validateMaintenanceDates(island: Island): QrResult<Unit, QrError.IslandError>? {
        val now = Clock.System.now()
        return when {
            island.installationDate?.let { it > now } == true ->
                QrResult.Error(QrError.IslandError.ValidationError.InvalidInstallationDate())
            island.warrantyExpiration?.let { exp ->
                island.installationDate?.let { install -> exp < install }
            } == true ->
                QrResult.Error(QrError.IslandError.ValidationError.InvalidWarrantyDate())
            island.lastMaintenanceDate?.let { last ->
                island.installationDate?.let { install -> last < install }
            } == true ->
                QrResult.Error(QrError.IslandError.ValidationError.InvalidMaintenanceDate())
            island.lastMaintenanceDate?.let { it > now } == true ->
                QrResult.Error(QrError.IslandError.ValidationError.InvalidMaintenanceDate())
            island.nextScheduledMaintenance?.let { next ->
                island.lastMaintenanceDate?.let { last -> next <= last }
            } == true ->
                QrResult.Error(QrError.IslandError.ValidationError.InvalidMaintenanceDate())
            else -> null
        }
    }

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
```

### 6.4 DeleteIslandUseCase

```kotlin
/**
 * Two-stage soft-delete for an island.
 *
 * Stage 1 — DEACTIVATE (isActive=true → isActive=false):
 *   Cascades is_active=0 to all child MechanicalUnits.
 *   Blocked if scheduled maintenance is overdue.
 *
 * Stage 2 — MARK DELETED (isActive=false → isDeleted=true):
 *   Cascades is_deleted=1 to all child MechanicalUnits.
 *
 * The use case reads the current state and automatically picks the correct stage.
 *
 * Note: when a parent Facility is deleted, island deactivation/deletion is
 * cascaded via bulk UPDATE queries in FacilityDao — this use case is not invoked.
 */
enum class DeleteIslandResult { DEACTIVATED, MARKED_DELETED }

class DeleteIslandUseCase @Inject constructor(
    private val islandRepository: IslandRepository,
    private val checkIslandExists: CheckIslandExistsUseCase
) {
    suspend operator fun invoke(
        islandId: String
    ): QrResult<DeleteIslandResult, QrError.IslandError> {

        if (islandId.isBlank())
            return QrResult.Error(QrError.IslandError.NotFound())

        // 1. Load current state
        val island = when (val r = checkIslandExists(islandId)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        return when {

            // ── Stage 1: island is still active ──────────────────────────────
            island.isActive -> {

                // Business rule: cannot deactivate if maintenance is overdue
                if (island.needsMaintenance())
                    return QrResult.Error(QrError.IslandError.CannotDeleteMaintenanceOverdue())

                islandRepository.deactivateIsland(islandId).fold(
                    onSuccess = { QrResult.Success(DeleteIslandResult.DEACTIVATED) },
                    onFailure = { QrResult.Error(QrError.IslandError.DeleteError(it.message)) }
                )
            }

            // ── Stage 2: island is already deactivated ────────────────────────
            !island.isActive && !island.isDeleted -> {
                islandRepository.markIslandDeleted(islandId).fold(
                    onSuccess = { QrResult.Success(DeleteIslandResult.MARKED_DELETED) },
                    onFailure = { QrResult.Error(QrError.IslandError.DeleteError(it.message)) }
                )
            }

            // ── Already deleted ───────────────────────────────────────────────
            else -> QrResult.Error(QrError.IslandError.NotFound())
        }
    }
}
```

**ViewModel usage pattern:**

```kotlin
when (val result = deleteIslandUseCase(islandId)) {
    is QrResult.Success -> when (result.data) {
        DeleteIslandResult.DEACTIVATED    -> showSnackbar(R.string.island_deactivated)
        DeleteIslandResult.MARKED_DELETED -> showSnackbar(R.string.island_deleted)
    }
    is QrResult.Error -> showError(result.error.asUiText())
}
```

---

## 7. UI STRUCTURE

### 7.1 Screens

```
IslandListScreen        ← IslandListViewModel
IslandDetailScreen      ← IslandDetailViewModel
  ├── InfoTab           (island data: type, serial, installation, warranty)
  ├── UnitsTab          (mechanical units list)
  └── MaintenanceTab    (maintenance history and schedule)
IslandFormScreen        ← IslandFormViewModel  (create + edit)
```

### 7.2 IslandListScreen — key elements

```
TopAppBar
  ├── Icon + title (from IslandPkg)
  ├── cycleCardVariant button  (FULL / COMPACT / MINIMAL)
  ├── sort button   → QReportSortOrderMenu (IslandSortOrder.entries)
  └── filter button → QReportFilterMenu   (IslandFilter.entries)

QReportSearchBar(query, onQueryChange)
QReportFiltersChipRow    ← shown only when filter/sort differ from defaults

QReportSelectorRow(      ← parent client selector (level 1)
    items = clients,
    selectedId = uiState.selectedClientId,
    onSelect = viewModel::selectClient,
    label = R.string.island_selector_client_label
)
QReportSelectorRow(      ← parent facility selector (level 2)
    items = uiState.facilitiesForSelectedClient,
    selectedId = uiState.selectedFacilityId,
    onSelect = viewModel::selectFacility,
    label = R.string.island_selector_facility_label,
    enabled = uiState.selectedClientId != null
)                        ← both null = show all islands

Content area (PullToRefresh wrapper):
  isLoading   → LoadingState()
  error       → QReportErrorState(error, onRetry, onDismiss)
  empty list  → EmptyState(...) with FAB shortcut
  list        → LazyColumn of IslandCard(variant)

FAB → onCreateNewIsland
```

### 7.3 IslandCard variants

| Variant | Button size | Contents |
|---------|------------|----------|
| FULL    | 48 dp      | Custom name / serial, type chip, facility + client, operational status badge, maintenance date, warranty badge, timestamps |
| COMPACT | 36 dp      | Custom name / serial, type chip, facility name, operational status badge |
| MINIMAL | —          | Custom name / serial only, tap to navigate |

---

## 8. CHECKUP INTEGRATION

> **Status: not yet implemented.**

The planned integration point is `islandId` stored on the `CheckUp` entity, enabling:
- Per-island checkup history in `IslandDetailScreen`
- Maintenance statistics correlated with checkup records

Implementation deferred to the CheckUp feature phase.