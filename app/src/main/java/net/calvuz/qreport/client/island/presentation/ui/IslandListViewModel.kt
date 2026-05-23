package net.calvuz.qreport.client.island.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.usecase.DeleteIslandUseCase
import net.calvuz.qreport.client.island.domain.usecase.GetIslandsByFacilityUseCase
import net.calvuz.qreport.client.island.domain.usecase.FacilityOperationalSummary
import net.calvuz.qreport.client.island.domain.usecase.GetIslandWithUnitsUseCase
import net.calvuz.qreport.client.island.presentation.model.IslandFilter
import net.calvuz.qreport.client.island.presentation.model.IslandSortOrder
import net.calvuz.qreport.settings.data.local.AppSettingsDataStore
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.domain.repository.AppSettingsRepository
import timber.log.Timber
import javax.inject.Inject
import kotlin.collections.count
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.coroutines.cancellation.CancellationException
import kotlin.text.contains

/**
 * ISLAND UI State
 */
data class FacilityIslandListUiState(
    val islands: List<IslandWithStats> = emptyList(),
    val facilityId: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDeletingIsland: String? = null,
    val allIslands: List<Island> = emptyList(),
    val filteredIslands: List<IslandWithStats> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: IslandFilter = IslandFilter.ACTIVE,
    val sortOrder: IslandSortOrder = IslandSortOrder.CUSTOM_NAME,
    val statistics: FacilityOperationalSummary? = null,
    val searchSuggestions: List<Island> = emptyList(),
    val error: String? = null,
    val cardVariant: ListViewMode = ListViewMode.FULL
)

/**
 * ISLAND WITH STATS
 */
data class IslandWithStats(
    val island: Island,
    val stats: IslandStatistics
)

/**
 * ISLAND STATS
 */
data class IslandStatistics(
    val unitsCount: Int = 0,
    val activeUnitsCount: Int = 0,
) {
    val hasUnits: Boolean = unitsCount > 0
}

@HiltViewModel
class IslandListViewModel @Inject constructor(
    private val getIslandsByFacilityUseCase: GetIslandsByFacilityUseCase,
    private val getIslandWithUnitsUseCase: GetIslandWithUnitsUseCase,
    private val deleteIslandUseCase: DeleteIslandUseCase,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacilityIslandListUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var currentFacilityId: String = ""

    companion object {
        private const val KEY = AppSettingsDataStore.LIST_KEY_ISLANDS
    }

    init {
        Timber.d("IslandListViewModel init")
    }
    /**
     * Inizializza per una facility specifica
     */
    fun initializeForFacility(facilityId: String) {
        if (facilityId != currentFacilityId) {
            currentFacilityId = facilityId
            loadIslands()
        } else {
            Timber.d("facilityId == currentFacilityId")
        }
    }

    /**
     * Carica isole della facility
     */
    fun loadIslands() {
        //val facilityId = _uiState.value.facilityId
        val facilityId = currentFacilityId
        if (facilityId.isEmpty()) return


        if (currentFacilityId.isBlank()) {
            Timber.d("currentFacilityId is blank")
            return
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                Timber.d("Loading islands for facility: $facilityId")

                getIslandsByFacilityUseCase.observeIslandsByFacility(currentFacilityId)
                    .catch { exception ->
                        if (exception is kotlinx.coroutines.CancellationException) throw exception
                        Timber.e(exception, "Error in islands flow")
                        if (currentCoroutineContext().isActive) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = "Errore caricamento isole: ${exception.message}"
                            )
                        }
                    }
                    .collect { islands ->
                        if (!currentCoroutineContext().isActive) {
                            Timber.d("Skipping facilities processing - job cancelled")
                            return@collect
                        }

                        // Enrich with statistics
                        val islandsWithStats = enrichWithStatistics(islands)

                        if (currentCoroutineContext().isActive) {
                            val currentState = _uiState.value
                            val filteredAndSorted = applyFiltersAndSort(
                                islandsWithStats,
                                currentState.searchQuery,
                                currentState.selectedFilter,
                                currentState.sortOrder
                            )

                            _uiState.value = currentState.copy(
                                islands = islandsWithStats,
                                filteredIslands = filteredAndSorted,
                                isLoading = false,
                                isRefreshing = false,
                                error = null
                            )

                            Timber.d("Loaded ${islands.size} facilities successfully")
                        }
                    }

            } catch (_: kotlinx.coroutines.CancellationException) {
                Timber.d("Islands loading cancelled")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Errore imprevisto: ${e.message}"
                    )
                }
            }
        }
    }


    fun refresh() {
        val facilityId = currentFacilityId
        if (facilityId.isBlank()) return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRefreshing = true, error = null) }

                Timber.d("Refreshing islands for facility: $facilityId")
                delay(500)

                getIslandsByFacilityUseCase(facilityId).fold(
                    onSuccess = { islands ->
                        if (!currentCoroutineContext().isActive) {
                            Timber.d("Skipping refresh processing - job cancelled")
                            return@launch
                        }

                        val islandWithStats = enrichWithStatistics(islands)

                        if (currentCoroutineContext().isActive) {
                            val currentState = _uiState.value
                            val filteredAndSorted = applyFiltersAndSort(
                                islandWithStats,
                                currentState.searchQuery,
                                currentState.selectedFilter,
                                currentState.sortOrder
                            )

                            _uiState.value = currentState.copy(
                                islands = islandWithStats,
                                filteredIslands = filteredAndSorted,
                                isRefreshing = false,
                                error = null
                            )

                            Timber.d("Facilities refresh completed successfully")
                        }
                    },
                    onFailure = { error ->
                        if (currentCoroutineContext().isActive) {
                            Timber.e(error, "Failed to refresh islands")
                            _uiState.value = _uiState.value.copy(
                                isRefreshing = false,
                                error = "Errore refresh: ${error.message}"
                            )
                        }
                    }
                )
//                loadIslands()

                // Il flag isRefreshing viene resettato al completamento del caricamento
                _uiState.update { it.copy(isRefreshing = false) }

            } catch (_: CancellationException) {
                Timber.d("Refresh cancelled")
                if (currentCoroutineContext().isActive) {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e, "Failed to refresh islands")
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = "Errore refresh: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteIsland(islandId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingIsland = islandId)

            try {
                Timber.d("Deleting island: $islandId")

                deleteIslandUseCase(islandId).fold(
                    onSuccess = {
                        Timber.d("Island deleted successfully")
                        refresh()
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to delete island")
                        _uiState.value = _uiState.value.copy(
                            isDeletingIsland = null,
                            error = "Errore eliminazione isola: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception deleting island")
                _uiState.value = _uiState.value.copy(
                    error = "Errore eliminazione isola: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isDeletingIsland = null)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        val currentState = _uiState.value

        if (query.length >= 3) {
            performSearch(query)
        } else {
            val filteredAndSorted = applyFiltersAndSort(
                currentState.islands,
                query,
                currentState.selectedFilter,
                currentState.sortOrder
            )

            _uiState.value = currentState.copy(
                searchQuery = query,
                filteredIslands = filteredAndSorted
            )
        }
    }

    fun updateFilter(filter: IslandFilter) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.islands,
            currentState.searchQuery,
            filter,
            currentState.sortOrder
        )

        _uiState.value = currentState.copy(
            selectedFilter = filter,
            filteredIslands = filteredAndSorted
        )
    }

    fun updateSortOrder(sortOrder: IslandSortOrder) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.islands,
            currentState.searchQuery,
            currentState.selectedFilter,
            sortOrder
        )

        _uiState.value = currentState.copy(
            sortOrder = sortOrder,
            filteredIslands = filteredAndSorted
        )
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

    /**
     * Dismisses current error
     */
    fun dismissError() {
        _uiState.update { it.copy(error = null) }
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

    private suspend fun enrichWithStatistics(islands: List<Island>): List<IslandWithStats> {
        return islands.map { island ->
            val stats = try {
                val islandWithUnits = getIslandWithUnitsUseCase(island.id).getOrNull()
                IslandStatistics(
                    unitsCount = islandWithUnits?.units?.size ?: 0,
                    activeUnitsCount = islandWithUnits?.units?.count { it.isActive } ?: 0
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception getting stats for island ${island.id}")
                createEmptyStats()
            }

            IslandWithStats(island = island, stats = stats)
        }
    }
    private fun createEmptyStats() = IslandStatistics(
        unitsCount = 0,
        activeUnitsCount = 0
    )

    private fun performSearch(query: String) {
        val currentState = _uiState.value
        val filtered = currentState.islands.filter { islandWithStats ->
            val island = islandWithStats.island
            island.islandType.displayName.contains(query, ignoreCase = true) ||
                    island.customName?.contains(query, ignoreCase = true) == true ||
                    island.notes?.contains(query, ignoreCase = true) == true
        }

        val filteredAndSorted = applyFiltersAndSort(
            filtered,
            query,
            currentState.selectedFilter,
            currentState.sortOrder
        )

        _uiState.value = currentState.copy(
            searchQuery = query,
            filteredIslands = filteredAndSorted
        )
    }

    private fun applyFiltersAndSort(
        islands: List<IslandWithStats>,
        searchQuery: String,
        filter: IslandFilter,
        sortOrder: IslandSortOrder
    ): List<IslandWithStats> {
        var filtered = islands

        // Applica filtro di ricerca
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.lowercase()
            filtered = filtered.filter {
                it.island.serialNumber.lowercase().contains(query) ||
                        it.island.customName?.lowercase()?.contains(query) == true ||
                        it.island.model?.lowercase()?.contains(query) == true ||
                        it.island.location?.lowercase()?.contains(query) == true ||
                        it.island.islandType.displayName.lowercase().contains(query)
            }
        }

        // Applica filtro categoria
        filtered = when (filter) {
            IslandFilter.ALL -> filtered
            IslandFilter.ACTIVE -> filtered.filter { it.island.isActive }
            IslandFilter.INACTIVE -> filtered.filter { !it.island.isActive }
            IslandFilter.MAINTENANCE_DUE -> filtered.filter { it.island.needsMaintenance() }
            IslandFilter.UNDER_WARRANTY -> filtered.filter { it.island.isUnderWarranty() }
            IslandFilter.HIGH_OPERATING_HOURS -> filtered.filter { it.island.operatingHours > 5000 }
            IslandFilter.BY_TYPE -> filtered // Gestito separatamente nei dropdown menu
        }

        // Applica ordinamento
        filtered = when (sortOrder) {
            IslandSortOrder.SERIAL_NUMBER -> filtered.sortedBy { it.island.serialNumber.lowercase() }
            IslandSortOrder.TYPE -> filtered.sortedBy { it.island.islandType.name }
            IslandSortOrder.STATUS -> filtered.sortedBy { it.island.islandOperationalStatus.ordinal }
            IslandSortOrder.OPERATING_HOURS -> filtered.sortedByDescending { it.island.operatingHours }
            IslandSortOrder.MAINTENANCE_DATE -> filtered.sortedBy {
                it.island.nextScheduledMaintenance ?: Instant.DISTANT_FUTURE
            }

            IslandSortOrder.CREATED_RECENT -> filtered.sortedByDescending { it.island.createdAt }
            IslandSortOrder.CUSTOM_NAME -> filtered.sortedBy {
                it.island.customName?.lowercase() ?: it.island.serialNumber.lowercase()
            }
        }

        return filtered
    }
}

/**
 * Eventi della UI
 */
sealed class FacilityIslandListEvent {
    data class SearchQueryChanged(val query: String) : FacilityIslandListEvent()
    data class FilterChanged(val filter: IslandFilter) : FacilityIslandListEvent()
    data class SortOrderChanged(val sortOrder: IslandSortOrder) : FacilityIslandListEvent()
    data class DeleteIsland(val islandId: String) : FacilityIslandListEvent()
    object Refresh : FacilityIslandListEvent()
    object DismissError : FacilityIslandListEvent()
}