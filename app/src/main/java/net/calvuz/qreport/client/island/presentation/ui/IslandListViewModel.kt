package net.calvuz.qreport.client.island.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.usecase.DeleteIslandUseCase
import net.calvuz.qreport.client.island.domain.usecase.GetIslandsByFacilityUseCase
import net.calvuz.qreport.client.island.domain.usecase.SearchIslandsUseCase
import net.calvuz.qreport.client.island.domain.usecase.FacilityOperationalSummary
import javax.inject.Inject

/**
 * ViewModel per FacilityIslandListScreen
 *
 * Gestisce:
 * - Lista isole per facility con ricerca e filtri
 * - Statistiche aggregate
 * - Sort per tipo, serial number, stato
 * - Eliminazione isole con conferma
 * - Refresh automatico
 */
@HiltViewModel
class IslandListViewModel @Inject constructor(
    private val getIslandsByFacilityUseCase: GetIslandsByFacilityUseCase,
    private val searchIslandsUseCase: SearchIslandsUseCase,
    private val deleteIslandUseCase: DeleteIslandUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacilityIslandListUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var currentFacilityId: String = ""

    /**
     * Inizializza per una facility specifica
     */
    fun initializeForFacility(facilityId: String) {
        if (facilityId != currentFacilityId) {
            currentFacilityId = facilityId
            loadIslands()
        }
    }

    /**
     * Carica isole della facility
     */
    fun loadIslands() {
        if (currentFacilityId.isBlank()) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Carica solo isole, statistiche semplificate
                val islandsResult = getIslandsByFacilityUseCase(currentFacilityId)

                islandsResult.fold(
                    onSuccess = { islands ->
                        // Calcola statistiche semplici localmente
                        val stats = calculateSimpleStats(islands)

                        _uiState.update { currentState ->
                            val filteredIslands = applyFiltersAndSort(
                                islands = islands,
                                searchQuery = currentState.searchQuery,
                                filter = currentState.selectedFilter,
                                sortOrder = currentState.sortOrder
                            )

                            currentState.copy(
                                isLoading = false,
                                allIslands = islands,
                                filteredIslands = filteredIslands,
                                statistics = stats,
                                error = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Errore nel caricamento isole"
                            )
                        }
                    }
                )
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

    /**
     * Calcola statistiche semplici localmente
     */
    private fun calculateSimpleStats(islands: List<Island>): FacilityOperationalSummary {
        val currentTime = Clock.System.now()

        return FacilityOperationalSummary(
            facilityId = currentFacilityId,
            totalIslands = islands.size,
            activeIslands = islands.count { it.isActive },
            islandsByType = islands.groupBy { it.islandType }.mapValues { it.value.size },
            totalOperatingHours = islands.sumOf { it.operatingHours },
            totalCycles = islands.sumOf { it.cycleCount },
            islandsUnderWarranty = islands.count { island ->
                island.warrantyExpiration?.let { it > currentTime } == true
            },
            islandsDueMaintenance = islands.count { island ->
                island.needsMaintenance()
            },
            averageOperatingHours = if (islands.isNotEmpty()) {
                islands.map { it.operatingHours }.average().toInt()
            } else 0
        )
    }

    /**
     * Aggiorna query di ricerca
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { currentState ->
            val filteredIslands = applyFiltersAndSort(
                islands = currentState.allIslands,
                searchQuery = query,
                filter = currentState.selectedFilter,
                sortOrder = currentState.sortOrder
            )

            currentState.copy(
                searchQuery = query,
                filteredIslands = filteredIslands
            )
        }

        // Se la query non è vuota, cerca globalmente per suggerimenti
        if (query.length >= 3) {
            performGlobalSearch(query)
        }
    }

    /**
     * Ricerca globale per suggerimenti
     */
    private fun performGlobalSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            searchIslandsUseCase(query).fold(
                onSuccess = { globalResults ->
                    // Filtra solo le isole di altre facility per suggerimenti
                    val suggestions = globalResults.filter { it.facilityId != currentFacilityId }
                    _uiState.update {
                        it.copy(searchSuggestions = suggestions.take(3)) // Max 3 suggerimenti
                    }
                },
                onFailure = {
                    // Ignora errori di ricerca globale
                    _uiState.update { it.copy(searchSuggestions = emptyList()) }
                }
            )
        }
    }

    /**
     * Aggiorna filtro
     */
    fun updateFilter(filter: IslandFilter) {
        _uiState.update { currentState ->
            val filteredIslands = applyFiltersAndSort(
                islands = currentState.allIslands,
                searchQuery = currentState.searchQuery,
                filter = filter,
                sortOrder = currentState.sortOrder
            )

            currentState.copy(
                selectedFilter = filter,
                filteredIslands = filteredIslands
            )
        }
    }

    /**
     * Aggiorna ordinamento
     */
    fun updateSortOrder(sortOrder: IslandSortOrder) {
        _uiState.update { currentState ->
            val filteredIslands = applyFiltersAndSort(
                islands = currentState.allIslands,
                searchQuery = currentState.searchQuery,
                filter = currentState.selectedFilter,
                sortOrder = sortOrder
            )

            currentState.copy(
                sortOrder = sortOrder,
                filteredIslands = filteredIslands
            )
        }
    }

    /**
     * Elimina isola con conferma
     */
    fun deleteIsland(islandId: String) {
        viewModelScope.launch {
            deleteIslandUseCase(islandId).fold(
                onSuccess = {
                    // Ricarica lista dopo eliminazione
                    loadIslands()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = "Errore eliminazione: ${error.message}")
                    }
                }
            )
        }
    }

    /**
     * Refresh con pull to refresh
     */
    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadIslands()
        // Il flag isRefreshing viene resettato al completamento del caricamento
        _uiState.update { it.copy(isRefreshing = false) }
    }

    /**
     * Dismisses current error
     */
    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Applica filtri e ordinamento
     */
    private fun applyFiltersAndSort(
        islands: List<Island>,
        searchQuery: String,
        filter: IslandFilter,
        sortOrder: IslandSortOrder
    ): List<Island> {
        var result = islands

        // Applica filtro di ricerca
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.lowercase()
            result = result.filter { island ->
                island.serialNumber.lowercase().contains(query) ||
                        island.customName?.lowercase()?.contains(query) == true ||
                        island.model?.lowercase()?.contains(query) == true ||
                        island.location?.lowercase()?.contains(query) == true ||
                        island.islandType.displayName.lowercase().contains(query)
            }
        }

        // Applica filtro categoria
        result = when (filter) {
            IslandFilter.ALL -> result
            IslandFilter.ACTIVE -> result.filter { it.isActive }
            IslandFilter.INACTIVE -> result.filter { !it.isActive }
            IslandFilter.MAINTENANCE_DUE -> result.filter { it.needsMaintenance() }
            IslandFilter.UNDER_WARRANTY -> result.filter { it.isUnderWarranty() }
            IslandFilter.HIGH_OPERATING_HOURS -> result.filter { it.operatingHours > 5000 }
            IslandFilter.BY_TYPE -> result // Gestito separatamente nei dropdown menu
        }

        // Applica ordinamento
        result = when (sortOrder) {
            IslandSortOrder.SERIAL_NUMBER -> result.sortedBy { it.serialNumber.lowercase() }
            IslandSortOrder.TYPE -> result.sortedBy { it.islandType.name }
            IslandSortOrder.STATUS -> result.sortedBy { it.islandOperationalStatus.ordinal }
            IslandSortOrder.OPERATING_HOURS -> result.sortedByDescending { it.operatingHours }
            IslandSortOrder.MAINTENANCE_DATE -> result.sortedBy {
                it.nextScheduledMaintenance ?: Instant.DISTANT_FUTURE
            }
            IslandSortOrder.CREATED_RECENT -> result.sortedByDescending { it.createdAt }
            IslandSortOrder.CUSTOM_NAME -> result.sortedBy {
                it.customName?.lowercase() ?: it.serialNumber.lowercase()
            }
        }

        return result
    }
}

/**
 * UI State per lista isole
 */
data class FacilityIslandListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val allIslands: List<Island> = emptyList(),
    val filteredIslands: List<Island> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: IslandFilter = IslandFilter.ALL,
    val sortOrder: IslandSortOrder = IslandSortOrder.SERIAL_NUMBER,
    val statistics: FacilityOperationalSummary? = null,
    val searchSuggestions: List<Island> = emptyList(),
    val error: String? = null
)

/**
 * Filtri per isole
 */
enum class IslandFilter {
    ALL,
    ACTIVE,
    INACTIVE,
    MAINTENANCE_DUE,
    UNDER_WARRANTY,
    HIGH_OPERATING_HOURS,
    BY_TYPE
}

/**
 * Ordinamenti per isole
 */
enum class IslandSortOrder {
    SERIAL_NUMBER,
    TYPE,
    STATUS,
    OPERATING_HOURS,
    MAINTENANCE_DATE,
    CREATED_RECENT,
    CUSTOM_NAME
}

// IslandStatistics rimosso - ora usiamo FacilityOperationalSummary per la facility

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