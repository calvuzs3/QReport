# QReport — 4_1 Client Feature Reference

**Version:** 2.0
**Date:** June 2026
**Scope:** `client/client` — domain models, Room schema, repository, use cases, UI structure

---

## 1. OVERVIEW

The Client feature is the root of the data hierarchy:

```
Client → Facilities → Islands → Mechanical Units
       → Contacts
       → Contracts
```

A `Client` represents an industrial company. All other entities depend on it via foreign keys.

---

## 2. DOMAIN MODELS

### 2.1 Client

```kotlin
// domain/model/Client.kt
@Serializable
data class Client(
    val id: String,

    // ===== DATA =====
    val companyName: String,
    val notes: String? = null,

    // ===== LOCALIZATION =====
    val headquarters: Address? = null,  // Serialized as JSON in the DB

    // ===== METADATA =====
    val isActive: Boolean = true,       // false = soft-deleted
    val createdAt: Instant,
    val updatedAt: Instant
)
```

> **Note:** No `displayName` property — the domain layer is display-agnostic.
> Use `client.companyName` directly in the UI layer.

---

### 2.2 Address

```kotlin
// domain/model/Address.kt
@Serializable
data class Address(
    // ===== ADDRESS =====
    val street: String? = null,
    val streetNumber: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val province: String? = null,
    val country: String = "Italia",

    // ===== GPS COORDINATES =====
    val coordinates: GeoCoordinates? = null,

    // ===== ADDITIONAL DETAILS =====
    val notes: String? = null            // Directions / access notes
) {
    /** Formatted address for display. */
    fun toDisplayString(): String = buildString {
        if (!street.isNullOrBlank()) {
            append(street)
            if (!streetNumber.isNullOrBlank()) append(" $streetNumber")
        }
        if (!city.isNullOrBlank()) {
            if (isNotEmpty()) append(", ")
            append(city)
        }
        if (!postalCode.isNullOrBlank()) {
            if (isNotEmpty()) append(" ")
            append("($postalCode)")
        }
        if (!province.isNullOrBlank()) {
            if (isNotEmpty()) append(" - ")
            append(province)
        }
        if (!country.equals("Italia", ignoreCase = true)) {
            if (isNotEmpty()) append(", ")
            append(country)
        }
    }

    fun isComplete(): Boolean = !street.isNullOrBlank() && !city.isNullOrBlank()
    fun hasCoordinates(): Boolean = coordinates != null
}
```

---

### 2.3 GeoCoordinates

```kotlin
// domain/model/GeoCoordinates.kt
@Serializable
data class GeoCoordinates(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null         // Accuracy in meters
) {
    override fun toString(): String = "$latitude, $longitude"
    fun toGoogleMapsUrl(): String = "https://maps.google.com/?q=$latitude,$longitude"
}
```

---

### 2.4 Aggregates (read-only, not persisted)

```kotlin
// domain/model/ClientWithDetails.kt
data class ClientWithDetails(
    val client: Client,
    val facilities: List<FacilityWithIslands>,
    val contacts: List<Contact>,
    val totalCheckUps: Int,
    val lastCheckUpDate: Instant?
)

data class FacilityWithIslands(
    val facility: Facility,
    val islands: List<FacilityIsland>
)

data class ClientStatistics(
    val totalFacilities: Int,
    val totalIslands: Int,
    val totalContacts: Int,
    val checkUpsThisYear: Int,
    val lastActivity: Instant?
)

// Used by ClientListViewModel (populated from ClientWithCountsResult)
data class ClientWithStats(
    val client: Client,
    val facilitiesCount: Int,
    val contactsCount: Int,
    val islandsCount: Int
)
```

---

## 3. DATABASE SCHEMA (ROOM)

### 3.1 ClientEntity

```kotlin
// data/local/entity/ClientEntity.kt

/**
 * Room entity for the clients table.
 *
 * Sync fields:
 *  - [updatedAt]  updated on every local write (create / edit / soft-delete)
 *  - [syncedAt]   set to [updatedAt] after a successful push to the server;
 *                 null means the record has never been synced
 *  - [isDeleted]  soft-delete flag; excluded from all normal queries and
 *                 pushed to the server so other devices can mirror the deletion
 */
@Entity(
    tableName = "clients",
    indices = [
        Index(value = ["company_name"]),
        Index(value = ["is_active"]),
        Index(value = ["is_deleted"]),   // speeds up the WHERE is_deleted = 0 filter
        Index(value = ["updated_at"]),   // speeds up the delta sync query
    ]
)
data class ClientEntity(
    @PrimaryKey
    val id: String,

    // ===== DATA =====
    @ColumnInfo(name = "company_name")
    val companyName: String,
    val notes: String?,

    // ===== HEADQUARTERS JSON =====
    @ColumnInfo(name = "headquarters_json")
    val headquartersJson: String?,      // Serialized JSON of Address object

    // ===== METADATA =====
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
    val isDeleted: Boolean = false      // Soft-delete: hidden in UI, pushed to server
)
```

---

### 3.2 ClientDao

```kotlin
// data/local/dao/ClientDao.kt
@Dao
interface ClientDao {

    // ===== REACTIVE QUERIES =====

    @Query("SELECT * FROM clients WHERE is_deleted = 0 ORDER BY company_name ASC")
    fun getAllClientsFlow(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE is_active = 1 AND is_deleted = 0 ORDER BY company_name ASC")
    fun getAllActiveClientsFlow(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id AND is_deleted = 0")
    fun getClientByIdFlow(id: String): Flow<ClientEntity?>

    // ===== SUSPEND QUERIES =====

    @Query("SELECT * FROM clients WHERE is_deleted = 0 ORDER BY company_name ASC")
    suspend fun getAllClients(): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE is_active = 1 AND is_deleted = 0")
    suspend fun getActiveClients(): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE id = :id AND is_deleted = 0")
    suspend fun getClientById(id: String): ClientEntity?

    // ===== CRUD =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<ClientEntity>)

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Query("UPDATE clients SET is_active = 0, is_deleted = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun softDeleteClient(id: String, timestamp: Long = System.currentTimeMillis())

    // ===== SEARCH =====

    @Query("""
        SELECT * FROM clients
        WHERE is_active = 1 AND is_deleted = 0
        AND (company_name LIKE '%' || :query || '%'
             OR notes LIKE '%' || :query || '%')
        ORDER BY company_name ASC
    """)
    suspend fun searchClients(query: String): List<ClientEntity>

    @Query("""
        SELECT * FROM clients
        WHERE is_active = 1 AND is_deleted = 0
        AND (company_name LIKE '%' || :query || '%'
             OR notes LIKE '%' || :query || '%')
        ORDER BY company_name ASC
    """)
    fun searchClientsFlow(query: String): Flow<List<ClientEntity>>

    // ===== VALIDATION =====

    @Query("SELECT COUNT(*) > 0 FROM clients WHERE company_name = :companyName AND id != :excludeId AND is_deleted = 0")
    suspend fun isCompanyNameTaken(companyName: String, excludeId: String = ""): Boolean

    // ===== STATISTICS =====

    @Query("SELECT COUNT(*) FROM clients WHERE is_active = 1 AND is_deleted = 0")
    suspend fun getActiveClientsCount(): Int

    @Query("SELECT COUNT(*) FROM clients WHERE is_deleted = 0")
    suspend fun getTotalClientsCount(): Int

    // ===== AGGREGATED QUERY FOR LIST SCREEN =====

    /**
     * Returns clients with denormalized counts for the list screen.
     * Results mapped to [ClientWithCountsResult].
     */
    @Query("""
        SELECT c.id,
               c.company_name    AS companyName,
               c.notes,
               c.headquarters_json AS headquartersJson,
               c.is_active       AS isActive,
               c.created_at      AS createdAt,
               c.updated_at      AS updatedAt,
               COUNT(DISTINCT f.id)  AS facilitiesCount,
               COUNT(DISTINCT ct.id) AS contactsCount,
               COUNT(DISTINCT fi.id) AS islandsCount
        FROM clients c
        LEFT JOIN facilities f  ON c.id = f.client_id  AND f.is_active = 1
        LEFT JOIN contacts ct   ON c.id = ct.client_id AND ct.is_active = 1
        LEFT JOIN facility_islands fi ON f.id = fi.facility_id AND fi.is_active = 1
        WHERE c.is_active = 1 AND c.is_deleted = 0
        GROUP BY c.id, c.company_name, c.notes, c.headquarters_json,
                 c.is_active, c.created_at, c.updated_at
        ORDER BY c.company_name ASC
    """)
    suspend fun getClientsWithCounts(): List<ClientWithCountsResult>

    // ===== BACKUP =====

    @Query("SELECT * FROM clients ORDER BY created_at ASC")
    suspend fun getAllForBackup(): List<ClientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(clients: List<ClientEntity>)

    @Query("DELETE FROM clients")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun count(): Int
}

// Projection result for the aggregated list query
data class ClientWithCountsResult(
    val id: String,
    val companyName: String,
    val notes: String?,
    val headquartersJson: String?,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val facilitiesCount: Int,
    val contactsCount: Int,
    val islandsCount: Int
)
```

---

## 4. REPOSITORY

### 4.1 Layer contract

The repository layer uses `kotlin.Result<T>` (not `QrResult`).
Error translation to `QrResult<D, QrError>` happens in use cases.

### 4.2 ClientRepository interface

```kotlin
// domain/repository/ClientRepository.kt
interface ClientRepository {

    // ===== REACTIVE =====
    fun getAllClientsFlow(): Flow<List<Client>>
    fun getAllActiveClientsFlow(): Flow<List<Client>>
    fun getClientByIdFlow(id: String): Flow<Client?>

    // ===== CRUD =====
    suspend fun getAllClients(): Result<List<Client>>
    suspend fun getActiveClients(): Result<List<Client>>
    suspend fun getClientById(id: String): Result<Client?>
    suspend fun createClient(client: Client): Result<Unit>
    suspend fun updateClient(client: Client): Result<Unit>
    suspend fun deleteClient(id: String): Result<Unit>

    // ===== SEARCH =====
    suspend fun searchClients(query: String): Result<List<Client>>
    fun searchClientsFlow(query: String): Flow<List<Client>>

    // ===== VALIDATION =====
    suspend fun isCompanyNameTaken(companyName: String, excludeId: String = ""): Result<Boolean>

    // ===== STATISTICS =====
    suspend fun getActiveClientsCount(): Result<Int>
    suspend fun getTotalClientsCount(): Result<Int>
    suspend fun getFacilitiesCount(clientId: String): Result<Int>
    suspend fun getContactsCount(clientId: String): Result<Int>
    suspend fun getContractsCount(clientId: String): Result<Int>
    suspend fun getIslandsCount(clientId: String): Result<Int>

    // ===== COMPLEX =====
    suspend fun getClientsWithCounts(): Result<List<ClientWithStats>>
    suspend fun getClientsWithFacilities(): Result<List<Client>>
    suspend fun getClientsWithContacts(): Result<List<Client>>
    suspend fun getClientsWithContracts(): Result<List<Client>>
    suspend fun getClientsWithIslands(): Result<List<Client>>

    // ===== BULK =====
    suspend fun createClients(clients: List<Client>): Result<Unit>
    suspend fun deleteInactiveClients(cutoffTimestamp: Instant): Result<Int>
}
```

---

## 5. ERROR HANDLING

Feature-specific errors are defined as a nested enum inside `QrError`:

```kotlin
// presentation/core/model/QrError.kt  (add to the sealed interface)
sealed interface QrError : Error {
    // ... existing enums ...

    enum class ClientError : QrError {
        LOAD,
        NOT_FOUND,
        CREATE,
        UPDATE,
        DELETE,
        COMPANY_NAME_TAKEN,     // Duplicate name validation
        FIELDS_REQUIRED,        // Mandatory fields missing
        HAS_DEPENDENCIES        // Cannot delete: client has linked facilities/contacts
    }
}
```

Use cases return `QrResult<D, QrError.ClientError>`.

---

## 6. USE CASES

### 6.1 Full list

```
CreateClientUseCase
UpdateClientUseCase
DeleteClientUseCase
GetClientByIdUseCase
GetClientsUseCase
ObserveClientsUseCase
SearchClientsUseCase
CheckCompanyNameUniquenessUseCase
CheckClientExistsUseCase
CheckClientDependenciesUseCase
DeleteClientDependenciesUseCase
GetClientStatisticsUseCase
GetClientWithDetailsUseCase
GetActiveClientsWithFacilitiesUseCase
GetActiveClientsWithContactsUseCase
GetActiveClientsWithContractsUseCase
GetActiveClientsWithIslandsUseCase
```

### 6.2 CreateClientUseCase

```kotlin
class CreateClientUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {
    suspend operator fun invoke(
        companyName: String,
        notes: String?,
        headquarters: Address?
    ): QrResult<Unit, QrError.ClientError> {

        if (companyName.isBlank())
            return QrResult.Error(QrError.ClientError.FIELDS_REQUIRED)

        val nameTaken = clientRepository.isCompanyNameTaken(companyName)
            .getOrElse { return QrResult.Error(QrError.ClientError.LOAD) }

        if (nameTaken)
            return QrResult.Error(QrError.ClientError.COMPANY_NAME_TAKEN)

        val client = Client(
            id = UUID.randomUUID().toString(),
            companyName = companyName.trim(),
            notes = notes?.trim()?.ifBlank { null },
            headquarters = headquarters,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        return clientRepository.createClient(client)
            .fold(
                onSuccess = { QrResult.Success(Unit) },
                onFailure = { QrResult.Error(QrError.ClientError.CREATE) }
            )
    }
}
```

### 6.3 UpdateClientUseCase

```kotlin
class UpdateClientUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {
    suspend operator fun invoke(
        client: Client,
        companyName: String,
        notes: String?,
        headquarters: Address?
    ): QrResult<Unit, QrError.ClientError> {

        if (companyName.isBlank())
            return QrResult.Error(QrError.ClientError.FIELDS_REQUIRED)

        val nameTaken = clientRepository.isCompanyNameTaken(companyName, excludeId = client.id)
            .getOrElse { return QrResult.Error(QrError.ClientError.LOAD) }

        if (nameTaken)
            return QrResult.Error(QrError.ClientError.COMPANY_NAME_TAKEN)

        val updated = client.copy(
            companyName = companyName.trim(),
            notes = notes?.trim()?.ifBlank { null },
            headquarters = headquarters,
            updatedAt = Clock.System.now()
        )

        return clientRepository.updateClient(updated)
            .fold(
                onSuccess = { QrResult.Success(Unit) },
                onFailure = { QrResult.Error(QrError.ClientError.UPDATE) }
            )
    }
}
```

### 6.4 DeleteClientUseCase

```kotlin
class DeleteClientUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {
    suspend operator fun invoke(clientId: String): QrResult<Unit, QrError.ClientError> {

        // Verify existence
        clientRepository.getClientById(clientId)
            .getOrElse { return QrResult.Error(QrError.ClientError.LOAD) }
            ?: return QrResult.Error(QrError.ClientError.NOT_FOUND)

        // Check dependencies (facilities, contacts, contracts)
        val facilitiesCount = clientRepository.getFacilitiesCount(clientId)
            .getOrElse { return QrResult.Error(QrError.ClientError.LOAD) }

        if (facilitiesCount > 0)
            return QrResult.Error(QrError.ClientError.HAS_DEPENDENCIES)

        return clientRepository.deleteClient(clientId)
            .fold(
                onSuccess = { QrResult.Success(Unit) },
                onFailure = { QrResult.Error(QrError.ClientError.DELETE) }
            )
    }
}
```

---

## 7. UI STRUCTURE

### 7.1 Screens

```
ClientListScreen          ← ClientListViewModel
ClientDetailScreen        ← ClientDetailViewModel
  ├── InfoTab             (company data, address, map link)
  ├── FacilitiesTab       (facilities + islands summary)
  ├── ContactsTab         (contacts list)
  └── ContractsTab        (contracts list)
CreateClientScreen        ← CreateClientViewModel
EditClientScreen          ← EditClientViewModel
```

### 7.2 ClientListScreen — key elements

```
TopAppBar
  ├── Icon + title (from ClientPkg)
  ├── cycleCardVariant button  (FULL / COMPACT / MINIMAL)
  ├── sort button  → QReportSortOrderMenu (ClientSortOrder.entries)
  └── filter button → QReportFilterMenu  (ClientFilter.entries)

QReportSearchBar(query, onQueryChange)
QReportFiltersChipRow    ← shown only when filter/sort differ from defaults

Content area (PullToRefresh wrapper):
  isLoading   → LoadingState()
  error       → QReportErrorState(error, onRetry, onDismiss)
  empty list  → EmptyState(...)   with FAB shortcut
  list        → LazyColumn of ClientCard(variant)

FAB → onCreateNewClient
```

### 7.3 ClientCard variants

| Variant | Button size | Contents |
|---------|------------|----------|
| FULL    | 48 dp      | Name, address, stats (facilities / contacts / islands), timestamps, status badge |
| COMPACT | 36 dp      | Name, city, stats counts |
| MINIMAL | —          | Name only, tap to navigate |

---

## 8. CHECKUP INTEGRATION

> **Status: not yet implemented.**

The planned integration point is `clientId` stored on the `CheckUp` entity, enabling:
- Per-client checkup history
- Statistics (`checkUpsThisYear`, `lastCheckUpDate`) in `ClientStatistics`
- `ClientWithDetails.totalCheckUps` populated from the CheckUp repository

Implementation deferred to the CheckUp feature phase.