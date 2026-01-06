package net.calvuz.qreport.client.facility.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.usecase.GetFacilitiesByClientUseCase
import net.calvuz.qreport.client.facility.domain.usecase.DeleteFacilityUseCase
import net.calvuz.qreport.client.facility.domain.usecase.GetFacilityWithIslandsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel per FacilityListScreen - seguendo pattern ClientListViewModel
 *
 * Features:
 * - Gestisce lista stabilimenti per cliente
 * - Ricerca e filtri per facility type/status
 * - Statistics con conteggio isole
 * - Pull to refresh
 * - Error handling ottimizzato
 */

data class FacilityListUiState(
    val facilities: List<FacilityWithStats> = emptyList(),
    val filteredFacilities: List<FacilityWithStats> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDeletingFacility: String? = null,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedFilter: FacilityFilter = FacilityFilter.ALL,
    val sortOrder: FacilitySortOrder = FacilitySortOrder.NAME,
    val clientId: String = ""
)

enum class FacilityFilter {
    ALL, ACTIVE, INACTIVE, PRIMARY_ONLY, WITH_ISLANDS, BY_TYPE
}

enum class FacilitySortOrder {
    NAME, CREATED_RECENT, CREATED_OLDEST, ISLANDS_COUNT, TYPE
}

@HiltViewModel
class FacilityListViewModel @Inject constructor(
    private val getFacilitiesByClientUseCase: GetFacilitiesByClientUseCase,
    private val deleteFacilityUseCase: DeleteFacilityUseCase,
    private val getFacilityWithIslandsUseCase: GetFacilityWithIslandsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacilityListUiState())
    val uiState: StateFlow<FacilityListUiState> = _uiState.asStateFlow()

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    fun initializeForClient(clientId: String) {
        if (clientId == _uiState.value.clientId) return // Già inizializzato per questo cliente

        _uiState.value = _uiState.value.copy(clientId = clientId)
        loadFacilities()
    }

    fun loadFacilities() {
        val clientId = _uiState.value.clientId
        if (clientId.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                Timber.d("Loading facilities for client: $clientId")

                getFacilitiesByClientUseCase.observeFacilitiesByClient(clientId)
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        Timber.e(exception, "Error in facilities flow")
                        if (currentCoroutineContext().isActive) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = "Errore caricamento stabilimenti: ${exception.message}"
                            )
                        }
                    }
                    .collect { facilities ->
                        if (!currentCoroutineContext().isActive) {
                            Timber.d("Skipping facilities processing - job cancelled")
                            return@collect
                        }

                        // Enrich with statistics
                        val facilitiesWithStats = enrichWithStatistics(facilities)

                        if (currentCoroutineContext().isActive) {
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

                            Timber.d("Loaded ${facilities.size} facilities successfully")
                        }
                    }

            } catch (_: CancellationException) {
                Timber.d("Facilities loading cancelled")
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e, "Failed to load facilities")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = "Errore caricamento stabilimenti: ${e.message}"
                    )
                }
            }
        }
    }

    fun refresh() {
        val clientId = _uiState.value.clientId
        if (clientId.isEmpty()) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

                Timber.d("Refreshing facilities for client: $clientId")

                getFacilitiesByClientUseCase(clientId).fold(
                    onSuccess = { facilities ->
                        if (!currentCoroutineContext().isActive) {
                            Timber.d("Skipping refresh processing - job cancelled")
                            return@launch
                        }

                        val facilitiesWithStats = enrichWithStatistics(facilities)

                        if (currentCoroutineContext().isActive) {
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
                                isRefreshing = false,
                                error = null
                            )

                            Timber.d("Facilities refresh completed successfully")
                        }
                    },
                    onFailure = { error ->
                        if (currentCoroutineContext().isActive) {
                            Timber.e(error, "Failed to refresh facilities")
                            _uiState.value = _uiState.value.copy(
                                isRefreshing = false,
                                error = "Errore refresh: ${error.message}"
                            )
                        }
                    }
                )

            } catch (_: CancellationException) {
                Timber.d("Refresh cancelled")
                if (currentCoroutineContext().isActive) {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e, "Failed to refresh facilities")
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = "Errore refresh: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteFacility(facilityId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingFacility = facilityId)

            try {
                Timber.d("Deleting facility: $facilityId")

                deleteFacilityUseCase(facilityId).fold(
                    onSuccess = {
                        Timber.d("Facility deleted successfully")
                        // La lista si aggiorna automaticamente tramite Flow
                        refresh()
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

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    private suspend fun enrichWithStatistics(facilities: List<Facility>): List<FacilityWithStats> {
        return facilities.map { facility ->
            val stats = try {
                val facilityWithIslands = getFacilityWithIslandsUseCase(facility.id).getOrNull()
                FacilityStatistics(
                    islandsCount = facilityWithIslands?.islands?.size ?: 0,
                    activeIslandsCount = facilityWithIslands?.islands?.count { it.isActive } ?: 0,
                    maintenanceDueCount = facilityWithIslands?.islands?.count { it.needsMaintenance() } ?: 0
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
                    facility.description?.contains(query, ignoreCase = true) == true ||
                    facility.facilityType.displayName.contains(query, ignoreCase = true) ||
                    facility.address.city?.contains(query, ignoreCase = true) == true
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
}

/**
 * Data class per facility con statistiche
 */
data class FacilityWithStats(
    val facility: Facility,
    val stats: FacilityStatistics
) {
    val formattedLastModified: String
        get() {
            val now = Clock.System.now()
            val updated = facility.updatedAt
            val diffMillis = (now - updated).inWholeMilliseconds

            return when {
                diffMillis < 60000 -> "Aggiornato ora"
                diffMillis < 3600000 -> "Aggiornato ${diffMillis / 60000} min fa"
                diffMillis < 86400000 -> "Aggiornato ${diffMillis / 3600000}h fa"
                else -> "Aggiornato ${diffMillis / 86400000} giorni fa"
            }
        }
}

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