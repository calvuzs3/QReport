package net.calvuz.qreport.client.facility.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.error.presentation.toUiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.usecase.ObserveClientsUseCase
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.usecase.DeleteFacilityUseCase
import net.calvuz.qreport.client.facility.domain.usecase.GetFacilityWithIslandsUseCase
import net.calvuz.qreport.client.facility.domain.usecase.ObserveFacilitiesUseCase
import net.calvuz.qreport.client.facility.domain.usecase.RestoreFacilityUseCase
import net.calvuz.qreport.client.facility.presentation.model.ClientOption
import net.calvuz.qreport.client.facility.presentation.model.FacilityFilter
import net.calvuz.qreport.client.facility.presentation.model.FacilityPkg
import net.calvuz.qreport.client.facility.presentation.model.FacilitySortOrder
import net.calvuz.qreport.settings.data.local.AppSettingsDataStore
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.domain.repository.AppSettingsRepository
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FacilityListViewModel @Inject constructor(
    private val observeFacilitiesUseCase: ObserveFacilitiesUseCase,
    private val deleteFacilityUseCase: DeleteFacilityUseCase,
    private val restoreFacilityUseCase: RestoreFacilityUseCase,
    private val getFacilityWithIslandsUseCase: GetFacilityWithIslandsUseCase,
    private val observeClientsUseCase: ObserveClientsUseCase,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    companion object {
        private const val KEY = AppSettingsDataStore.LIST_KEY_FACILITIES
    }

    private val _uiState = MutableStateFlow(FacilityListUiState())

    val uiState: StateFlow<FacilityListUiState> = _uiState.asStateFlow()
    // Tracks the active observation coroutine so switching clients never leaves stale collectors.
    private var loadJob: Job? = null

    private var currentClientId: String = ""

    init {
        Timber.d("FacilityListViewModel init")
        observeCardVariant()
        loadClientsForDropdown()
    }

    // =========================================================================
    // PUBLIC METHODS
    // =========================================================================

    fun initialize() {
        loadFacilities()
    }

    fun initializeForClient(clientId: String) {
        if (clientId == currentClientId) return
        currentClientId = clientId
        _uiState.update {
            it.copy(
                clientId = clientId,
                selectedClient = it.availableClients.find { opt -> opt.id == clientId }
                    ?: ClientOption.ALL)
        }
        loadFacilities()
    }

    fun onListEvent(event: FacilityListEvent) {
        when (event) {
            is FacilityListEvent.DeleteFacility -> deleteFacility(event.facilityId)
            is FacilityListEvent.RestoreFacility -> restoreFacility(event.facilityId)
            is FacilityListEvent.FilterChanged -> updateFilter(event.filter)
            is FacilityListEvent.SearchQueryChanged -> updateSearchQuery(event.query)
            is FacilityListEvent.SortOrderChanged -> updateSortOrder(event.sortOrder)
            is FacilityListEvent.CycleCardVariant -> cycleCardVariant()
            is FacilityListEvent.SelectedClientChanged -> updateSelectedClient(event.client)
            is FacilityListEvent.DismissError -> dismissError()
            is FacilityListEvent.Refresh -> refresh()
        }
    }

    // =========================================================================
    // PRIVATE METHODS
    // =========================================================================

    /**
     * Observes facilities via Room Flow.
     * Cancels any previous observation before starting a new one.
     */
    private fun loadFacilities() {
        loadJob?.cancel()

        val clientId = currentClientId.takeIf { it.isNotEmpty() }
        Timber.d("Observing facilities clientId=${clientId ?: "all"}")

        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = !it.isRefreshing, error = null)
            }

            try {
                observeFacilitiesUseCase(clientId).catch { exception ->
                    if (exception is CancellationException) {
                        Timber.d("Error in facilities flow")
                        throw exception
                    }
                    if (currentCoroutineContext().isActive) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = UiText.StringResource(R.string.err_facility_load)
                            )
                        }
                    }
                }.collect { facilities ->
                    if (!currentCoroutineContext().isActive) return@collect
                    val withStats = enrichWithStatistics(facilities)
                    val currentState = _uiState.value
                    _uiState.value = currentState.copy(
                        facilities = withStats, filteredFacilities = applyFiltersAndSort(
                            withStats,
                            currentState.searchQuery,
                            currentState.selectedFilter,
                            currentState.sortOrder
                        ), isLoading = false, isRefreshing = false, error = null
                    )
                    Timber.d("Received ${facilities.size} facilities from Flow")
                }
            } catch (_: CancellationException) {
                Timber.d("Facilities observation cancelled")
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.d("Unexpected error loading facilities: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = UiText.StringResource(R.string.err_facility_load)
                        )
                    }
                }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            delay(500)
            // Future: call remote sync here before restarting the observer
            loadFacilities()
        }
    }

    private fun deleteFacility(facilityId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingFacility = facilityId) }

            when (val result = deleteFacilityUseCase(facilityId)) {
                is QrResult.Success -> {
                    Timber.d("Facility deleted: $facilityId")
                    // List updates automatically via Flow
                }

                is QrResult.Error -> {
                    Timber.e("Failed to delete facility: $facilityId")
                    _uiState.update {
                        it.copy(
                            error = result.error.toUiText()
                        )
                    }
                }
            }.also { _uiState.update { it.copy(isDeletingFacility = null) } }
        }
    }

    private fun restoreFacility(facilityId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoringFacility = facilityId) }

            when (val result = restoreFacilityUseCase(facilityId)) {
                is QrResult.Success -> {
                    Timber.d("Successfully restored facility $facilityId")
                    // List updates automatically via Flow
                }

                is QrResult.Error -> {
                    Timber.d("Failed to delete facility $facilityId")
                    _uiState.update {
                        it.copy(
                            error = result.error.toUiText()
                        )
                    }
                }
            }.also { _uiState.update { it.copy(isRestoringFacility = null) } }
        }
    }

    private fun updateSearchQuery(query: String) {
        val currentState = _uiState.value
        if (query.length >= 3) {
            performSearch(query)
        } else {
            _uiState.value = currentState.copy(
                searchQuery = query,
                filteredFacilities = applyFiltersAndSort(
                    currentState.facilities,
                    query,
                    currentState.selectedFilter,
                    currentState.sortOrder
                )
            )
        }
    }

    private fun updateFilter(filter: FacilityFilter) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            selectedFilter = filter,
            filteredFacilities = applyFiltersAndSort(
                currentState.facilities,
                currentState.searchQuery,
                filter,
                currentState.sortOrder
            )
        )
    }

    private fun updateSortOrder(sortOrder: FacilitySortOrder) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            sortOrder = sortOrder, filteredFacilities = applyFiltersAndSort(
                currentState.facilities,
                currentState.searchQuery,
                currentState.selectedFilter,
                sortOrder
            )
        )
    }

    private fun updateSelectedClient(client: ClientOption) {
        if (client == _uiState.value.selectedClient) return
        currentClientId = client.id
        _uiState.update { it.copy(selectedClient = client, clientId = client.id) }
        loadFacilities()
    }

    private fun cycleCardVariant() {
        val next = when (_uiState.value.cardVariant) {
            ListViewMode.FULL -> ListViewMode.COMPACT
            ListViewMode.COMPACT -> ListViewMode.MINIMAL
            ListViewMode.MINIMAL -> ListViewMode.FULL
        }
        _uiState.update { it.copy(cardVariant = next) }
        viewModelScope.launch {
            try {
                appSettingsRepository.setListViewMode(KEY, next)
            } catch (e: Exception) {
                Timber.e(e,"Failed to persist card variant preference")
            }
        }
    }

    private fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun observeCardVariant() {
        viewModelScope.launch {
            appSettingsRepository.getListViewMode(KEY)
                .catch { e -> Timber.d("Error observing card variant preference: ${e.message}") }
                .collect { viewMode -> _uiState.update { it.copy(cardVariant = viewMode) } }
        }
    }

    private suspend fun enrichWithStatistics(facilities: List<Facility>): List<FacilityWithStats> =
        facilities.map { facility ->
            val stats = when (val result = getFacilityWithIslandsUseCase(facility.id)) {
                is QrResult.Success -> {
                    val islands = result.data.islands
                    FacilityStatistics(
                        islandsCount = islands.size,
                        activeIslandsCount = islands.count { it.isActive },
                        maintenanceDueCount = islands.count { it.needsMaintenance() })
                }

                is QrResult.Error -> {
                    Timber.w("Failed to get stats for facility ${facility.id}: ${result.error}")
                    createEmptyStats()
                }
            }
            FacilityWithStats(facility = facility, stats = stats)
        }

    private fun performSearch(query: String) {
        val currentState = _uiState.value
        val filtered = currentState.facilities.filter { facilityWithStats ->
            val f = facilityWithStats.facility
            f.name.contains(query, ignoreCase = true) || f.code?.contains(
                query, ignoreCase = true
            ) == true || f.notes?.contains(
                query, ignoreCase = true
            ) == true || f.address?.city?.contains(query, ignoreCase = true) == true
            // facilityType.displayName removed — use labelResId for display, not for search
        }
        _uiState.update { it.copy (
            searchQuery = query, filteredFacilities = applyFiltersAndSort(
                filtered, query, currentState.selectedFilter, currentState.sortOrder
            )
        ) }
    }

    private fun applyFiltersAndSort(
        facilities: List<FacilityWithStats>,
        searchQuery: String,
        filter: FacilityFilter,
        sortOrder: FacilitySortOrder
    ): List<FacilityWithStats> {
        var filtered = facilities

        // Short query local filter (≤2 chars); longer queries go through performSearch
        if (searchQuery.isNotBlank() && searchQuery.length <= 2) {
            filtered = filtered.filter { fws ->
                fws.facility.name.contains(
                    searchQuery, ignoreCase = true
                ) || fws.facility.code?.contains(searchQuery, ignoreCase = true) == true
                // facilityType.displayName removed
            }
        }

        filtered = when (filter) {
            FacilityFilter.ALL -> filtered
            FacilityFilter.ACTIVE -> filtered.filter { it.facility.isActive }
            FacilityFilter.INACTIVE -> filtered.filter { !it.facility.isActive }
            FacilityFilter.PRIMARY_ONLY -> filtered.filter { it.facility.isPrimary }
            FacilityFilter.WITH_ISLANDS -> filtered.filter { it.stats.islandsCount > 0 }
        }

        return when (sortOrder) {
            FacilitySortOrder.NAME -> filtered.sortedWith(compareByDescending<FacilityWithStats> { it.facility.isPrimary }.thenBy { it.facility.name.lowercase() })

            FacilitySortOrder.CREATED_RECENT -> filtered.sortedByDescending { it.facility.createdAt }
            FacilitySortOrder.CREATED_OLDEST -> filtered.sortedBy { it.facility.createdAt }
            FacilitySortOrder.ISLANDS_COUNT -> filtered.sortedByDescending { it.stats.islandsCount }
            FacilitySortOrder.TYPE -> filtered.sortedWith(compareBy<FacilityWithStats> { it.facility.facilityType.name }.thenBy { it.facility.name })
        }
    }

    private fun createEmptyStats() = FacilityStatistics()

    private fun loadClientsForDropdown() {
        viewModelScope.launch {
            try {
                observeClientsUseCase().catch { e -> Timber.d("Error loading clients for dropdown: ${e.message}") }
                    .collect { clients ->
                        val options = listOf(ClientOption.ALL) + clients.map { client ->
                            ClientOption(id = client.id, companyName = client.companyName)
                        }
                        _uiState.update { state ->
                            val syncedSelection =
                                options.find { it.id == state.clientId } ?: ClientOption.ALL
                            state.copy(availableClients = options, selectedClient = syncedSelection)
                        }
                    }
            } catch (e: Exception) {
                Timber.d("Failed to start client observation: ${e.message}")
            }
        }
    }
}

data class FacilityListUiState(
    val facilities: List<FacilityWithStats> = emptyList(),
    val filteredFacilities: List<FacilityWithStats> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDeletingFacility: String? = null,
    val isRestoringFacility: String? = null,
    val error: UiText? = null,              // UiText instead of raw String
    val searchQuery: String = "",
    val selectedFilter: FacilityFilter = FacilityPkg.selectedFilter,
    val sortOrder: FacilitySortOrder = FacilityPkg.selectedSortOrder,
    val clientId: String = "",
    val cardVariant: ListViewMode = ListViewMode.FULL,
    val availableClients: List<ClientOption> = listOf(ClientOption.ALL),
    val selectedClient: ClientOption = ClientOption.ALL
)

data class FacilityWithStats(
    val facility: Facility, val stats: FacilityStatistics
)

data class FacilityStatistics(
    val islandsCount: Int = 0, val activeIslandsCount: Int = 0, val maintenanceDueCount: Int = 0
)

sealed class FacilityListEvent {
    data class DeleteFacility(val facilityId: String) : FacilityListEvent()
    data class RestoreFacility(val facilityId: String) : FacilityListEvent()
    data class SearchQueryChanged(val query: String) : FacilityListEvent()
    data class FilterChanged(val filter: FacilityFilter) : FacilityListEvent()
    data class SortOrderChanged(val sortOrder: FacilitySortOrder) : FacilityListEvent()
    data class SelectedClientChanged(val client: ClientOption) : FacilityListEvent()
    object CycleCardVariant : FacilityListEvent()
    object DismissError : FacilityListEvent()
    object Refresh : FacilityListEvent()
}