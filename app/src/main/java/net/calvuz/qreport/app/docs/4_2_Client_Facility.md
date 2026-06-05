# QReport — 4_2 Facility Feature Reference

**Version:** 2.0
**Date:** June 2026
**Scope:** `client/facility` — domain models, Room schema, repository, use cases, UI structure

---

## 1. OVERVIEW

The Facility feature is the first child of Client in the data hierarchy:

```
Client → Facilities → Islands → Mechanical Units
```

A `Facility` represents a physical plant or site belonging to a `Client`.
All Island and MechanicalUnit entities depend on it via foreign keys.

---

## 2. DOMAIN MODELS

### 2.1 Facility

```kotlin
// domain/model/Facility.kt
@Serializable
data class Facility(
    val id: String,
    val clientId: String,

    // ===== DATA =====
    val name: String,
    val code: String? = null,
    val notes: String? = null,
    val facilityType: FacilityType = FacilityType.PRODUCTION,

    // ===== ADDRESS =====
    val address: Address? = null,

    // ===== META =====
    val isPrimary: Boolean = false,     // Main facility for the client
    val isActive: Boolean = true,       // false = deactivated (first delete stage)
    val createdAt: Instant,
    val updatedAt: Instant
) {
    /** Returns true if the minimum required data is present. */
    fun isComplete(): Boolean = name.isNotBlank()
}
```

> **Note:** No `displayName` property — the domain layer is display-agnostic.
> Build display strings in the UI layer: e.g. `if (code != null) "$name ($code)" else name`.

---

### 2.2 FacilityType

```kotlin
// domain/model/FacilityType.kt
@Serializable
enum class FacilityType(val labelResId: Int, val descriptionResId: Int) {
    PRODUCTION(R.string.facility_type_production,   R.string.facility_type_production_desc),
    WAREHOUSE(R.string.facility_type_warehouse,     R.string.facility_type_warehouse_desc),
    ASSEMBLY(R.string.facility_type_assembly,       R.string.facility_type_assembly_desc),
    TESTING(R.string.facility_type_testing,         R.string.facility_type_testing_desc),
    LOGISTICS(R.string.facility_type_logistics,     R.string.facility_type_logistics_desc),
    OFFICE(R.string.facility_type_office,           R.string.facility_type_office_desc),
    MAINTENANCE(R.string.facility_type_maintenance, R.string.facility_type_maintenance_desc),
    R_AND_D(R.string.facility_type_r_and_d,         R.string.facility_type_r_and_d_desc),
    OTHER(R.string.facility_type_other,             R.string.facility_type_other_desc)
}
```

---

### 2.3 Address / GeoCoordinates

Shared domain models — defined in `4_1_Client_Client.md §2.2` and `§2.3`.
Not redefined here.

---

### 2.4 Aggregates (read-only, not persisted)

```kotlin
// domain/model/FacilityWithIslands.kt
data class FacilityWithIslands(
    val facility: Facility,
    val islands: List<Island>,
    val statistics: IslandStatistics = IslandStatistics()
) {
    val hasIslands: Boolean get() = islands.isNotEmpty()
    val islandCount: Int get() = islands.size
    val activeIslandCount: Int get() = statistics.activeCount

    /** Islands that need maintenance, used by the client detail screen. */
    val islandsNeedingMaintenance: List<Island>
        get() = islands.filter { it.needsMaintenance() }
}

/**
 * Aggregate statistics computed from a [FacilityWithIslands.islands] list.
 */
data class IslandStatistics(
    val totalCount: Int = 0,
    val activeCount: Int = 0,
    val inactiveCount: Int = 0,
    val byType: Map<IslandType, Int> = emptyMap(),
    val totalOperatingHours: Int = 0,
    val totalCycleCount: Long = 0L,
    val averageOperatingHours: Double = 0.0,
    val maintenanceDueCount: Int = 0,
    val underWarrantyCount: Int = 0,
    val oldestInstallation: Instant? = null,
    val newestInstallation: Instant? = null
) {
    val hasActiveIslands: Boolean get() = activeCount > 0
    val maintenanceRate: Float
        get() = if (totalCount > 0) maintenanceDueCount.toFloat() / totalCount else 0f
    val warrantyRate: Float
        get() = if (totalCount > 0) underWarrantyCount.toFloat() / totalCount else 0f
}
```

---

## 3. DATABASE SCHEMA (ROOM)

### 3.1 FacilityEntity

```kotlin
// data/local/entity/FacilityEntity.kt

/**
 * Room entity for the facilities table.
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
 *  isActive=false, isDeleted=false  →  deactivated  (first stage, cascade to children)
 *  isActive=false, isDeleted=true   →  marked deleted (second stage, cascade to children)
 */
@Entity(
    tableName = "facilities",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["client_id"]),
        Index(value = ["name"]),
        Index(value = ["is_primary", "client_id"]),
        Index(value = ["is_active"]),
        Index(value = ["is_deleted"]),
        Index(value = ["updated_at"]),
    ]
)
data class FacilityEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "client_id")
    val clientId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "code")
    val code: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "facility_type")
    val facilityType: String,           // FacilityType.name

    @ColumnInfo(name = "address_json")
    val addressJson: String?,           // JSON-serialized Address

    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean = false,

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

### 3.2 FacilityDao

```kotlin
// data/local/dao/FacilityDao.kt
@Dao
interface FacilityDao {

    // ===== REACTIVE QUERIES =====

    @Query("SELECT * FROM facilities WHERE is_deleted = 0 ORDER BY is_primary DESC, name ASC")
    fun getAllFacilitiesFlow(): Flow<List<FacilityEntity>>

    @Query("SELECT * FROM facilities WHERE is_active = 1 AND is_deleted = 0 ORDER BY is_primary DESC, name ASC")
    fun getActiveFacilitiesFlow(): Flow<List<FacilityEntity>>

    @Query("SELECT * FROM facilities WHERE client_id = :clientId AND is_deleted = 0 ORDER BY is_primary DESC, name ASC")
    fun getFacilitiesForClientFlow(clientId: String): Flow<List<FacilityEntity>>

    @Query("SELECT * FROM facilities WHERE client_id = :clientId AND is_active = 1 AND is_deleted = 0 ORDER BY is_primary DESC, name ASC")
    fun getActiveFacilitiesForClientFlow(clientId: String): Flow<List<FacilityEntity>>

    @Query("SELECT * FROM facilities WHERE id = :id AND is_deleted = 0")
    fun getFacilityByIdFlow(id: String): Flow<FacilityEntity?>

    // ===== SUSPEND QUERIES =====

    @Query("SELECT * FROM facilities WHERE is_active = 1 AND is_deleted = 0 ORDER BY name ASC")
    suspend fun getAllActiveFacilities(): List<FacilityEntity>

    @Query("SELECT * FROM facilities WHERE client_id = :clientId AND is_active = 1 AND is_deleted = 0 ORDER BY is_primary DESC, name ASC")
    suspend fun getFacilitiesForClient(clientId: String): List<FacilityEntity>

    @Query("SELECT * FROM facilities WHERE id = :id AND is_deleted = 0")
    suspend fun getFacilityById(id: String): FacilityEntity?

    @Query("SELECT * FROM facilities WHERE client_id = :clientId AND is_primary = 1 AND is_active = 1 AND is_deleted = 0")
    suspend fun getPrimaryFacility(clientId: String): FacilityEntity?

    // ===== CRUD =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacility(facility: FacilityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacilities(facilities: List<FacilityEntity>)

    @Update
    suspend fun updateFacility(facility: FacilityEntity)

    // ===== DELETE — TWO-STAGE =====

    /**
     * Stage 1: deactivate facility and cascade to children.
     * Called by [FacilityRepositoryImpl.deactivateFacility].
     */
    @Query("UPDATE facilities SET is_active = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun deactivateFacility(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE facility_islands SET is_active = 0, updated_at = :timestamp WHERE facility_id = :facilityId")
    suspend fun deactivateIslandsByFacility(facilityId: String, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE mechanical_units SET is_active = 0, updated_at = :timestamp
        WHERE island_id IN (SELECT id FROM islands WHERE facility_id = :facilityId)
    """)
    suspend fun deactivateMechanicalUnitsByFacility(facilityId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Stage 2: mark facility and children as deleted for server sync.
     * Called by [FacilityRepositoryImpl.markFacilityDeleted].
     */
    @Query("UPDATE facilities SET is_deleted = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun markFacilityDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE facility_islands SET is_deleted = 1, updated_at = :timestamp WHERE facility_id = :facilityId")
    suspend fun markIslandDeletedByFacility(facilityId: String, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE mechanical_units SET is_deleted = 1, updated_at = :timestamp
        WHERE island_id IN (SELECT id FROM islands WHERE facility_id = :facilityId)
    """)
    suspend fun markMechanicalUnitsDeletedByFacility(facilityId: String, timestamp: Long = System.currentTimeMillis())

    // ===== PRIMARY FACILITY MANAGEMENT =====

    @Transaction
    suspend fun setPrimaryFacility(clientId: String, facilityId: String) {
        clearPrimaryFacility(clientId)
        setPrimaryFlag(facilityId, true, System.currentTimeMillis())
    }

    @Query("UPDATE facilities SET is_primary = 0, updated_at = :timestamp WHERE client_id = :clientId")
    suspend fun clearPrimaryFacility(clientId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE facilities SET is_primary = :isPrimary, updated_at = :timestamp WHERE id = :facilityId")
    suspend fun setPrimaryFlag(facilityId: String, isPrimary: Boolean, timestamp: Long)

    // ===== SEARCH =====

    @Query("""
        SELECT f.* FROM facilities f
        INNER JOIN clients c ON f.client_id = c.id
        WHERE f.is_active = 1 AND f.is_deleted = 0 AND c.is_active = 1
        AND (f.name LIKE '%' || :query || '%'
             OR f.code LIKE '%' || :query || '%'
             OR c.company_name LIKE '%' || :query || '%')
        ORDER BY f.name ASC
    """)
    suspend fun searchFacilities(query: String): List<FacilityEntity>

    @Query("SELECT * FROM facilities WHERE facility_type = :facilityType AND is_active = 1 AND is_deleted = 0 ORDER BY name ASC")
    suspend fun getFacilitiesByType(facilityType: String): List<FacilityEntity>

    // ===== VALIDATION =====

    @Query("""
        SELECT COUNT(*) > 0 FROM facilities
        WHERE client_id = :clientId AND name = :name AND id != :excludeId
        AND is_active = 1 AND is_deleted = 0
    """)
    suspend fun isFacilityNameTakenForClient(clientId: String, name: String, excludeId: String = ""): Boolean

    // ===== STATISTICS =====

    @Query("SELECT COUNT(*) FROM facilities WHERE client_id = :clientId AND is_active = 1 AND is_deleted = 0")
    suspend fun getFacilitiesCountForClient(clientId: String): Int

    @Query("SELECT COUNT(*) FROM facilities WHERE is_active = 1 AND is_deleted = 0")
    suspend fun getActiveFacilitiesCount(): Int

    @Query("""
        SELECT COUNT(*) FROM islands
        WHERE facility_id = :facilityId AND is_active = 1 AND is_deleted = 0
    """)
    suspend fun getIslandsCountForFacility(facilityId: String): Int

    // ===== BACKUP =====

    @Query("SELECT * FROM facilities ORDER BY created_at ASC")
    suspend fun getAllForBackup(): List<FacilityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(facilities: List<FacilityEntity>)

    @Query("DELETE FROM facilities")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM facilities")
    suspend fun count(): Int
}
```

> **Note on cascade queries:** the `deactivate*` and `markDeleted*` queries for `islands` and
> `mechanical_units` live in `FacilityDao` to keep the cascade inside a single `@Transaction`
> in `FacilityRepositoryImpl`. They do not replace the per-entity queries in `IslandDao` /
> `MechanicalUnitDao`, which handle their own single-entity operations.

---

## 4. REPOSITORY

### 4.1 Layer contract

The repository layer uses `kotlin.Result<T>` (not `QrResult`).
Error translation to `QrResult<D, QrError>` happens in use cases.

### 4.2 FacilityRepository interface

```kotlin
// domain/repository/FacilityRepository.kt
interface FacilityRepository {

    // ===== REACTIVE =====
    fun getAllFacilitiesFlow(): Flow<List<Facility>>
    fun getActiveFacilitiesFlow(): Flow<List<Facility>>
    fun getFacilitiesByClientFlow(clientId: String): Flow<List<Facility>>
    fun getActiveFacilitiesByClientFlow(clientId: String): Flow<List<Facility>>
    fun getFacilityByIdFlow(id: String): Flow<Facility?>

    // ===== CRUD =====
    suspend fun getAllActiveFacilities(): Result<List<Facility>>
    suspend fun getFacilityById(id: String): Result<Facility?>
    suspend fun getFacilitiesByClient(clientId: String): Result<List<Facility>>
    suspend fun getActiveFacilitiesByClient(clientId: String): Result<List<Facility>>
    suspend fun createFacility(facility: Facility): Result<Unit>
    suspend fun updateFacility(facility: Facility): Result<Unit>

    // ===== DELETE — TWO-STAGE =====
    /**
     * Stage 1: sets isActive=false on the facility and all its children
     * (islands, mechanical units) inside a single transaction.
     */
    suspend fun deactivateFacility(id: String): Result<Unit>

    /**
     * Stage 2: sets isDeleted=true on the facility and all its children
     * inside a single transaction. Only valid if isActive is already false.
     */
    suspend fun markFacilityDeleted(id: String): Result<Unit>

    // ===== PRIMARY MANAGEMENT =====
    suspend fun getPrimaryFacility(clientId: String): Result<Facility?>
    suspend fun setPrimaryFacility(clientId: String, facilityId: String): Result<Unit>
    suspend fun hasPrimaryFacility(clientId: String, excludeId: String = ""): Result<Boolean>

    // ===== SEARCH & FILTER =====
    suspend fun searchFacilities(query: String): Result<List<Facility>>
    suspend fun getFacilitiesByType(facilityType: FacilityType): Result<List<Facility>>

    // ===== VALIDATION =====
    suspend fun isFacilityNameTakenForClient(
        clientId: String,
        name: String,
        excludeId: String = ""
    ): Result<Boolean>

    // ===== STATISTICS =====
    suspend fun getActiveFacilitiesCount(): Result<Int>
    suspend fun getFacilitiesCountByClient(clientId: String): Result<Int>
    suspend fun getIslandsCount(facilityId: String): Result<Int>

    // ===== BULK =====
    suspend fun createFacilities(facilities: List<Facility>): Result<Unit>

    // ===== BACKUP =====
    suspend fun touchFacility(id: String): Result<Unit>
}
```

### 4.3 Two-stage delete — Repository implementation

```kotlin
// data/repository/FacilityRepositoryImpl.kt  (relevant methods only)

@Transaction
override suspend fun deactivateFacility(id: String): Result<Unit> = runCatching {
    val ts = System.currentTimeMillis()
    facilityDao.deactivateMechanicalUnitsByFacility(id, ts)
    facilityDao.deactivateIslandsByFacility(id, ts)
    facilityDao.deactivateFacility(id, ts)
}

@Transaction
override suspend fun markFacilityDeleted(id: String): Result<Unit> = runCatching {
    val ts = System.currentTimeMillis()
    facilityDao.markMechanicalUnitsDeletedByFacility(id, ts)
    facilityDao.markIslandDeletedByFacility(id, ts)
    facilityDao.markFacilityDeleted(id, ts)
}
```

> Children are updated before the parent so that a partial failure (unlikely with Room
> on-device, but defensive) leaves the parent still visible rather than orphaning children.

---

## 5. ERROR HANDLING

```kotlin
// presentation/core/model/QrError.kt  (add to the sealed interface)
sealed interface FacilityError : QrError {

    // ── CRUD ─────────────────────────────────────────────────────────────────

    data class LoadError(val message: String? = null) : FacilityError
    data class NotFound(val message: String? = null) : FacilityError
    data class CreateError(val message: String? = null) : FacilityError
    data class UpdateError(val message: String? = null) : FacilityError
    data class DeleteError(val message: String? = null) : FacilityError

    // ── Validation ───────────────────────────────────────────────────────────

    /** Facility name is missing or blank. */
    data class MissingName(val message: String? = null) : FacilityError

    /** Facility name is too short (< 2) or too long (> 100 characters). */
    data class InvalidName(val message: String? = null) : FacilityError

    /** clientId is blank or does not match an existing client. */
    data class MissingClientId(val message: String? = null) : FacilityError

    // ── Business rules ───────────────────────────────────────────────────────

    /** Cannot delete: this is the only active facility for the client. */
    data class CannotDeleteLastFacility(val message: String? = null) : FacilityError

    /** Cannot deactivate: facility still has active islands. */
    data class CannotDeleteHasActiveIslands(val islandsCount: Int = 0) : FacilityError

    /** Attempted second-stage delete on a facility that is still active. */
    data class CannotMarkDeletedWhileActive(val message: String? = null) : FacilityError
}
```

Use cases return `QrResult<D, QrError.FacilityError>`.

---

## 6. USE CASES

### 6.1 Full list

```
CheckFacilityExistsUseCase
CheckFacilityNameUniquenessUseCase
NewFacilityUseCase
UpdateFacilityUseCase
DeleteFacilityUseCase               ← two-stage, see §6.4
GetFacilitiesByClientUseCase
GetFacilitiesUseCase
GetFacilityByIdUseCase
GetFacilityWithIslandsUseCase
HandlePrimaryFacilityChangeUseCase
ObserveAllActiveFacilitiesUseCase
ObserveFacilitiesByClientUseCase
ObserveFacilitiesUseCase
```

### 6.2 NewFacilityUseCase

```kotlin
/**
 * Creates a new facility after validating the client, name uniqueness,
 * and handling primary facility promotion if needed.
 */
class NewFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val checkClientExists: CheckClientExistsUseCase,
    private val checkFacilityNameUniqueness: CheckFacilityNameUniquenessUseCase,
    private val handlePrimaryFacilityChange: HandlePrimaryFacilityChangeUseCase
) {
    suspend operator fun invoke(facility: Facility): QrResult<Unit, QrError.FacilityError> {

        // 1. Validate required fields
        if (facility.clientId.isBlank())
            return QrResult.Error(QrError.FacilityError.MissingClientId())
        if (facility.name.isBlank())
            return QrResult.Error(QrError.FacilityError.MissingName())

        // 2. Verify client exists
        when (checkClientExists(facility.clientId)) {
            is QrResult.Error -> return QrResult.Error(QrError.FacilityError.MissingClientId())
            is QrResult.Success -> Unit
        }

        // 3. Check name uniqueness within the client
        when (val unique = checkFacilityNameUniqueness(facility.clientId, facility.name)) {
            is QrResult.Error -> return unique
            is QrResult.Success -> Unit
        }

        // 4. Handle primary promotion if needed
        if (facility.isPrimary) {
            handlePrimaryFacilityChange(facility.clientId).onFailure {
                return QrResult.Error(QrError.FacilityError.UpdateError(it.message))
            }
        }

        // 5. Persist
        return facilityRepository.createFacility(facility).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.FacilityError.CreateError(it.message)) }
        )
    }
}
```

### 6.3 UpdateFacilityUseCase

```kotlin
/**
 * Updates an existing facility, refreshing its [Facility.updatedAt] timestamp.
 *
 * Validates: existence, name, clientId immutability, name uniqueness, primary change.
 */
class UpdateFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val checkFacilityExists: CheckFacilityExistsUseCase,
    private val checkFacilityNameUniqueness: CheckFacilityNameUniquenessUseCase
) {
    suspend operator fun invoke(facility: Facility): QrResult<Unit, QrError.FacilityError> {

        // 1. Verify exists
        val original = when (val r = checkFacilityExists(facility.id)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        // 2. clientId must not change
        if (facility.clientId != original.clientId)
            return QrResult.Error(QrError.FacilityError.UpdateError("Cannot change client of an existing facility"))

        // 3. Validate fields
        val fieldError = validateFields(facility)
        if (fieldError != null) return fieldError

        // 4. Check name uniqueness if name changed
        if (facility.name != original.name) {
            when (val u = checkFacilityNameUniqueness(facility.clientId, facility.name, facility.id)) {
                is QrResult.Error -> return QrResult.Error(u.error)
                is QrResult.Success -> Unit
            }
        }

        // 5. Handle primary change
        if (facility.isPrimary != original.isPrimary) {
            val primaryError = handlePrimaryChange(facility, original)
            if (primaryError != null) return primaryError
        }

        // 6. Persist with refreshed timestamp
        val updated = facility.copy(updatedAt = Clock.System.now())
        return facilityRepository.updateFacility(updated).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.FacilityError.UpdateError(it.message)) }
        )
    }

    private fun validateFields(facility: Facility): QrResult<Unit, QrError.FacilityError>? = when {
        facility.name.isBlank() ->
            QrResult.Error(QrError.FacilityError.MissingName())
        facility.name.length < 2 || facility.name.length > 100 ->
            QrResult.Error(QrError.FacilityError.InvalidName("Name must be 2–100 characters"))
        (facility.code?.length ?: 0) > 50 ->
            QrResult.Error(QrError.FacilityError.UpdateError("Code too long (max 50 chars)"))
        (facility.notes?.length ?: 0) > 500 ->
            QrResult.Error(QrError.FacilityError.UpdateError("Notes too long (max 500 chars)"))
        else -> null
    }

    private suspend fun handlePrimaryChange(
        facility: Facility,
        original: Facility
    ): QrResult<Unit, QrError.FacilityError>? = when {

        // Promoting to primary
        facility.isPrimary && !original.isPrimary ->
            facilityRepository.setPrimaryFacility(facility.clientId, facility.id).fold(
                onSuccess = { null },
                onFailure = { QrResult.Error(QrError.FacilityError.UpdateError(it.message)) }
            )

        // Demoting from primary: another active facility must exist to take over
        !facility.isPrimary && original.isPrimary ->
            facilityRepository.getActiveFacilitiesByClient(facility.clientId).fold(
                onSuccess = { active ->
                    val others = active.filter { it.id != facility.id }
                    if (others.isEmpty())
                        QrResult.Error(QrError.FacilityError.UpdateError(
                            "Cannot remove primary flag: no other active facility"
                        ))
                    else
                        facilityRepository.setPrimaryFacility(facility.clientId, others.first().id).fold(
                            onSuccess = { null },
                            onFailure = { QrResult.Error(QrError.FacilityError.UpdateError(it.message)) }
                        )
                },
                onFailure = { QrResult.Error(QrError.FacilityError.UpdateError(it.message)) }
            )

        else -> null
    }
}
```

### 6.4 DeleteFacilityUseCase

```kotlin
/**
 * Two-stage soft-delete for a facility.
 *
 * Stage 1 — DEACTIVATE (isActive=true → isActive=false):
 *   Cascades is_active=0 to all child islands and mechanical units.
 *   Blocked if the facility is the last active one for the client.
 *
 * Stage 2 — MARK DELETED (isActive=false → isDeleted=true):
 *   Cascades is_deleted=1 to all child islands and mechanical units.
 *   This signals the server sync layer to propagate the deletion.
 *
 * The use case reads the current state and automatically picks the correct stage.
 */
enum class DeleteFacilityResult { DEACTIVATED, MARKED_DELETED }

class DeleteFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val checkFacilityExists: CheckFacilityExistsUseCase
) {
    suspend operator fun invoke(
        facilityId: String
    ): QrResult<DeleteFacilityResult, QrError.FacilityError> {

        if (facilityId.isBlank())
            return QrResult.Error(QrError.FacilityError.NotFound())

        // 1. Load current state
        val facility = when (val r = checkFacilityExists(facilityId)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        return when {

            // ── Stage 1: facility is still active ────────────────────────────
            facility.isActive -> {

                // Business rule: cannot deactivate the only active facility
                facilityRepository.getActiveFacilitiesByClient(facility.clientId).fold(
                    onSuccess = { active ->
                        if (active.size == 1 && active.first().id == facilityId)
                            return QrResult.Error(QrError.FacilityError.CannotDeleteLastFacility())
                    },
                    onFailure = {
                        return QrResult.Error(QrError.FacilityError.LoadError(it.message))
                    }
                )

                // If primary, reassign to next active facility
                if (facility.isPrimary) {
                    facilityRepository.getActiveFacilitiesByClient(facility.clientId).fold(
                        onSuccess = { active ->
                            val next = active.firstOrNull { it.id != facilityId }
                            if (next != null) {
                                facilityRepository.setPrimaryFacility(facility.clientId, next.id)
                            }
                        },
                        onFailure = { /* non-fatal: log only */ }
                    )
                }

                facilityRepository.deactivateFacility(facilityId).fold(
                    onSuccess = { QrResult.Success(DeleteFacilityResult.DEACTIVATED) },
                    onFailure = { QrResult.Error(QrError.FacilityError.DeleteError(it.message)) }
                )
            }

            // ── Stage 2: facility is already deactivated ──────────────────────
            !facility.isActive && !facility.isDeleted -> {
                facilityRepository.markFacilityDeleted(facilityId).fold(
                    onSuccess = { QrResult.Success(DeleteFacilityResult.MARKED_DELETED) },
                    onFailure = { QrResult.Error(QrError.FacilityError.DeleteError(it.message)) }
                )
            }

            // ── Already deleted ───────────────────────────────────────────────
            else -> QrResult.Error(QrError.FacilityError.NotFound())
        }
    }
}
```

**ViewModel usage pattern:**

```kotlin
when (val result = deleteFacilityUseCase(facilityId)) {
    is QrResult.Success -> when (result.data) {
        DeleteFacilityResult.DEACTIVATED   -> showSnackbar(R.string.facility_deactivated)
        DeleteFacilityResult.MARKED_DELETED -> showSnackbar(R.string.facility_deleted)
    }
    is QrResult.Error -> showError(result.error.asUiText())
}
```

---

## 7. UI STRUCTURE

### 7.1 Screens

```
FacilityListScreen        ← FacilityListViewModel
FacilityDetailScreen      ← FacilityDetailViewModel
  ├── InfoTab             (facility data, address, map link)
  ├── IslandsTab          (islands list)
  └── MaintenanceTab      (maintenance summary across islands)
FacilityFormScreen        ← FacilityFormViewModel  (create + edit)
```

### 7.2 FacilityListScreen — key elements

```
TopAppBar
  ├── Icon + title (from FacilityPkg)
  ├── cycleCardVariant button  (FULL / COMPACT / MINIMAL)
  ├── sort button   → QReportSortOrderMenu (FacilitySortOrder.entries)
  └── filter button → QReportFilterMenu   (FacilityFilter.entries)

QReportSearchBar(query, onQueryChange)
QReportFiltersChipRow    ← shown only when filter/sort differ from defaults

QReportSelectorRow(      ← parent client selector
    items = clients,
    selectedId = uiState.selectedClientId,
    onSelect = viewModel::selectClient,
    label = R.string.facility_selector_client_label
)                        ← null selection = show all facilities

Content area (PullToRefresh wrapper):
  isLoading   → LoadingState()
  error       → QReportErrorState(error, onRetry, onDismiss)
  empty list  → EmptyState(...) with FAB shortcut
  list        → LazyColumn of FacilityCard(variant)

FAB → onCreateNewFacility
```

### 7.3 FacilityCard variants

| Variant | Button size | Contents |
|---------|------------|----------|
| FULL    | 48 dp      | Name, code chip, type chip, city, island count, primary badge, timestamps, status badge |
| COMPACT | 36 dp      | Name, code, city, island count |
| MINIMAL | —          | Name only, tap to navigate |

---

## 8. CHECKUP INTEGRATION

> **Status: not yet implemented.**

The planned integration point is `facilityId` stored on the `CheckUp` entity, enabling:
- Per-facility checkup history in `FacilityDetailScreen`
- Maintenance statistics aggregated per facility

Implementation deferred to the CheckUp feature phase.