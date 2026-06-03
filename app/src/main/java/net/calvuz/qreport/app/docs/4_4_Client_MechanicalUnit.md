# QReport — 4_4 MechanicalUnit Feature Reference

**Version:** 1.0
**Date:** June 2026
**Scope:** `client/unit` — domain models, Room schema, repository, use cases, UI structure

---

## 1. OVERVIEW

The MechanicalUnit feature is the leaf level of the data hierarchy:

```
Client → Facilities → Islands → Mechanical Units
```

A `MechanicalUnit` represents a single physical component of a robotic island
(robot, axis, safety fence, electrical panel, etc.).
It has no children — delete operations cascade only upward from the parent Island.

---

## 2. DOMAIN MODELS

### 2.1 MechanicalUnit

```kotlin
// client/unit/domain/model/MechanicalUnit.kt
@Serializable
data class MechanicalUnit(
    val id: String,
    val islandId: String,
    val unitType: UnitType,

    // ===== DATA =====
    val name: String,
    val serialNumber: String? = null,
    val model: String? = null,
    val notes: String? = null,

    // ===== META =====
    val isActive: Boolean = true,       // false = deactivated (first delete stage)
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)
```

> **Note:** No `displayName` — use `unit.name` directly in the UI layer.

---

### 2.2 UnitType

```kotlin
// client/unit/domain/model/UnitType.kt
@Serializable
enum class UnitType(val labelResId: Int) {
    ROBOT(R.string.unit_type_robot),
    AXIS(R.string.unit_type_axis),
    SAFETY(R.string.unit_type_safety),
    ELECTRICAL_PANEL(R.string.unit_type_electrical_panel),
    PNEUMATIC_PANEL(R.string.unit_type_pneumatic_panel),
    STATION(R.string.unit_type_station),
    MAGAZINE(R.string.unit_type_magazine),
    TOOL_RACK(R.string.unit_type_tool_rack),
    OTHER(R.string.unit_type_other)
}
```

---

## 3. DATABASE SCHEMA (ROOM)

### 3.1 MechanicalUnitEntity

```kotlin
// client/unit/data/local/entity/MechanicalUnitEntity.kt

/**
 * Room entity for the mechanical_units table.
 *
 * Delete lifecycle:
 *  isActive=true  →  normal
 *  isActive=false →  deactivated  
 *
 * Note: IslandDao contains bulk UPDATE queries targeting this table
 * to cascade deactivation/deletion from a parent Island in a single @Transaction.
 */
@Entity(
    tableName = "mechanical_units",
    foreignKeys = [
        ForeignKey(
            entity = IslandEntity::class,
            parentColumns = ["id"],
            childColumns = ["island_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["island_id"]),
        Index(value = ["is_active"]),
        Index(value = ["updated_at"]),
    ]
)
data class MechanicalUnitEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "island_id")
    val islandId: String,

    @ColumnInfo(name = "unit_type")
    val unitType: String,               // UnitType.name

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "serial_number")
    val serialNumber: String? = null,

    @ColumnInfo(name = "model")
    val model: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,                // Epoch milliseconds

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,                // Epoch milliseconds
    
    val isDeleted: Boolean = false      // Second delete stage
)
```

---

### 3.2 MechanicalUnitDao

```kotlin
// client/unit/data/local/dao/MechanicalUnitDao.kt
@Dao
interface MechanicalUnitDao {

    // ===== REACTIVE QUERIES =====

    @Query("SELECT * FROM mechanical_units WHERE island_id = :islandId AND is_active = 1 AND is_deleted = 0 ORDER BY name ASC")
    fun getActiveUnitsForIslandFlow(islandId: String): Flow<List<MechanicalUnitEntity>>

    @Query("SELECT * FROM mechanical_units WHERE id = :id AND is_deleted = 0")
    fun getUnitByIdFlow(id: String): Flow<MechanicalUnitEntity?>

    // ===== SUSPEND QUERIES =====

    @Query("SELECT * FROM mechanical_units WHERE island_id = :islandId AND is_active = 1 AND is_deleted = 0 ORDER BY name ASC")
    suspend fun getActiveUnitsForIsland(islandId: String): List<MechanicalUnitEntity>

    @Query("SELECT * FROM mechanical_units WHERE id = :id AND is_deleted = 0")
    suspend fun getUnitById(id: String): MechanicalUnitEntity?

    @Query("SELECT * FROM mechanical_units WHERE is_active = 1 AND is_deleted = 0 ORDER BY name ASC")
    suspend fun getAllActiveUnits(): List<MechanicalUnitEntity>

    // ===== CRUD =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: MechanicalUnitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<MechanicalUnitEntity>)

    @Update
    suspend fun updateUnit(unit: MechanicalUnitEntity)

    // ===== DELETE — TWO-STAGE =====

    /** Stage 1: deactivate a single unit. No children to cascade. */
    @Query("UPDATE mechanical_units SET is_active = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun deactivateUnit(id: String, timestamp: Long = System.currentTimeMillis())

    /** Stage 2: mark a single unit as deleted for server sync. */
    @Query("UPDATE mechanical_units SET is_deleted = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun markUnitDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    // ===== SEARCH =====

    @Query("""
        SELECT * FROM mechanical_units
        WHERE island_id = :islandId AND is_deleted = 0
          AND (name LIKE '%' || :query || '%'
               OR serial_number LIKE '%' || :query || '%'
               OR model LIKE '%' || :query || '%')
        ORDER BY name ASC
    """)
    suspend fun searchUnitsForIsland(islandId: String, query: String): List<MechanicalUnitEntity>

    @Query("SELECT * FROM mechanical_units WHERE unit_type = :unitType AND is_active = 1 AND is_deleted = 0 ORDER BY name ASC")
    suspend fun getUnitsByType(unitType: String): List<MechanicalUnitEntity>

    // ===== STATISTICS =====

    @Query("SELECT COUNT(*) FROM mechanical_units WHERE island_id = :islandId AND is_active = 1 AND is_deleted = 0")
    suspend fun getActiveUnitsCountForIsland(islandId: String): Int

    @Query("SELECT COUNT(*) FROM mechanical_units WHERE is_active = 1 AND is_deleted = 0")
    suspend fun getTotalActiveUnitsCount(): Int

    // ===== VALIDATION =====

    @Query("SELECT COUNT(*) > 0 FROM mechanical_units WHERE island_id = :islandId AND name = :name AND id != :excludeId AND is_active = 1 AND is_deleted = 0")
    suspend fun isNameTakenForIsland(islandId: String, name: String, excludeId: String = ""): Boolean

    // ===== BACKUP =====

    @Query("SELECT * FROM mechanical_units ORDER BY created_at ASC")
    suspend fun getAllForBackup(): List<MechanicalUnitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(units: List<MechanicalUnitEntity>)

    @Query("DELETE FROM mechanical_units")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM mechanical_units")
    suspend fun count(): Int
}
```

---

## 4. REPOSITORY

### 4.1 Layer contract

The repository layer uses `kotlin.Result<T>` (not `QrResult`).
Error translation to `QrResult<D, QrError>` happens in use cases.

### 4.2 MechanicalUnitRepository interface

```kotlin
// client/unit/domain/repository/MechanicalUnitRepository.kt
interface MechanicalUnitRepository {

    // ===== REACTIVE =====
    fun getActiveUnitsByIslandFlow(islandId: String): Flow<List<MechanicalUnit>>
    fun getUnitByIdFlow(id: String): Flow<MechanicalUnit?>

    // ===== CRUD =====
    suspend fun getAllActiveUnits(): Result<List<MechanicalUnit>>
    suspend fun getUnitById(id: String): Result<MechanicalUnit?>
    suspend fun getActiveUnitsByIsland(islandId: String): Result<List<MechanicalUnit>>
    suspend fun createUnit(unit: MechanicalUnit): Result<Unit>
    suspend fun updateUnit(unit: MechanicalUnit): Result<Unit>

    // ===== DELETE — TWO-STAGE =====
    /** Stage 1: sets isActive=false. No children to cascade. */
    suspend fun deactivateUnit(id: String): Result<Unit>
    /** Stage 2: sets isDeleted=true for server sync. */
    suspend fun markUnitDeleted(id: String): Result<Unit>

    // ===== SEARCH =====
    suspend fun searchUnitsForIsland(islandId: String, query: String): Result<List<MechanicalUnit>>
    suspend fun getUnitsByType(unitType: UnitType): Result<List<MechanicalUnit>>

    // ===== VALIDATION =====
    suspend fun isNameTakenForIsland(islandId: String, name: String, excludeId: String = ""): Result<Boolean>

    // ===== STATISTICS =====
    suspend fun getActiveUnitsCountForIsland(islandId: String): Result<Int>

    // ===== BULK =====
    suspend fun createUnits(units: List<MechanicalUnit>): Result<Unit>
}
```

---

## 5. ERROR HANDLING

```kotlin
// presentation/core/model/QrError.kt  (add to the sealed interface)
sealed interface UnitError : QrError {

    // ── CRUD ─────────────────────────────────────────────────────────────────
    data class LoadError(val message: String? = null) : UnitError
    data class NotFound(val message: String? = null) : UnitError
    data class CreateError(val message: String? = null) : UnitError
    data class UpdateError(val message: String? = null) : UnitError
    data class DeleteError(val message: String? = null) : UnitError

    // ── Validation ───────────────────────────────────────────────────────────
    data class MissingName(val message: String? = null) : UnitError
    data class InvalidField(val message: String? = null) : UnitError

    // ── Business rules ───────────────────────────────────────────────────────
    /** Unit is already deactivated or deleted. */
    data class AlreadyDeleted(val message: String? = null) : UnitError
    /** Parent island not found or inactive. */
    data class IslandNotFound(val message: String? = null) : UnitError
}
```

Use cases return `QrResult<D, QrError.UnitError>`.

---

## 6. USE CASES

### 6.1 Full list

```
CheckUnitExistsUseCase
CreateMechanicalUnitUseCase
UpdateMechanicalUnitUseCase
DeleteMechanicalUnitUseCase         ← see §6.4
GetUnitsByIslandUseCase
GetUnitByIdUseCase
ObserveUnitsByIslandUseCase
SearchUnitsUseCase
```

### 6.2 CreateMechanicalUnitUseCase

```kotlin
class CreateMechanicalUnitUseCase @Inject constructor(
    private val unitRepository: MechanicalUnitRepository,
    private val checkIslandExists: CheckIslandExistsUseCase
) {
    suspend operator fun invoke(unit: MechanicalUnit): QrResult<Unit, QrError.UnitError> {

        if (unit.name.isBlank())
            return QrResult.Error(QrError.UnitError.MissingName())

        when (checkIslandExists(unit.islandId)) {
            is QrResult.Error -> return QrResult.Error(QrError.UnitError.IslandNotFound())
            is QrResult.Success -> Unit
        }

        return unitRepository.createUnit(unit).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.UnitError.CreateError(it.message)) }
        )
    }
}
```

### 6.3 UpdateMechanicalUnitUseCase

```kotlin
class UpdateMechanicalUnitUseCase @Inject constructor(
    private val unitRepository: MechanicalUnitRepository,
    private val checkUnitExists: CheckUnitExistsUseCase
) {
    suspend operator fun invoke(unit: MechanicalUnit): QrResult<Unit, QrError.UnitError> {

        val original = when (val r = checkUnitExists(unit.id)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        // islandId must not change
        if (unit.islandId != original.islandId)
            return QrResult.Error(QrError.UnitError.UpdateError("Cannot change parent island"))

        if (unit.name.isBlank())
            return QrResult.Error(QrError.UnitError.MissingName())

        val updated = unit.copy(updatedAt = Clock.System.now())
        return unitRepository.updateUnit(updated).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.UnitError.UpdateError(it.message)) }
        )
    }
}
```

### 6.4 DeleteMechanicalUnitUseCase

```kotlin
/**
 * Soft-delete for a MechanicalUnit.
 * No children to cascade — only the unit row is affected.
 *
 * DEACTIVATE: isActive=true  → isActive=false
 */
enum class DeleteUnitResult { DEACTIVATED, MARKED_DELETED }

class DeleteMechanicalUnitUseCase @Inject constructor(
    private val unitRepository: MechanicalUnitRepository,
    private val checkUnitExists: CheckUnitExistsUseCase
) {
    suspend operator fun invoke(unitId: String): QrResult<DeleteUnitResult, QrError.UnitError> {

        if (unitId.isBlank())
            return QrResult.Error(QrError.UnitError.NotFound())

        val unit = when (val r = checkUnitExists(unitId)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        return when {
            unit.isActive -> unitRepository.deactivateUnit(unitId).fold(
                onSuccess = { QrResult.Success(DeleteUnitResult.DEACTIVATED) },
                onFailure = { QrResult.Error(QrError.UnitError.DeleteError(it.message)) }
            )
            else -> QrResult.Error(QrError.UnitError.AlreadyDeleted())
        }
    }
}
```

**ViewModel usage pattern:**

```kotlin
when (val result = deleteMechanicalUnitUseCase(unitId)) {
    is QrResult.Success -> when (result.data) {
        DeleteUnitResult.DEACTIVATED    -> showSnackbar(R.string.unit_deactivated)
        DeleteUnitResult.MARKED_DELETED -> showSnackbar(R.string.unit_deleted)
    }
    is QrResult.Error -> showError(result.error.asUiText())
}
```

---

## 7. UI STRUCTURE

### 7.1 Screens

```
MechanicalUnitListScreen    ← MechanicalUnitListViewModel
MechanicalUnitFormScreen    ← MechanicalUnitFormViewModel  (create + edit)
```

> No detail screen — all unit data is visible directly on the card (FULL variant).
> Units are also shown in the UnitsTab of IslandDetailScreen.

### 7.2 MechanicalUnitListScreen — key elements

```
TopAppBar
├── Icon + title (from UnitPkg)
├── cycleCardVariant button  (FULL / COMPACT / MINIMAL)
├── sort button   → QReportSortOrderMenu (UnitSortOrder.entries)
└── filter button → QReportFilterMenu   (UnitFilter.entries)

QReportSearchBar(query, onQueryChange)
QReportFiltersChipRow    ← shown only when filter/sort differ from defaults

QReportSelectorRow(      ← parent client selector (level 1)
items = clients,
selectedId = uiState.selectedClientId,
onSelect = viewModel::selectClient,
label = R.string.unit_selector_client_label
)
QReportSelectorRow(      ← parent facility selector (level 2)
items = uiState.facilitiesForSelectedClient,
selectedId = uiState.selectedFacilityId,
onSelect = viewModel::selectFacility,
label = R.string.unit_selector_facility_label,
enabled = uiState.selectedClientId != null
)
QReportSelectorRow(      ← parent island selector (level 3)
items = uiState.islandsForSelectedFacility,
selectedId = uiState.selectedIslandId,
onSelect = viewModel::selectIsland,
label = R.string.unit_selector_island_label,
enabled = uiState.selectedFacilityId != null
)                        ← all null = show all units

Content area (PullToRefresh wrapper):
isLoading   → LoadingState()
error       → QReportErrorState(error, onRetry, onDismiss)
empty list  → EmptyState(...) with FAB shortcut
list        → LazyColumn of MechanicalUnitCard(variant)

FAB → onCreateNewUnit
```

### 7.3 MechanicalUnitCard variants

| Variant | Button size | Contents |
|---------|------------|----------|
| FULL    | 48 dp      | Name, type chip, model, serial number, island + facility context, status badge |
| COMPACT | 36 dp      | Name, type chip, island name |
| MINIMAL | —          | Name only, tap to edit |

---

## 8. CHECKUP INTEGRATION

> **Status: not yet implemented.**

The planned integration point is `unitId` stored on individual checkup line items,
enabling per-unit inspection records within an island checkup.

Implementation deferred to the CheckUp feature phase.