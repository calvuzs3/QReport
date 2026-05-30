# 🏢 QReport - 4_1_ Gestione Clienti

**Versione:** 1.0  
**Data:** Maggio 2026  
**Target:** Sistema completo gestione anagrafica clienti industriali

---

## 1. PANORAMICA E OBIETTIVI

### 1.1 Obiettivi Business

**🎯 Sistema Anagrafica Clienti Completo:**
- **Gestione clienti industriali** con informazioni complete

**📊 Workflow Target:**
```
Cliente → Stabilimenti → Isole → Unità 
        → Referenti
        → Contratti
```

### 1.2 Funzionalità Principali

**👥 Gestione Clienti:**
- ✅ Anagrafica completa con P.IVA e sede legale
- ✅ Localizzazione geografica con coordinate
- ✅ Storico checkup e statistiche per cliente

---

## 3. DOMAIN MODELS

### 3.1 Client - Modello Principale

```kotlin
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
) {
    /** Convenience alias used throughout the UI. */
    val displayName: String get() = companyName
}
```

### 3.5 Address - Localizzazione

```kotlin
@Serializable
data class Address(
    // ===== INDIRIZZO =====
    val street: String? = null,
    val streetNumber: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val province: String? = null,
    val country: String = "Italia",

    // ===== COORDINATE GPS =====
    val coordinates: GeoCoordinates? = null,

    // ===== DETTAGLI AGGIUNTIVI =====
    val notes: String? = null            // Indicazioni aggiuntive per raggiungere
) {

    /** Indirizzo formattato per display */
    fun toDisplayString(): String = buildString {
        if (!street.isNullOrBlank()) {
            append(street)
            if (!streetNumber.isNullOrBlank()) {
                append(" $streetNumber")
            }
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

        if (!country.equals("Italia")) {
            if (isNotEmpty()) append(", ")
            append(country)
        }
    }

    /** Verifica se indirizzo è completo */
    fun isComplete(): Boolean = !street.isNullOrBlank() && !city.isNullOrBlank()
    fun hasCoordinates(): Boolean = false
}

@Serializable
data class GeoCoordinates(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null        // Accuratezza in metri
) {

    /** Coordinate formattate */
    override fun toString(): String = "$latitude, $longitude"

    /** Link Google Maps */
    fun toGoogleMapsUrl(): String = "https://maps.google.com/?q=$latitude,$longitude"
}
```

---

## 4. ARCHITETTURA CLEAN

### 4.1 Structure Overview

### 4.2 Repository Interfaces

```kotlin
// domain/repository/ClientRepository.kt
interface ClientRepository {
    
    // ===== CLIENT CRUD =====
    suspend fun getAllClients(): List<Client>
    fun getAllClientsFlow(): Flow<List<Client>>
    suspend fun getClientById(id: String): Client?
    suspend fun createClient(client: Client): String
    suspend fun updateClient(client: Client)
    suspend fun deleteClient(id: String)
    
    // ===== SEARCH & FILTER =====
    suspend fun searchClientsByName(query: String): List<Client>
    suspend fun getActiveClients(): List<Client>
    suspend fun getClientsWithFacilities(): List<Client>
    
    // ===== COMPLEX QUERIES =====
    suspend fun getClientWithDetails(id: String): ClientWithDetails?
    suspend fun getClientStatistics(id: String): ClientStatistics
}

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
```

---

## 5. DATABASE SCHEMA (ROOM)

### 5.1 Entity Definitions

```kotlin
/**
 * Client Entity Room
 *
 * Sync fields:
 *  - [updatedAt]  updated on every local write (create / edit / soft-delete)
 *  - [syncedAt]   set to [updatedAt] value after a successful push to the server;
 *                 null means the record has never been synced
 *  - [isDeleted]  soft-delete flag; the row is excluded from all normal queries
 *                 and pushed to the server so other devices can mirror the deletion
 */
@Entity(
    tableName = "clients",
    indices = [
        Index(value = ["company_name"]),
        Index(value = ["is_active"]),
        Index(value = ["is_deleted"]),   // speeds up the WHERE is_deleted = 0 filter
        Index(value = ["updated_at"]),   // speeds up the delta query (updated_at > synced_at)
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
    val headquartersJson: String?, // Serialized JSON of Address object

    // ===== METADATA =====
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long, // Timestamp in milliseconds

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long, // Timestamp in milliseconds — updated on every local write

    // ===== SYNC =====
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null, // null = never synced; set after successful server push

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false // Soft-delete: row hidden in UI, pushed to server
)
```

### 5.2 DAO Definitions

```kotlin
@Dao
interface ClientDao {

    // ===== BASIC CRUD =====

    @Query("SELECT * FROM clients ORDER BY company_name ASC")
    fun getAllClientsFlow(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE is_active = 1 ORDER BY company_name ASC")
    fun getAllActiveClientsFlow(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients ORDER BY company_name ASC")
    suspend fun getAllClients(): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientById(id: String): ClientEntity?

    @Query("SELECT * FROM clients WHERE id = :id")
    fun getClientByIdFlow(id: String): Flow<ClientEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<ClientEntity>)

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Delete
    suspend fun deleteClient(client: ClientEntity)

    @Query("UPDATE clients SET is_active = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun softDeleteClient(id: String, timestamp: Long = System.currentTimeMillis())

    // ===== SEARCH & FILTER =====

    @Query("""
        SELECT * FROM clients 
        WHERE is_active = 1 
        AND (company_name LIKE '%' || :query || '%' 
             OR notes LIKE '%' || :query || '%' )
        ORDER BY company_name ASC
    """)
    suspend fun searchClients(query: String): List<ClientEntity>
    @Query("""
        SELECT * FROM clients 
        WHERE (company_name LIKE '%' || :query || '%' 
             OR notes LIKE '%' || :query || '%' )
        ORDER BY company_name ASC
    """)
    suspend fun searchAllClients(query: String): List<ClientEntity>

    @Query("""
        SELECT * FROM clients 
        WHERE is_active = 1 
        AND (company_name LIKE '%' || :query || '%' 
             OR notes LIKE '%' || :query || '%' )
        ORDER BY company_name ASC
    """)
    fun searchClientsFlow(query: String): Flow<List<ClientEntity>>
    @Query("""
        SELECT * FROM clients 
        WHERE (company_name LIKE '%' || :query || '%' 
             OR notes LIKE '%' || :query || '%' )
        ORDER BY company_name ASC
    """)
    fun searchAllClientsFlow(query: String): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE is_active = 1")
    suspend fun getActiveClients(): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE is_active = 0 ORDER BY updated_at DESC")
    suspend fun getInactiveClients(): List<ClientEntity>

    // ===== STATISTICS =====

    @Query("SELECT COUNT(*) FROM clients WHERE is_active = 1")
    suspend fun getActiveClientsCount(): Int

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun getTotalClientsCount(): Int

    @Query("""
        SELECT COUNT(*) FROM facilities f
        INNER JOIN clients c ON f.client_id = c.id
        WHERE c.id = :clientId AND c.is_active = 1 AND f.is_active = 1
    """)
    suspend fun getFacilitiesCount(clientId: String): Int

    @Query("""
        SELECT COUNT(*) FROM contacts ct
        INNER JOIN clients c ON ct.client_id = c.id
        WHERE c.id = :clientId AND c.is_active = 1 AND ct.is_active = 1
    """)
    suspend fun getContactsCount(clientId: String): Int

    @Query("""
        SELECT COUNT(*) FROM contracts ctr
        INNER JOIN clients c ON ctr.client_id = c.id
        WHERE c.id = :clientId AND c.is_active = 1 AND ctr.is_active = 1
    """)
    suspend fun getContractsCount(clientId: String): Int

    @Query("""
        SELECT COUNT(*) FROM facility_islands fi
        INNER JOIN facilities f ON fi.facility_id = f.id
        INNER JOIN clients c ON f.client_id = c.id
        WHERE c.id = :clientId AND c.is_active = 1 AND f.is_active = 1 AND fi.is_active = 1
    """)
    suspend fun getIslandsCount(clientId: String): Int

    // ===== VALIDATION =====

    @Query("SELECT COUNT(*) > 0 FROM clients WHERE company_name = :companyName AND id != :excludeId")
    suspend fun isCompanyNameTaken(companyName: String, excludeId: String = ""): Boolean

    // ===== BULK OPERATIONS =====

    @Transaction
    suspend fun deleteClientCompletely(clientId: String) {
        // Le foreign key CASCADE si occupano di facilities, contacts e islands
        // Ma potremmo voler fare cleanup manuale per controllo
        softDeleteClient(clientId)
    }

    @Query("DELETE FROM clients WHERE is_active = 0 AND updated_at < :cutoffTimestamp")
    suspend fun permanentlyDeleteInactiveClients(cutoffTimestamp: Long): Int

    // ===== COMPLEX QUERIES =====

    @Query("""
        SELECT c.* FROM clients c
        WHERE c.is_active = 1
        AND EXISTS (
            SELECT 1 FROM facilities f 
            WHERE f.client_id = c.id AND f.is_active = 1
        )
        ORDER BY c.company_name ASC
    """)
    suspend fun getClientsWithFacilities(): List<ClientEntity>

    @Query("""
        SELECT c.* FROM clients c
        WHERE c.is_active = 1
        AND EXISTS (
            SELECT 1 FROM contacts ct 
            WHERE ct.client_id = c.id AND ct.is_active = 1
        )
        ORDER BY c.company_name ASC
    """)
    suspend fun getClientsWithContacts(): List<ClientEntity>

    @Query("""
        SELECT c.* FROM clients c
        WHERE c.is_active = 1
        AND EXISTS (
            SELECT 1 FROM contracts ct 
            WHERE ct.client_id = c.id AND ct.is_active = 1
        )
        ORDER BY c.company_name ASC
    """)
    suspend fun getClientsWithContracts(): List<ClientEntity>

    @Query("""
        SELECT c.* FROM clients c
        WHERE c.is_active = 1
        AND EXISTS (
            SELECT 1 FROM facility_islands fi
            INNER JOIN facilities f ON fi.facility_id = f.id
            WHERE f.client_id = c.id AND fi.is_active = 1 AND f.is_active = 1
        )
        ORDER BY c.company_name ASC
    """)
    suspend fun getClientsWithIslands(): List<ClientEntity>

    // ===== QUERY CON CONTEGGI CORRETTA ===== ✅
    @Query("""
        SELECT c.id,
               c.company_name as companyName,
               c.notes,
               c.headquarters_json as headquartersJson,
               c.is_active as isActive,
               c.created_at as createdAt,
               c.updated_at as updatedAt,
               COUNT(DISTINCT f.id) as facilitiesCount,
               COUNT(DISTINCT ct.id) as contactsCount,
               COUNT(DISTINCT fi.id) as islandsCount
        FROM clients c
        LEFT JOIN facilities f ON c.id = f.client_id AND f.is_active = 1
        LEFT JOIN contacts ct ON c.id = ct.client_id AND ct.is_active = 1  
        LEFT JOIN facility_islands fi ON f.id = fi.facility_id AND fi.is_active = 1
        WHERE c.is_active = 1
        GROUP BY c.id, c.company_name, c.notes, c.headquarters_json, c.is_active, c.created_at, c.updated_at
        ORDER BY c.company_name ASC
    """)
    suspend fun getClientsWithCounts(): List<ClientWithCountsResult>

    // ===== MAINTENANCE =====

    @Query("UPDATE clients SET updated_at = :timestamp WHERE id = :id")
    suspend fun touchClient(id: String, timestamp: Long = System.currentTimeMillis())

    // ============================================================
    // BACKUP METHODS
    // ============================================================

    @Query("SELECT * FROM clients ORDER BY created_at ASC")
    suspend fun getAllForBackup(): List<ClientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(clients: List<ClientEntity>)

    @Query("DELETE FROM clients")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun count(): Int
}

data class ClientWithCountsResult(
    val id: String,
    val companyName: String,           // ✅ Matches alias in query
    val notes: String?,
    val headquartersJson: String?,     // ✅ Matches alias in query
    val isActive: Boolean,             // ✅ Matches alias in query
    val createdAt: Long,               // ✅ Matches alias in query
    val updatedAt: Long,               // ✅ Matches alias in query
    val facilitiesCount: Int,          // ✅ Matches alias in query
    val contactsCount: Int,            // ✅ Matches alias in query
    val islandsCount: Int              // ✅ Matches alias in query
)
```
---

## 6. USE CASES

### 6.1 Client Use Cases

```kotlin

```

---

## 7. REPOSITORY PATTERN

### 7.1 Repository Implementation

```kotlin
class ClientRepositoryImpl @Inject constructor(
    private val clientDao: ClientDao,
    private val clientMapper: ClientMapper
) : ClientRepository {

    // ===== CRUD OPERATIONS =====

    override suspend fun getAllClients(): Result<List<Client>> {
        return try {
            val entities = clientDao.getAllClients()
            val clients = entities.map { clientMapper.toDomain(it) }
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveClients(): Result<List<Client>> {
        return try {
            val entities = clientDao.getActiveClients()
            val clients = entities.map { clientMapper.toDomain(it) }
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getClientById(id: String): Result<Client?> {
        return try {
            val entity = clientDao.getClientById(id)
            val client = entity?.let { clientMapper.toDomain(it) }
            Result.success(client)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createClient(client: Client): Result<Unit> {
        return try {
            val entity = clientMapper.toEntity(client)
            clientDao.insertClient(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateClient(client: Client): Result<Unit> {
        return try {
            val entity = clientMapper.toEntity(client)
            clientDao.updateClient(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteClient(id: String): Result<Unit> {
        return try {
            clientDao.softDeleteClient(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== FLOW OPERATIONS (REACTIVE) =====

    override fun getAllClientsFlow(): Flow<List<Client>> {
        return clientDao.getAllClientsFlow()
            .map { entities -> entities.map { clientMapper.toDomain(it) } }
    }

    override fun getAllActiveClientsFlow(): Flow<List<Client>> {
        return clientDao.getAllActiveClientsFlow()
            .map { entities -> entities.map { clientMapper.toDomain(it) } }
    }

    override fun getClientByIdFlow(id: String): Flow<Client?> {
        return clientDao.getClientByIdFlow(id)
            .map { entity -> entity?.let { clientMapper.toDomain(it) } }
    }

    // ===== SEARCH & FILTER =====

    override suspend fun searchClients(query: String): Result<List<Client>> {
        return try {
            val entities = clientDao.searchClients(query)
            val clients = entities.map { clientMapper.toDomain(it) }
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun searchClientsFlow(query: String): Flow<List<Client>> {
        return clientDao.searchClientsFlow(query)
            .map { entities -> entities.map { clientMapper.toDomain(it) } }
    }

    // ===== VALIDATION =====

    override suspend fun isCompanyNameTaken(companyName: String, excludeId: String): Result<Boolean> {
        return try {
            val isTaken = clientDao.isCompanyNameTaken(companyName, excludeId)
            Result.success(isTaken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== STATISTICS =====

    override suspend fun getActiveClientsCount(): Result<Int> {
        return try {
            val count = clientDao.getActiveClientsCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTotalClientsCount(): Result<Int> {
        return try {
            val count = clientDao.getTotalClientsCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFacilitiesCount(clientId: String): Result<Int> {
        return try {
            val count = clientDao.getFacilitiesCount(clientId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getContactsCount(clientId: String): Result<Int> {
        return try {
            val count = clientDao.getContactsCount(clientId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getContractsCount(clientId: String): Result<Int> {
        return try {
            val count = clientDao.getContractsCount(clientId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandsCount(clientId: String): Result<Int> {
        return try {
            val count = clientDao.getIslandsCount(clientId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== COMPLEX QUERIES =====

    override suspend fun getClientsWithFacilities(): Result<List<Client>> {
        return try {
            val entities = clientDao.getClientsWithFacilities()
            val clients = entities.map { clientMapper.toDomain(it) }
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getClientsWithContacts(): Result<List<Client>> {
        return try {
            val entities = clientDao.getClientsWithContacts()
            val clients = entities.map { clientMapper.toDomain(it) }
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getClientsWithContracts(): Result<List<Client>> {
        return try {
            val entities = clientDao.getClientsWithContracts()
            val clients = entities.map { clientMapper.toDomain(it) }
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getClientsWithIslands(): Result<List<Client>> {
        return try {
            val entities = clientDao.getClientsWithIslands()
            val clients = entities.map { clientMapper.toDomain(it) }
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== BULK OPERATIONS =====

    override suspend fun createClients(clients: List<Client>): Result<Unit> {
        return try {
            val entities = clients.map { clientMapper.toEntity(it) }
            clientDao.insertClients(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteInactiveClients(cutoffTimestamp: Instant): Result<Int> {
        return try {
            val deletedCount = clientDao.permanentlyDeleteInactiveClients(cutoffTimestamp.toEpochMilliseconds())
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== MAINTENANCE =====

    override suspend fun touchClient(id: String): Result<Unit> {
        return try {
            clientDao.touchClient(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 8. UI PLANNING

### 8.1 Screen Structure

**📱 Lista Clienti (Prioritaria):**
```
ClientListScreen
├── ClientListViewModel  

```

**📋 Client Detail Screen:**
```
ClientDetailScreen
├── ClientDetailViewModel
├── Tabs:
│   ├── InfoTab (dati aziendali)
│   ├── FacilitiesTab (stabilimenti + isole)
│   ├── ContactsTab (referenti)  
│   └── ContractsTab (contratti)
```

### 8.2 ClientListScreen Design

```kotlin

/**
 * Client list screen
 *
 * Features:
 * - Client list from db with real statistics
 * - Advanced search with SearchClientsUseCase
 * - Filter for state and type
 * - Pull to refresh
 * - Optimized loading/error/empty states
 * - ClientCard reusable
 * - Dedicated SearchBar component
 */
@Suppress("ParamsComparedByRef")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientListScreen(
    onNavigateToClientDetail: (String, String) -> Unit,
    onNavigateToEditClient: (String) -> Unit,
    onCreateNewClient: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClientListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar con azioni
        TopAppBar(
            navigationIcon = { ClientPkg.icon },
            title = { Text(ClientPkg.title) },
            actions = {
                var showFilterMenu by remember { mutableStateOf(false) }
                var showSortOrderMenu by remember { mutableStateOf(false) }

                // View mode toggle button
                IconButton(onClick = viewModel::cycleCardVariant) {
                    Icon(
                        imageVector = uiState.cardVariant.getCardVariantIcon(),
                        contentDescription = uiState.cardVariant.getCardVariantDescription()
                    )
                }

                // Sort button
                IconButton(onClick = { showSortOrderMenu = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Sort,
                        contentDescription = stringResource(R.string.client_screen_list_action_sort)
                    )
                }

                // Filter button
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.client_screen_list_action_filter)
                    )
                }

                // Filter menu
                QReportFilterMenu(
                    expanded = showFilterMenu,
                    entries = ClientFilter.entries,
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = viewModel::updateFilter,
                    onDismiss = { showFilterMenu = false }
                )

                // Sort menu
                QReportSortOrderMenu (
                    expanded = showSortOrderMenu,
                    entries = ClientSortOrder.entries,
                    selectedSortOrder = uiState.selectedSortOrder,
                    onSortOrderSelected = viewModel::updateSortOrder,
                    onDismiss = { showSortOrderMenu = false }
                )
            }
        )

        // Search bar
        QReportSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            placeholder = stringResource(R.string.client_screen_list_search_placeholder),
            modifier = Modifier.padding(16.dp)
        )

        // Filter chips
        if (uiState.selectedFilter != ClientPkg.selectedFilter || uiState.selectedSortOrder != ClientPkg.selectedSortOrder) {
            QReportFiltersChipRow (
                modifier = Modifier.padding(horizontal = 16.dp),
                selectedFilter = uiState.selectedFilter,
                avoidFilter = ClientPkg.selectedFilter,
                onClearFilter = { viewModel.updateFilter(ClientPkg.selectedFilter) },
                selectedSort = uiState.selectedSortOrder,
                avoidSort = ClientPkg.selectedSortOrder,
                onClearSort = { viewModel.updateSortOrder(ClientPkg.selectedSortOrder) }
            )
        }

        // Content with Pull to Refresh
        val pullToRefreshState = rememberPullToRefreshState()

        // Handle pull to refresh
        LaunchedEffect(pullToRefreshState.isRefreshing) {
            if (pullToRefreshState.isRefreshing) {
                viewModel.refresh()
            }
        }

        // Reset refresh state when not refreshing
        LaunchedEffect(uiState.isRefreshing) {
            if (!uiState.isRefreshing && pullToRefreshState.isRefreshing) {
                pullToRefreshState.endRefresh()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {

            // Store error in local variable to avoid smart cast issues
            val currentError = uiState.error

            when {
                uiState.isLoading -> {
                    LoadingState()
                }

                currentError != null -> {
                    QReportErrorState(
                        error = currentError, // Smart cast works correctly
                        onRetry = viewModel::loadClients,
                        onDismiss = viewModel::dismissError
                    )
                }

                uiState.filteredClients.isEmpty() -> {
                    val (title, message) = when {
                        uiState.clients.isEmpty() ->
                            stringResource(R.string.client_screen_list_empty_title) to
                                    stringResource(R.string.client_screen_list_empty_message)
                        uiState.selectedFilter != ClientFilter.ALL ->
                            stringResource(R.string.client_screen_list_empty_filtered_title) to
                                    stringResource(R.string.client_screen_list_empty_filtered_message, uiState.selectedFilter.getDisplayName())
                        else ->
                            stringResource(R.string.client_screen_list_empty_generic_title) to
                                    stringResource(R.string.client_screen_list_empty_generic_message)
                    }
                    EmptyState(
                        textTitle = title,
                        textMessage = message,
                        iconImageVector = Icons.Outlined.Factory,
                        iconContentDescription = stringResource(R.string.client_screen_list_empty_icon_description),
                        iconActionImageVector = Icons.Default.Add,
                        iconActionContentDescription = stringResource(R.string.client_screen_list_fab_new),
                        textAction = stringResource(R.string.client_screen_list_empty_action),
                        onAction = onCreateNewClient
                    )
                }

                else -> {
                    ClientListContent(
                        clients = uiState.filteredClients,
                        variant = uiState.cardVariant,
                        onClientClick = onNavigateToClientDetail,
                        onClientEdit = onNavigateToEditClient,
                        onClientDelete = viewModel::inactivateClient
                    )
                }
            }

            // Pull to refresh indicator
            if (pullToRefreshState.isRefreshing || uiState.isRefreshing) {
                PullToRefreshContainer(
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            // FAB per nuovo cliente
            FloatingActionButton(
                onClick = onCreateNewClient,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.client_screen_list_fab_new)
                )
            }
        }
    }
}

@Composable
private fun ClientListContent(
    clients: List<ClientWithStats>,
    variant: ListViewMode,
    onClientClick: (String, String) -> Unit,
    onClientEdit: (String) -> Unit,
    onClientDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = clients,
            key = { it.client.id }
        ) { clientWithStats ->
            ClientCard(
                client = clientWithStats.client,
                stats = clientWithStats.stats,
                onClick = {
                    onClientClick(
                        clientWithStats.client.id,
                        clientWithStats.client.companyName
                    )
                },
                onEdit = { onClientEdit(clientWithStats.client.id) },
                //onDelete = { onClientDelete(clientWithStats.client.id) },
                onDelete = null,
                variant = variant
            )
        }
    }
}
```

---

## 9. INTEGRAZIONE CON CHECKUP
