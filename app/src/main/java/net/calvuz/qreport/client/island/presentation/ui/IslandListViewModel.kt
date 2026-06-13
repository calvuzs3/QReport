package net.calvuz.qreport.client.island.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.usecase.ObserveAllActiveFacilitiesUseCase
import net.calvuz.qreport.client.island.presentation.model.FacilityOption
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.usecase.DeleteIslandUseCase
import net.calvuz.qreport.client.island.domain.usecase.FacilityOperationalSummary
import net.calvuz.qreport.client.island.domain.usecase.GetIslandWithUnitsUseCase
import net.calvuz.qreport.client.island.domain.usecase.ObserveIslandsUseCase
import net.calvuz.qreport.client.island.domain.usecase.RestoreIslandUseCase
import net.calvuz.qreport.client.island.presentation.model.IslandFilter
import net.calvuz.qreport.client.island.presentation.model.IslandSortOrder
import net.calvuz.qreport.settings.data.local.AppSettingsDataStore
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.domain.repository.AppSettingsRepository
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

data class FacilityIslandListUiState(
    val islands: List<IslandWithStats> = emptyList(),
    val facilityId: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDeletingIsland: String? = null,
    val isRestoringIsland: String? = null,
    val allIslands: List<Island> = emptyList(),
    val filteredIslands: List<IslandWithStats> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: IslandFilter = IslandFilter.ACTIVE,
    val sortOrder: IslandSortOrder = IslandSortOrder.CUSTOM_NAME,
    val statistics: FacilityOperationalSummary? = null,
    val error: UiText? = null,              // UiText instead of raw String
    val cardVariant: ListViewMode = ListViewMode.FULL,
    val availableFacilities: List<FacilityOption> = listOf(FacilityOption.ALL),
    val selectedFacility: FacilityOption = FacilityOption.ALL
)

data class IslandWithStats(
    val island: Island,
    val stats: IslandStatistics
)

data class IslandStatistics(
    val unitsCount: Int = 0,
    val activeUnitsCount: Int = 0
)

@HiltViewModel
class IslandListViewModel @Inject constructor(
    private val getIslandWithUnitsUseCase: GetIslandWithUnitsUseCase,
    private val deleteIslandUseCase: DeleteIslandUseCase,
    private val restoreIslandUseCase: RestoreIslandUseCase,
    private val observeIslandsUseCase: ObserveIslandsUseCase,
    private val observeAllActiveFacilitiesUseCase: ObserveAllActiveFacilitiesUseCase,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacilityIslandListUiState())
    val uiState: StateFlow<FacilityIslandListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var currentFacilityId: String = ""

    companion object {
        private const val KEY = AppSettingsDataStore.LIST_KEY_ISLANDS
    }

    init {
        Timber.d("IslandListViewModel init")
        observeCardVariant()
        loadFacilitiesForDropdown()
    }

    // =========================================================================
    // PUBLIC
    // =========================================================================

    fun initialize(facilityId: String? = null) {
        Timber.d("IslandListViewModel initialize facilityId=$facilityId")
        when  (facilityId.isNullOrBlank()) {
            true -> loadIslands()
            false -> initializeForFacility(facilityId)
        }
    }

    fun onListEvent(event: IslandListEvent) {
        Timber.v("IslandListEvent: $event")
        when (event) {
            is IslandListEvent.DeleteIsland -> deleteIsland(event.islandId)
            is IslandListEvent.RestoreIsland -> restoreIsland(event.islandId)
            is IslandListEvent.FilterChanged -> updateFilter(event.filter)
            is IslandListEvent.SearchQueryChanged -> updateSearchQuery(event.query)
            is IslandListEvent.SortOrderChanged -> updateSortOrder(event.sortOrder)
            is IslandListEvent.CycleCardVariant -> cycleCardVariant()
            is IslandListEvent.DismissError -> dismissError()
            is IslandListEvent.Refresh -> refresh()
            is IslandListEvent.SelectedIslandChanged -> updateSelectedFacility(facility = event.facility)
        }
    }

    // =========================================================================
    // PRIVATE
    // =========================================================================

    private fun initializeForFacility(facilityId: String) {
        if (facilityId == currentFacilityId) return
        currentFacilityId = facilityId
        _uiState.update { state ->
            state.copy(
                facilityId = facilityId,
                selectedFacility = state.availableFacilities.find { it.id == facilityId }
                    ?: FacilityOption.ALL
            )
        }
        loadIslands()
    }

    private fun loadIslands() {
        loadJob?.cancel()
        val facilityId = currentFacilityId.takeIf { it.isNotEmpty() }
        Timber.d("Observing islands facilityId=${facilityId ?: "all"}")

        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = !it.isRefreshing, error = null) }
            try {
                observeIslandsUseCase(facilityId)
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        Timber.e(exception)
                        if (currentCoroutineContext().isActive) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    error = UiText.StringResource(R.string.err_island_load)
                                )
                            }
                        }
                    }
                    .collect { islands ->
                        if (!currentCoroutineContext().isActive) return@collect
                        val withStats = enrichWithStatistics(islands)
                        val current = _uiState.value
                        _uiState.value = current.copy(
                            islands = withStats,
                            allIslands = islands,
                            filteredIslands = applyFiltersAndSort(
                                withStats, current.searchQuery, current.selectedFilter, current.sortOrder
                            ),
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                        Timber.d("Loaded ${islands.size} islands from Flow")
                    }
            } catch (_: CancellationException) {
                Timber.d("Islands observation cancelled")
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = UiText.StringResource(R.string.err_island_load)
                        )
                    }
                }
            }
        }
    }

    private fun refresh() {
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        loadIslands()
    }

    private fun deleteIsland(islandId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingIsland = islandId) }
            when (val result = deleteIslandUseCase(islandId)) {
                is QrResult.Success -> Timber.d("Island deleted: $islandId")
                is QrResult.Error -> {
                    Timber.e("Failed to delete island: ${result.error}")
                    _uiState.update {
                        it.copy(error = UiText.StringResource(R.string.err_island_delete))
                    }
                }
            }
            _uiState.update { it.copy(isDeletingIsland = null) }
        }
    }

    private fun restoreIsland(islandId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoringIsland = islandId) }
            when (val result = restoreIslandUseCase(islandId)) {
                is QrResult.Success -> Timber.d("Successfully restored island $islandId")
                is QrResult.Error -> {
                    Timber.e("Failed to restore island: ${result.error}")
                    _uiState.update {
                        it.copy(error = UiText.StringResource(R.string.err_island_restore))
                    }
                }
            }
        }
    }

    private fun updateSearchQuery(query: String) {
        val current = _uiState.value
        if (query.length >= 3) {
            performSearch(query)
        } else {
            _uiState.update {
                it.copy(
                    searchQuery = query,
                    filteredIslands = applyFiltersAndSort(
                        current.islands, query, current.selectedFilter, current.sortOrder
                    )
                )
            }
        }
    }

    private fun updateFilter(filter: IslandFilter) {
        val current = _uiState.value
        _uiState.update {
            it.copy(
                selectedFilter = filter,
                filteredIslands = applyFiltersAndSort(
                    current.islands, current.searchQuery, filter, current.sortOrder
                )
            )
        }
    }

    private fun updateSortOrder(sortOrder: IslandSortOrder) {
        val current = _uiState.value
        _uiState.update {
            it.copy(
                sortOrder = sortOrder,
                filteredIslands = applyFiltersAndSort(
                    current.islands, current.searchQuery, current.selectedFilter, sortOrder
                )
            )
        }
    }

    private fun updateSelectedFacility(facility: FacilityOption) {
        if (facility == _uiState.value.selectedFacility) return
        currentFacilityId = facility.id
        _uiState.update { it.copy(selectedFacility = facility, facilityId = facility.id) }
        loadIslands()
    }

    private fun cycleCardVariant() {
        val next = when (_uiState.value.cardVariant) {
            ListViewMode.FULL -> ListViewMode.COMPACT
            ListViewMode.COMPACT -> ListViewMode.MINIMAL
            ListViewMode.MINIMAL -> ListViewMode.FULL
        }
        _uiState.update { it.copy(cardVariant = next) }
        viewModelScope.launch {
            try { appSettingsRepository.setListViewMode(KEY, next) }
            catch (e: Exception) { Timber.e(e) }
        }
    }

    private fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun observeCardVariant() {
        viewModelScope.launch {
            appSettingsRepository.getListViewMode(KEY)
                .catch { e -> Timber.e(e) }
                .collect { viewMode -> _uiState.update { it.copy(cardVariant = viewMode) } }
        }
    }

    private suspend fun enrichWithStatistics(islands: List<Island>): List<IslandWithStats> =
        islands.map { island ->
            val stats = when (val result = getIslandWithUnitsUseCase(island.id)) {
                is QrResult.Success -> IslandStatistics(
                    unitsCount = result.data.units.size,
                    activeUnitsCount = result.data.units.count { it.isActive }
                )
                is QrResult.Error -> {
                    Timber.w("Failed to get units for island ${island.id}")
                    IslandStatistics()
                }
            }
            IslandWithStats(island = island, stats = stats)
        }

    private fun performSearch(query: String) {
        val current = _uiState.value
        val filtered = current.islands.filter { iws ->
            iws.island.serialNumber.contains(query, ignoreCase = true) ||
                    iws.island.customName?.contains(query, ignoreCase = true) == true ||
                    iws.island.location?.contains(query, ignoreCase = true) == true ||
                    iws.island.notes?.contains(query, ignoreCase = true) == true
        }
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredIslands = applyFiltersAndSort(
                    filtered, query, current.selectedFilter, current.sortOrder
                )
            )
        }
    }

    private fun applyFiltersAndSort(
        islands: List<IslandWithStats>,
        searchQuery: String,
        filter: IslandFilter,
        sortOrder: IslandSortOrder
    ): List<IslandWithStats> {
        var result = islands

        // Short query local filter (≤2 chars)
        if (searchQuery.isNotBlank() && searchQuery.length <= 2) {
            val q = searchQuery.lowercase()
            result = result.filter {
                it.island.serialNumber.lowercase().contains(q) ||
                        it.island.customName?.lowercase()?.contains(q) == true ||
                        it.island.location?.lowercase()?.contains(q) == true
            }
        }

        result = when (filter) {
            IslandFilter.ALL -> result
            IslandFilter.ACTIVE -> result.filter { it.island.isActive }
            IslandFilter.INACTIVE -> result.filter { !it.island.isActive }
            IslandFilter.MAINTENANCE_DUE -> result.filter { it.island.needsMaintenance() }
            IslandFilter.UNDER_WARRANTY -> result.filter { it.island.isUnderWarranty() }
            IslandFilter.HIGH_OPERATING_HOURS -> result.filter { it.island.operatingHours > 5000 }
        }

        return when (sortOrder) {
            IslandSortOrder.SERIAL_NUMBER -> result.sortedBy { it.island.serialNumber.lowercase() }
            IslandSortOrder.TYPE -> result.sortedBy { it.island.islandType.name }
            IslandSortOrder.STATUS -> result.sortedBy { it.island.islandOperationalStatus.ordinal }
            IslandSortOrder.OPERATING_HOURS -> result.sortedByDescending { it.island.operatingHours }
            IslandSortOrder.MAINTENANCE_DATE -> result.sortedBy {
                it.island.nextScheduledMaintenance ?: Instant.DISTANT_FUTURE
            }
            IslandSortOrder.CREATED_RECENT -> result.sortedByDescending { it.island.createdAt }
            IslandSortOrder.CREATED_OLDEST -> result.sortedBy { it.island.createdAt }
            IslandSortOrder.CUSTOM_NAME -> result.sortedBy {
                it.island.customName?.lowercase() ?: it.island.serialNumber.lowercase()
            }

        }
    }

    private fun loadFacilitiesForDropdown() {
        viewModelScope.launch {
            try {
                observeAllActiveFacilitiesUseCase()
                    .catch { e -> Timber.e(e) }
                    .collect { facilities ->
                        val options = listOf(FacilityOption.ALL) + facilities.map { f ->
                            FacilityOption(id = f.id, name = f.name)
                        }
                        _uiState.update { state ->
                            val synced = options.find { it.id == state.facilityId }
                                ?: FacilityOption.ALL
                            state.copy(availableFacilities = options, selectedFacility = synced)
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}

sealed class IslandListEvent {
    data class SearchQueryChanged(val query: String) : IslandListEvent()
    data class FilterChanged(val filter: IslandFilter) : IslandListEvent()
    data class SortOrderChanged(val sortOrder: IslandSortOrder) : IslandListEvent()
    data class SelectedIslandChanged(val facility: FacilityOption) : IslandListEvent()
    object CycleCardVariant : IslandListEvent()
    data class DeleteIsland(val islandId: String) : IslandListEvent()
    data class RestoreIsland(val islandId: String) : IslandListEvent()
    object DismissError : IslandListEvent()
    object Refresh : IslandListEvent()
}