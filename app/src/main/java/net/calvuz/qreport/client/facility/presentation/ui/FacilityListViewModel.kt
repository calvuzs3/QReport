package net.calvuz.qreport.client.facility.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import net.calvuz.qreport.client.client.domain.usecase.ObserveAllActiveClientsUseCase
import net.calvuz.qreport.client.client.presentation.model.ClientOption
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.usecase.DeleteFacilityUseCase
import net.calvuz.qreport.client.facility.domain.usecase.GetFacilityWithIslandsUseCase
import net.calvuz.qreport.client.facility.domain.usecase.ObserveFacilitiesByClientUseCase
import net.calvuz.qreport.client.facility.domain.usecase.ObserveFacilitiesUseCase
import net.calvuz.qreport.client.facility.presentation.model.FacilityFilter
import net.calvuz.qreport.client.facility.presentation.model.FacilityPkg
import net.calvuz.qreport.client.facility.presentation.model.FacilitySortOrder
import net.calvuz.qreport.settings.data.local.AppSettingsDataStore
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.domain.repository.AppSettingsRepository
import timber.log.Timber
import javax.inject.Inject

data class FacilityListUiState(
    val facilities: List<FacilityWithStats> = emptyList(),
    val filteredFacilities: List<FacilityWithStats> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDeletingFacility: String? = null,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedFilter: FacilityFilter = FacilityPkg.selectedFilter,
    val sortOrder: FacilitySortOrder = FacilityPkg.selectedSortOrder,
    val clientId: String = "",
    val cardVariant: ListViewMode = ListViewMode.FULL,
    val availableClients: List<ClientOption> = listOf(ClientOption.ALL),
    val selectedClient: ClientOption = ClientOption.ALL

)


@HiltViewModel
class FacilityListViewModel @Inject constructor(
    private val observeFacilitiesUseCase: ObserveFacilitiesUseCase,
    private val observeFacilitiesByClientUseCase: ObserveFacilitiesByClientUseCase,
    private val deleteFacilityUseCase: DeleteFacilityUseCase,
    private val getFacilityWithIslandsUseCase: GetFacilityWithIslandsUseCase,
    private val observeAllActiveClientsUseCase: ObserveAllActiveClientsUseCase,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacilityListUiState())
    val uiState: StateFlow<FacilityListUiState> = _uiState.asStateFlow()

    // Tracks the active facility-loading coroutine so it can be cancelled
    // before starting a new one (client switch, refresh, etc.).
    private var loadJob: Job? = null

    companion object {
        private const val KEY = AppSettingsDataStore.LIST_KEY_FACILITIES
    }


    init {
        loadClients()
    }

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    fun initialize() {
        loadFacilities()
    }

    fun initializeForClient(clientId: String) {
        if (clientId == _uiState.value.clientId) return

        _uiState.value = _uiState.value.copy(
            clientId = clientId,
            // Sync the dropdown selection with the client being loaded.
            // Falls back to ALL if the client list hasn't loaded yet;
            // it will be corrected once loadClients() delivers results.
            selectedClient = _uiState.value.availableClients.find { it.id == clientId }
                ?: ClientOption.ALL
        )
        loadFacilities()
    }


    /**
     * Loads facilities by observing a Room Flow.
     *
     * Cancels any previous observation before starting a new one, so switching
     * clients or calling refresh never leaves stale collectors running in parallel.
     *
     * Room re-emits automatically on every DB change, so no manual re-fetch is
     * needed for inserts, updates, or deletes.
     */
    fun loadFacilities() {
        // Cancel previous collector before starting a new one.
        loadJob?.cancel()

        val clientId = _uiState.value.clientId
        val flow = if (clientId.isEmpty()) {
            Timber.d("Observing all facilities")
            observeFacilitiesUseCase()
        } else {
            Timber.d("Observing facilities for client: $clientId")
            observeFacilitiesByClientUseCase(clientId)
        }

        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isRefreshing = false, error = null) }

            try {
                flow
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        Timber.e(exception, "Error in facilities flow")
                        if (currentCoroutineContext().isActive) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    error = "Errore caricamento stabilimenti: ${exception.message}"
                                )
                            }
                        }
                    }
                    .collect { facilities ->
                        if (!currentCoroutineContext().isActive) return@collect

                        val facilitiesWithStats = enrichWithStatistics(facilities)
                        val currentState = _uiState.value
                        val filteredAndSorted = applyFiltersAndSort(
                            facilitiesWithStats,
                            currentState.searchQuery,
                            currentState.selectedFilter,
                            currentState.sortOrder
                        )

                        _uiState.value = currentState.copy(
                            facilities = facilitiesWithStats,
                            filteredFacilities = filteredAndSorted,
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )

                        Timber.d("Received ${facilities.size} facilities from Flow")
                    }

            } catch (_: CancellationException) {
                Timber.d("Facilities observation cancelled")
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e, "Unexpected error loading facilities")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = "Errore imprevisto: ${e.message}"
                        )
                    }
                }
            }
        }
    }

    /** Refresh data */
    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        loadFacilities() // cancels old job, restarts Flow observation
    }

    /** Delete facility */
    fun softDeleteFacility(facilityId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingFacility = facilityId)

            try {
                Timber.d("Deleting facility: $facilityId")

                deleteFacilityUseCase(facilityId).fold(
                    onSuccess = {
                        Timber.d("Facility deleted successfully")
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to delete facility")
                        _uiState.value = _uiState.value.copy(
                            isDeletingFacility = null,
                            error = "Errore eliminazione stabilimento: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception deleting facility")
                _uiState.value = _uiState.value.copy(
                    error = "Errore eliminazione stabilimento: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isDeletingFacility = null)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        val currentState = _uiState.value

        if (query.length >= 3) {
            performSearch(query)
        } else {
            val filteredAndSorted = applyFiltersAndSort(
                currentState.facilities,
                query,
                currentState.selectedFilter,
                currentState.sortOrder
            )

            _uiState.value = currentState.copy(
                searchQuery = query,
                filteredFacilities = filteredAndSorted
            )
        }
    }

    fun updateFilter(filter: FacilityFilter) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.facilities,
            currentState.searchQuery,
            filter,
            currentState.sortOrder
        )

        _uiState.value = currentState.copy(
            selectedFilter = filter,
            filteredFacilities = filteredAndSorted
        )
    }

    fun updateSortOrder(sortOrder: FacilitySortOrder) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.facilities,
            currentState.searchQuery,
            currentState.selectedFilter,
            sortOrder
        )

        _uiState.value = currentState.copy(
            sortOrder = sortOrder,
            filteredFacilities = filteredAndSorted
        )
    }

    /**
     * Called when the user picks a client from the dropdown.
     * Reloads facilities scoped to that client, or all facilities if ALL is selected.
     */
    fun updateSelectedClient(client: ClientOption) {
        if (client == _uiState.value.selectedClient) return

        _uiState.update { it.copy(selectedClient = client, clientId = client.id) }
        loadFacilities()
    }

    /**
     * Cycle through card display variants: FULL -> COMPACT -> MINIMAL -> FULL.
     * The preference is persisted via [AppSettingsRepository].
     */
    fun cycleCardVariant() {
        val current = _uiState.value.cardVariant
        val next = when (current) {
            ListViewMode.FULL -> ListViewMode.COMPACT
            ListViewMode.COMPACT -> ListViewMode.MINIMAL
            ListViewMode.MINIMAL -> ListViewMode.FULL
        }

        // Update UI immediately
        _uiState.value = _uiState.value.copy(cardVariant = next)

        // Persist in background
        viewModelScope.launch {
            try {
                appSettingsRepository.setListViewMode(
                    KEY,
                    next
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist card variant preference")
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    /**
     * Observe the persisted card variant preference and apply it to UI state.
     */
    private fun observeCardVariant() {
        viewModelScope.launch {
            appSettingsRepository.getListViewMode(KEY)
                .catch { e ->
                    Timber.e(e, "Error observing card variant preference")
                }
                .collect { viewMode ->
                    _uiState.value = _uiState.value.copy(
                        cardVariant = viewMode
                    )
                }
        }
    }

    private suspend fun enrichWithStatistics(facilities: List<Facility>): List<FacilityWithStats> {
        return facilities.map { facility ->
            val stats = try {
                val facilityWithIslands = getFacilityWithIslandsUseCase(facility.id).getOrNull()
                FacilityStatistics(
                    islandsCount = facilityWithIslands?.islands?.size ?: 0,
                    activeIslandsCount = facilityWithIslands?.islands?.count { it.isActive } ?: 0,
                    maintenanceDueCount = facilityWithIslands?.islands?.count { it.needsMaintenance() }
                        ?: 0
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception getting stats for facility ${facility.id}")
                createEmptyStats()
            }

            FacilityWithStats(facility = facility, stats = stats)
        }
    }

    private fun performSearch(query: String) {
        val currentState = _uiState.value
        val filtered = currentState.facilities.filter { facilityWithStats ->
            val facility = facilityWithStats.facility
            facility.name.contains(query, ignoreCase = true) ||
                    facility.code?.contains(query, ignoreCase = true) == true ||
                    facility.notes?.contains(query, ignoreCase = true) == true ||
                    facility.facilityType.displayName.contains(query, ignoreCase = true) ||
                    facility.address?.city?.contains(query, ignoreCase = true) == true
        }

        val filteredAndSorted = applyFiltersAndSort(
            filtered,
            query,
            currentState.selectedFilter,
            currentState.sortOrder
        )

        _uiState.value = currentState.copy(
            searchQuery = query,
            filteredFacilities = filteredAndSorted
        )
    }

    private fun applyFiltersAndSort(
        facilities: List<FacilityWithStats>,
        searchQuery: String,
        filter: FacilityFilter,
        sortOrder: FacilitySortOrder
    ): List<FacilityWithStats> {
        var filtered = facilities

        // Apply status filter
        filtered = when (filter) {
            FacilityFilter.ALL -> filtered
            FacilityFilter.ACTIVE -> filtered.filter { it.facility.isActive }
            FacilityFilter.INACTIVE -> filtered.filter { !it.facility.isActive }
            FacilityFilter.PRIMARY_ONLY -> filtered.filter { it.facility.isPrimary }
            FacilityFilter.WITH_ISLANDS -> filtered.filter { it.stats.islandsCount > 0 }
            FacilityFilter.BY_TYPE -> filtered // Gestito dalla UI con selezione tipo
        }

        // Apply local search query (per query corte)
        if (searchQuery.isNotBlank() && searchQuery.length <= 2) {
            filtered = filtered.filter { facilityWithStats ->
                val facility = facilityWithStats.facility
                facility.name.contains(searchQuery, ignoreCase = true) ||
                        facility.code?.contains(searchQuery, ignoreCase = true) == true ||
                        facility.facilityType.displayName.contains(searchQuery, ignoreCase = true)
            }
        }

        // Apply sorting
        filtered = when (sortOrder) {
            FacilitySortOrder.NAME -> filtered.sortedWith(
                compareByDescending<FacilityWithStats> { it.facility.isPrimary }
                    .thenBy { it.facility.name.lowercase() }
            )

            FacilitySortOrder.CREATED_RECENT -> filtered.sortedByDescending { it.facility.createdAt }
            FacilitySortOrder.CREATED_OLDEST -> filtered.sortedBy { it.facility.createdAt }
            FacilitySortOrder.ISLANDS_COUNT -> filtered.sortedByDescending { it.stats.islandsCount }
            FacilitySortOrder.TYPE -> filtered.sortedWith(
                compareBy<FacilityWithStats> { it.facility.facilityType.name }
                    .thenBy { it.facility.name }
            )
        }

        return filtered
    }

    private fun createEmptyStats() = FacilityStatistics(
        islandsCount = 0,
        activeIslandsCount = 0,
        maintenanceDueCount = 0
    )

    /**
     * Observes all clients and keeps [FacilityListUiState.availableClients] up to date.
     * The ALL sentinel is always prepended so the user can clear the client filter.
     */
    private fun loadClients() {
        viewModelScope.launch {
            try {
                observeAllActiveClientsUseCase()
                    .catch { e -> Timber.e(e, "Error loading clients for dropdown") }
                    .collect { clients ->
                        val options = listOf(ClientOption.ALL) + clients.map { client ->
                            ClientOption(id = client.id, companyName = client.companyName)
                        }
                        _uiState.update { state ->
                            // Also fix selectedClient if it was set before clients loaded
                            val syncedSelection = options.find { it.id == state.clientId }
                                ?: ClientOption.ALL
                            state.copy(
                                availableClients = options,
                                selectedClient = syncedSelection
                            )
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start client observation")
            }
        }
    }

}

/**
 * Data class per facility con statistiche
 */
data class FacilityWithStats(
    val facility: Facility,
    val stats: FacilityStatistics
)

/**
 * Statistiche per facility
 */
data class FacilityStatistics(
    val islandsCount: Int = 0,
    val activeIslandsCount: Int = 0,
    val maintenanceDueCount: Int = 0
) {
    val hasIslands: Boolean = islandsCount > 0
    val hasMaintenanceIssues: Boolean = maintenanceDueCount > 0
}