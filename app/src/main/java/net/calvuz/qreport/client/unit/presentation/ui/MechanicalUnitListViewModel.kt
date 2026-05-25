package net.calvuz.qreport.client.unit.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.calvuz.qreport.client.island.domain.usecase.ObserveIslandsUseCase
import net.calvuz.qreport.client.island.presentation.ui.components.IslandOption
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.model.UnitType
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import net.calvuz.qreport.client.unit.domain.usecase.ObserveMechanicalUnitsUseCase
import net.calvuz.qreport.client.unit.presentation.model.MechanicalUnitFilter
import net.calvuz.qreport.client.unit.presentation.model.MechanicalUnitPkg
import net.calvuz.qreport.client.unit.presentation.model.MechanicalUnitSortOrder
import net.calvuz.qreport.settings.data.local.AppSettingsDataStore
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.domain.repository.AppSettingsRepository
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * UI State for [MechanicalUnitListScreen].
 */
data class MechanicalUnitListUiState(
    val allUnits: List<MechanicalUnit> = emptyList(),
    val filteredUnits: List<MechanicalUnit> = emptyList(),
    val islandId: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDeletingUnit: String? = null,
    val searchQuery: String = "",
    val selectedFilter: MechanicalUnitFilter = MechanicalUnitPkg.selectedFilter,
    val sortOrder: MechanicalUnitSortOrder = MechanicalUnitPkg.selectedSortOrder,
    val error: String? = null,
    val cardVariant: ListViewMode = ListViewMode.FULL,
    val availableIslands: List<IslandOption> = listOf(IslandOption.ALL),
    val selectedIsland: IslandOption = IslandOption.ALL
)

@HiltViewModel
class MechanicalUnitListViewModel @Inject constructor(
    private val repository: MechanicalUnitRepository,
    private val observeIslandsUseCase: ObserveIslandsUseCase,
    private val observeMechanicalUnitsUseCase: ObserveMechanicalUnitsUseCase,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MechanicalUnitListUiState())
    val uiState: StateFlow<MechanicalUnitListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var currentIslandId: String = ""

    companion object {
        private const val KEY = AppSettingsDataStore.LIST_KEY_MECHANICAL_UNITS
    }

    init {
        Timber.d("MechanicalUnitListViewModel init")
        observeCardVariant()        // Restore persisted card variant
        loadIslandsForDropdown()    // Populate island selector
    }

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    fun initialize() {
        loadUnits()
    }

    /**
     * Init for a specific island
     */
    fun initializeForIsland(islandId: String) {
        if (islandId == currentIslandId) return

        currentIslandId = islandId
        _uiState.update { state ->
            state.copy(
                islandId = islandId,
                // Sync dropdown; corrected once loadIslandsForDropdown() delivers results.
                selectedIsland = state.availableIslands.find { it.id == islandId }
                    ?: IslandOption.ALL
            )
        }
        loadUnits()
    }

    /**
     * Loads units by observing a Room Flow.
     *
     * Cancels any previous observation before starting a new one, so switching
     * clients or calling refresh never leaves stale collectors running in parallel.
     *
     * Room re-emits automatically on every DB change, so no manual re-fetch is
     * needed for inserts, updates, or deletes.
     */
    fun loadUnits() {
        loadJob?.cancel()

        val islandId = currentIslandId
        val flow = if (islandId.isEmpty()) {
            Timber.d("Observing all Facilities")
            observeMechanicalUnitsUseCase(null)
        } else {
            Timber.d("Observing islands for facility: $islandId")
            observeMechanicalUnitsUseCase(islandId)
        }

        loadJob = viewModelScope.launch {
            // If called from refresh(), keep isRefreshing=true visible until data arrives.
            // If called fresh, show isLoading instead.
            _uiState.update { it.copy(
                isLoading = !it.isRefreshing,
                error = null
            ) }

            try {
                Timber.d("Loading units for island: $islandId")

                flow
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        Timber.e(exception, "Error in units flow")
                        if (currentCoroutineContext().isActive) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    error = "Errore caricamento unità: ${exception.message}"
                                )
                            }
                        }
                    }
                    .collect { units ->
                        if (!currentCoroutineContext().isActive) return@collect

                        val currentState = _uiState.value
                        val filteredAndSorted = applyFiltersAndSort(
                            units,
                            currentState.searchQuery,
                            currentState.selectedFilter,
                            currentState.sortOrder
                        )

                        _uiState.value = currentState.copy(
                            allUnits = units,
                            filteredUnits = filteredAndSorted,
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )

                        Timber.d("Loaded ${units.size} units successfully")
                    }

            } catch (_: CancellationException) {
                Timber.d("Units loading cancelled")
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    _uiState.update {
                        it.copy(
                            isLoading = false, isRefreshing = false,
                            error = "Errore imprevisto: ${e.message}"
                        )
                    }
                }
            }
        }
    }

    /** Refresh data */
    fun refresh() {
        if (currentIslandId.isBlank()) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        loadUnits() // cancels old job, restarts Flow; isRefreshing cleared when data arrives
    }

    fun deleteUnit(unitId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingUnit = unitId) }

            try {
                Timber.d("Deleting unit: $unitId")

                repository.delete(unitId).fold(
                    onSuccess = {
                        Timber.d("Unit deleted successfully")
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to delete unit")
                        _uiState.update {
                            it.copy(error = "Errore eliminazione unità: ${error.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception deleting unit")
                _uiState.update { it.copy(error = "Errore eliminazione unità: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isDeletingUnit = null) }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        if (query.length >= 3) {
            performSearch(query)
        } else {
            val currentState = _uiState.value
            val filteredAndSorted = applyFiltersAndSort(
                units = currentState.allUnits,
                searchQuery =  query,
                filter = currentState.selectedFilter,
                sortOrder = currentState.sortOrder
            )
            _uiState.update { it.copy(
                searchQuery = query,
                filteredUnits = filteredAndSorted)
            }
        }
    }

    fun updateFilter(filter: MechanicalUnitFilter) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.allUnits, currentState.searchQuery,
            filter, currentState.sortOrder
        )
        _uiState.update { it.copy(
            selectedFilter = filter,
            filteredUnits = filteredAndSorted)
        }
    }

    fun updateSortOrder(sortOrder: MechanicalUnitSortOrder) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.allUnits, currentState.searchQuery,
            currentState.selectedFilter, sortOrder
        )
        _uiState.update { it.copy(
            sortOrder = sortOrder,
            filteredUnits = filteredAndSorted)
        }
    }

    /**
     * Called when the user picks an island from the dropdown.
     * Reloads mechanical units scoped to that facility.
     */
    fun updateSelectedIsland(island: IslandOption) {
        if (island == _uiState.value.selectedIsland) return

        currentIslandId = island.id
        _uiState.update { it.copy(selectedIsland = island, islandId = island.id) }
        loadUnits()
    }

    fun cycleCardVariant() {
        val next = when (_uiState.value.cardVariant) {
            ListViewMode.FULL -> ListViewMode.COMPACT
            ListViewMode.COMPACT -> ListViewMode.MINIMAL
            ListViewMode.MINIMAL -> ListViewMode.FULL
        }

        // Update UI immediately
        _uiState.value = _uiState.value.copy(cardVariant = next)

        // Persist in background
        viewModelScope.launch {
            try {
                appSettingsRepository.setListViewMode(KEY, next)
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist card variant preference")
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    private fun observeCardVariant() {
        viewModelScope.launch {
            appSettingsRepository.getListViewMode(KEY)
                .catch { e -> Timber.e(e, "Error observing card variant preference") }
                .collect { viewMode ->
                    _uiState.value = _uiState.value.copy(cardVariant = viewMode)
                }
        }
    }

    private fun performSearch(query: String) {
        val currentState = _uiState.value
        val filtered = currentState.allUnits.filter { unit ->
            unit.name.contains(query, ignoreCase = true) ||
                    unit.serialNumber?.contains(query, ignoreCase = true) == true ||
                    unit.model?.contains(query, ignoreCase = true) == true ||
                    unit.unitType.displayName.contains(query, ignoreCase = true)
        }

        val filteredAndSorted = applyFiltersAndSort(
            filtered, query, currentState.selectedFilter, currentState.sortOrder
        )

        _uiState.value = currentState.copy(
            searchQuery = query,
            filteredUnits = filteredAndSorted
        )
    }

    private fun applyFiltersAndSort(
        units: List<MechanicalUnit>,
        searchQuery: String,
        filter: MechanicalUnitFilter,
        sortOrder: MechanicalUnitSortOrder
    ): List<MechanicalUnit> {
        var result = units

        // Apply text search for short queries (longer ones handled by performSearch)
        if (searchQuery.isNotBlank() && searchQuery.length < 3) {
            result = result.filter { unit ->
                unit.name.contains(searchQuery, ignoreCase = true) ||
                        unit.unitType.displayName.contains(searchQuery, ignoreCase = true)
            }
        }

        // Apply status / type filter
        result = when (filter) {
            MechanicalUnitFilter.ALL -> result
            MechanicalUnitFilter.ACTIVE -> result.filter { it.isActive }
            MechanicalUnitFilter.INACTIVE -> result.filter { !it.isActive }
            MechanicalUnitFilter.ROBOT -> result.filter { it.unitType == UnitType.ROBOT }
        }

        // Apply sorting
        result = when (sortOrder) {
            MechanicalUnitSortOrder.CREATED_RECENT -> result.sortedByDescending { it.createdAt }
            MechanicalUnitSortOrder.NAME -> result.sortedBy { it.name.lowercase() }
            MechanicalUnitSortOrder.BY_TYPE -> result.sortedWith(
                compareBy<MechanicalUnit> { it.unitType.name }.thenBy { it.name.lowercase() }
            )
            MechanicalUnitSortOrder.BY_SERIAL -> result.sortedWith(
                compareBy<MechanicalUnit> { it.serialNumber }.thenBy { it.serialNumber }
            )
        }

        return result
    }

    /**
     * Observes all islands and keeps [MechanicalUnitListUiState.availableIslands] up to date.
     * Syncs [selectedIsland] once the list arrives so the dropdown shows the correct name
     * even if navigation set the id before the list loaded.
     */
    private fun loadIslandsForDropdown() {
        viewModelScope.launch {
            try {
                observeIslandsUseCase()
                    .catch { e -> Timber.e(e, "Error loading islands for dropdown") }
                    .collect { islands ->
                        val options = listOf(IslandOption.ALL) + islands.map { island ->
                            IslandOption(id = island.id, name = island.serialNumber)
                        }
                        _uiState.update { state ->
                            val syncedSelection = options.find { it.id == state.islandId }
                                ?: IslandOption.ALL
                            state.copy(
                                availableIslands = options,
                                selectedIsland = syncedSelection
                            )
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start islands observation for dropdown")
            }
        }
    }
}

/**
 * UI events for [MechanicalUnitListScreen].
 */
sealed class MechanicalUnitListEvent {
    data class SearchQueryChanged(val query: String) : MechanicalUnitListEvent()
    data class FilterChanged(val filter: MechanicalUnitFilter) : MechanicalUnitListEvent()
    data class SortOrderChanged(val sortOrder: MechanicalUnitSortOrder) : MechanicalUnitListEvent()
    data class DeleteUnit(val unitId: String) : MechanicalUnitListEvent()
    object Refresh : MechanicalUnitListEvent()
    object DismissError : MechanicalUnitListEvent()
}


