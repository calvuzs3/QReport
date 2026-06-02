# QReport — 4_6 Contract Feature Reference

**Version:** 1.0
**Date:** June 2026
**Scope:** `client/contract` — domain models, Room schema, repository, use cases, UI structure

---

## 1. OVERVIEW

Contract is a direct child of Client:

```
Client → Contracts
```

A `Contract` records the service agreement with a client, including date range
and the three service flags (priority response, remote assistance, maintenance).
No children entities depend on it.

---

## 2. DOMAIN MODELS

### 2.1 Contract

```kotlin
// client/contract/domain/model/Contract.kt
@Serializable
data class Contract(
    val id: String,
    val clientId: String,

    // ===== DATA =====
    val name: String? = null,
    val description: String? = null,
    val startDate: Instant,
    val endDate: Instant,

    // ===== SERVICE FLAGS =====
    val hasPriority: Boolean = true,            // 48h priority response
    val hasRemoteAssistance: Boolean = true,    // 24h remote assistance
    val hasMaintenance: Boolean = true,         // Island / robot maintenance

    val notes: String? = null,

    // ===== META =====
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    /** Returns true if the contract end date is in the future. */
    fun isValid(): Boolean = endDate > Clock.System.now()

    /** Returns true if the contract has expired. */
    fun isExpired(): Boolean = endDate <= Clock.System.now()
}
```

---

### 2.2 ContractStatistics (read-only, not persisted)

```kotlin
// client/contract/domain/model/ContractStatistics.kt
@Serializable
data class ContractStatistics(
    val totalContracts: Int,
    val activeContracts: Int,
    val inactiveContracts: Int
) {
    companion object {
        fun empty() = ContractStatistics(
            totalContracts = 0,
            activeContracts = 0,
            inactiveContracts = 0
        )
    }
}
```

---

## 3. DATABASE SCHEMA (ROOM)

### 3.1 ContractEntity

```kotlin
// client/contract/data/local/entity/ContractEntity.kt

/**
 * Room entity for the contracts table.
 *
 * Delete lifecycle:
 *  isActive=true,  isDeleted=false  →  normal
 *  isActive=false, isDeleted=false  →  deactivated  (first stage)
 *  isActive=false, isDeleted=true   →  marked deleted (second stage)
 */
@Entity(
    tableName = "contracts",
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
        Index(value = ["has_maintenance"]),
        Index(value = ["is_deleted"]),
        Index(value = ["updated_at"]),
    ]
)
data class ContractEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "client_id")
    val clientId: String,

    @ColumnInfo(name = "name")
    val name: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "start_date")
    val startDate: Long,                // Epoch milliseconds

    @ColumnInfo(name = "end_date")
    val endDate: Long,                  // Epoch milliseconds

    @ColumnInfo(name = "has_priority")
    val hasPriority: Boolean = true,

    @ColumnInfo(name = "has_remote_assistance")
    val hasRemoteAssistance: Boolean = true,

    @ColumnInfo(name = "has_maintenance")
    val hasMaintenance: Boolean = true,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,                // Epoch milliseconds

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,                // Epoch milliseconds

    // ===== SYNC =====
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)
```

---

### 3.2 ContractDao

```kotlin
// client/contract/data/local/dao/ContractDao.kt
@Dao
interface ContractDao {

    // ===== REACTIVE QUERIES =====

    @Query("SELECT * FROM contracts WHERE client_id = :clientId AND is_active = 1 AND is_deleted = 0 ORDER BY end_date ASC")
    fun getActiveContractsForClientFlow(clientId: String): Flow<List<ContractEntity>>

    @Query("SELECT * FROM contracts WHERE id = :id AND is_deleted = 0")
    fun getContractByIdFlow(id: String): Flow<ContractEntity?>

    // ===== SUSPEND QUERIES =====

    @Query("SELECT * FROM contracts WHERE client_id = :clientId AND is_active = 1 AND is_deleted = 0 ORDER BY end_date ASC")
    suspend fun getActiveContractsForClient(clientId: String): List<ContractEntity>

    @Query("SELECT * FROM contracts WHERE id = :id AND is_deleted = 0")
    suspend fun getContractById(id: String): ContractEntity?

    // ===== CRUD =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContract(contract: ContractEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContracts(contracts: List<ContractEntity>)

    @Update
    suspend fun updateContract(contract: ContractEntity)

    // ===== DELETE — TWO-STAGE =====

    @Query("UPDATE contracts SET is_active = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun deactivateContract(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE contracts SET is_deleted = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun markContractDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    // ===== SEARCH =====

    @Query("""
        SELECT * FROM contracts
        WHERE client_id = :clientId AND is_active = 1 AND is_deleted = 0
          AND (name        LIKE '%' || :query || '%'
               OR description LIKE '%' || :query || '%'
               OR notes    LIKE '%' || :query || '%')
        ORDER BY end_date ASC
    """)
    suspend fun searchContractsForClient(clientId: String, query: String): List<ContractEntity>

    @Query("SELECT * FROM contracts WHERE has_maintenance = 1 AND is_active = 1 AND is_deleted = 0")
    suspend fun getContractsWithMaintenance(): List<ContractEntity>

    // ===== STATISTICS =====

    @Query("SELECT COUNT(*) FROM contracts WHERE client_id = :clientId AND is_active = 1 AND is_deleted = 0")
    suspend fun getActiveContractsCountForClient(clientId: String): Int

    // ===== BACKUP =====

    @Query("SELECT * FROM contracts ORDER BY created_at ASC")
    suspend fun getAllForBackup(): List<ContractEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(contracts: List<ContractEntity>)

    @Query("DELETE FROM contracts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM contracts")
    suspend fun count(): Int
}
```

---

## 4. REPOSITORY

### 4.1 Layer contract

The repository layer uses `kotlin.Result<T>` (not `QrResult`).
Error translation to `QrResult<D, QrError>` happens in use cases.

### 4.2 ContractRepository interface

```kotlin
// client/contract/domain/repository/ContractRepository.kt
interface ContractRepository {

    // ===== REACTIVE =====
    fun getActiveContractsByClientFlow(clientId: String): Flow<List<Contract>>
    fun getContractByIdFlow(id: String): Flow<Contract?>

    // ===== CRUD =====
    suspend fun getContractById(id: String): Result<Contract?>
    suspend fun getActiveContractsByClient(clientId: String): Result<List<Contract>>
    suspend fun createContract(contract: Contract): Result<Unit>
    suspend fun updateContract(contract: Contract): Result<Unit>

    // ===== DELETE — TWO-STAGE =====
    suspend fun deactivateContract(id: String): Result<Unit>
    suspend fun markContractDeleted(id: String): Result<Unit>

    // ===== SEARCH =====
    suspend fun searchContractsForClient(clientId: String, query: String): Result<List<Contract>>
    suspend fun getContractsWithMaintenance(): Result<List<Contract>>

    // ===== STATISTICS =====
    suspend fun getActiveContractsCountForClient(clientId: String): Result<Int>

    // ===== BULK =====
    suspend fun createContracts(contracts: List<Contract>): Result<Unit>
}
```

---

## 5. ERROR HANDLING

```kotlin
// presentation/core/model/QrError.kt  (add to the sealed interface)
sealed interface ContractsError : QrError {

    // ── CRUD ─────────────────────────────────────────────────────────────────
    data class LoadError(val message: String? = null) : ContractsError
    data class NotFound(val message: String? = null) : ContractsError
    data class CreateError(val message: String? = null) : ContractsError
    data class UpdateError(val message: String? = null) : ContractsError
    data class DeleteError(val message: String? = null) : ContractsError

    // ── Business rules ───────────────────────────────────────────────────────
    data class MissingClientId(val message: String? = null) : ContractsError
    data class ClientNotFound(val message: String? = null) : ContractsError
    /** endDate is before startDate. */
    data class InvalidDateRange(val message: String? = null) : ContractsError
}
```

Use cases return `QrResult<D, QrError.ContractsError>`.

---

## 6. USE CASES

### 6.1 Full list

```
CheckContractExistsUseCase
CreateContractUseCase
UpdateContractUseCase
DeleteContractUseCase               ← two-stage
GetContractsByClientUseCase
GetContractByIdUseCase
ObserveContractsByClientUseCase
```

### 6.2 CreateContractUseCase

```kotlin
class CreateContractUseCase @Inject constructor(
    private val contractRepository: ContractRepository,
    private val checkClientExists: CheckClientExistsUseCase
) {
    suspend operator fun invoke(contract: Contract): QrResult<Unit, QrError.ContractsError> {

        if (contract.clientId.isBlank())
            return QrResult.Error(QrError.ContractsError.MissingClientId())

        if (contract.endDate <= contract.startDate)
            return QrResult.Error(QrError.ContractsError.InvalidDateRange())

        when (checkClientExists(contract.clientId)) {
            is QrResult.Error -> return QrResult.Error(QrError.ContractsError.ClientNotFound())
            is QrResult.Success -> Unit
        }

        return contractRepository.createContract(contract).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.ContractsError.CreateError(it.message)) }
        )
    }
}
```

### 6.3 DeleteContractUseCase

```kotlin
enum class DeleteContractResult { DEACTIVATED, MARKED_DELETED }

class DeleteContractUseCase @Inject constructor(
    private val contractRepository: ContractRepository,
    private val checkContractExists: CheckContractExistsUseCase
) {
    suspend operator fun invoke(contractId: String): QrResult<DeleteContractResult, QrError.ContractsError> {

        if (contractId.isBlank())
            return QrResult.Error(QrError.ContractsError.NotFound())

        val contract = when (val r = checkContractExists(contractId)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        return when {
            contract.isActive -> contractRepository.deactivateContract(contractId).fold(
                onSuccess = { QrResult.Success(DeleteContractResult.DEACTIVATED) },
                onFailure = { QrResult.Error(QrError.ContractsError.DeleteError(it.message)) }
            )
            !contract.isActive && !contract.isDeleted -> contractRepository.markContractDeleted(contractId).fold(
                onSuccess = { QrResult.Success(DeleteContractResult.MARKED_DELETED) },
                onFailure = { QrResult.Error(QrError.ContractsError.DeleteError(it.message)) }
            )
            else -> QrResult.Error(QrError.ContractsError.NotFound())
        }
    }
}
```

---

## 7. UI STRUCTURE

### 7.1 Screens

```
ContractListScreen      ← ContractListViewModel
ContractFormScreen      ← ContractFormViewModel  (create + edit)
```

> No detail screen — all contract data visible on the FULL card.
> Contracts also shown in the ContractsTab of ClientDetailScreen.

### 7.2 ContractListScreen — key elements

```
TopAppBar
  ├── Icon + title (from ContractPkg)
  ├── cycleCardVariant button  (FULL / COMPACT / MINIMAL)
  ├── sort button   → QReportSortOrderMenu (ContractSortOrder.entries)
  └── filter button → QReportFilterMenu   (ContractFilter.entries)

QReportSearchBar(query, onQueryChange)
QReportFiltersChipRow

QReportSelectorRow(      ← parent client selector
    items = clients,
    selectedId = uiState.selectedClientId,
    onSelect = viewModel::selectClient,
    label = R.string.contract_selector_client_label
)                        ← null = show all contracts

Content area (PullToRefresh wrapper):
  isLoading / error / empty / list → ContractCard(variant)

FAB → onCreateNewContract
```

### 7.3 ContractCard variants

| Variant | Button size | Contents |
|---------|------------|----------|
| FULL    | 48 dp      | Name, date range, service flags chips (priority / remote / maintenance), expiry status badge, notes |
| COMPACT | 36 dp      | Name, date range, service flag icons |
| MINIMAL | —          | Name + end date only, tap to edit |