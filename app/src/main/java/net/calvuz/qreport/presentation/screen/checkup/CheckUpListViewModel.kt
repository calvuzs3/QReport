package net.calvuz.qreport.presentation.screen.checkup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.calvuz.qreport.domain.model.*
import net.calvuz.qreport.domain.usecase.checkup.*
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel per CheckUpListScreen
 *
 * Gestisce:
 * - Lista check-up con filtri
 * - Ricerca e ordinamento
 * - Gestione stati
 * - Pull-to-refresh
 */

data class CheckUpListUiState(
    val checkUps: List<CheckUpWithStats> = emptyList(),
    val filteredCheckUps: List<CheckUpWithStats> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedFilter: CheckUpFilter = CheckUpFilter.ALL,
    val sortOrder: SortOrder = SortOrder.RECENT_FIRST
)

enum class CheckUpFilter {
    ALL, DRAFT, IN_PROGRESS, COMPLETED
}

enum class SortOrder {
    RECENT_FIRST, OLDEST_FIRST, CLIENT_NAME, STATUS
}

@HiltViewModel
class CheckUpListViewModel @Inject constructor(
    private val getCheckUpsUseCase: GetCheckUpsUseCase,
    private val getCheckUpStatsUseCase: GetCheckUpStatsUseCase,
    private val deleteCheckUpUseCase: DeleteCheckUpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckUpListUiState())
    val uiState: StateFlow<CheckUpListUiState> = _uiState.asStateFlow()

    init {
        Timber.d("CheckUpListViewModel initialized")
        loadCheckUps()
    }

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    fun loadCheckUps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                Timber.d("Loading check-ups list")

                getCheckUpsUseCase()
                    .catch { exception ->
                        Timber.e(exception, "Error in getCheckUpsUseCase flow")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = "Errore caricamento check-ups: ${exception.message}"
                        )
                    }
                    .collect { checkUps ->
                        // Enrich with statistics
                        val checkUpsWithStats = checkUps.map { checkUp ->
                            val stats = try {
                                getCheckUpStatsUseCase(checkUp.id).getOrElse {
                                    Timber.w("Failed to get stats for check-up ${checkUp.id}: ${it.message}")
                                    CheckUpStatistics(
                                        totalItems = 0,
                                        completedItems = 0,
                                        okItems = 0,
                                        nokItems = 0,
                                        naItems = 0,
                                        pendingItems = 0,
                                        criticalIssues = 0,
                                        importantIssues = 0,
                                        photosCount = 0,
                                        sparePartsCount = 0,
                                        completionPercentage = 0f
                                    )
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Exception getting stats for check-up ${checkUp.id}")
                                CheckUpStatistics(
                                    totalItems = 0,
                                    completedItems = 0,
                                    okItems = 0,
                                    nokItems = 0,
                                    naItems = 0,
                                    pendingItems = 0,
                                    criticalIssues = 0,
                                    importantIssues = 0,
                                    photosCount = 0,
                                    sparePartsCount = 0,
                                    completionPercentage = 0f
                                )
                            }

                            CheckUpWithStats(
                                checkUp = checkUp,
                                statistics = stats
                            )
                        }

                        val currentState = _uiState.value
                        val filteredAndSorted = applyFiltersAndSort(
                            checkUpsWithStats,
                            currentState.searchQuery,
                            currentState.selectedFilter,
                            currentState.sortOrder
                        )

                        _uiState.value = currentState.copy(
                            checkUps = checkUpsWithStats,
                            filteredCheckUps = filteredAndSorted,
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                    }

            } catch (e: Exception) {
                Timber.e(e, "Failed to load check-ups")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = "Errore caricamento check-ups: ${e.message}"
                )
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadCheckUps()
    }

    fun updateSearchQuery(query: String) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.checkUps,
            query,
            currentState.selectedFilter,
            currentState.sortOrder
        )

        _uiState.value = currentState.copy(
            searchQuery = query,
            filteredCheckUps = filteredAndSorted
        )
    }

    fun updateFilter(filter: CheckUpFilter) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.checkUps,
            currentState.searchQuery,
            filter,
            currentState.sortOrder
        )

        _uiState.value = currentState.copy(
            selectedFilter = filter,
            filteredCheckUps = filteredAndSorted
        )
    }

    fun updateSortOrder(sortOrder: SortOrder) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.checkUps,
            currentState.searchQuery,
            currentState.selectedFilter,
            sortOrder
        )

        _uiState.value = currentState.copy(
            sortOrder = sortOrder,
            filteredCheckUps = filteredAndSorted
        )
    }

    fun deleteCheckUp(checkUpId: String) {
        viewModelScope.launch {
            try {
                Timber.d("Deleting check-up: $checkUpId")

                deleteCheckUpUseCase(checkUpId).fold(
                    onSuccess = {
                        Timber.d("Check-up deleted successfully")
                        // The list will be automatically updated via Flow
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to delete check-up")
                        _uiState.value = _uiState.value.copy(
                            error = "Errore eliminazione check-up: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception deleting check-up")
                _uiState.value = _uiState.value.copy(
                    error = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    private fun applyFiltersAndSort(
        checkUps: List<CheckUpWithStats>,
        searchQuery: String,
        filter: CheckUpFilter,
        sortOrder: SortOrder
    ): List<CheckUpWithStats> {
        var filtered = checkUps

        // Apply status filter
        filtered = when (filter) {
            CheckUpFilter.ALL -> filtered
            CheckUpFilter.DRAFT -> filtered.filter { it.checkUp.status == CheckUpStatus.DRAFT }
            CheckUpFilter.IN_PROGRESS -> filtered.filter { it.checkUp.status == CheckUpStatus.IN_PROGRESS }
            CheckUpFilter.COMPLETED -> filtered.filter {
                it.checkUp.status in listOf(CheckUpStatus.COMPLETED, CheckUpStatus.EXPORTED, CheckUpStatus.ARCHIVED)
            }
        }

        // Apply search query
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { checkUpWithStats ->
                val checkUp = checkUpWithStats.checkUp
                checkUp.header.clientInfo.companyName.contains(searchQuery, ignoreCase = true) ||
                        checkUp.header.clientInfo.site.contains(searchQuery, ignoreCase = true) ||
                        checkUp.header.islandInfo.serialNumber.contains(searchQuery, ignoreCase = true) ||
                        checkUp.header.islandInfo.model.contains(searchQuery, ignoreCase = true)
            }
        }

        // Apply sorting
        filtered = when (sortOrder) {
            SortOrder.RECENT_FIRST -> filtered.sortedByDescending { it.checkUp.createdAt }
            SortOrder.OLDEST_FIRST -> filtered.sortedBy { it.checkUp.createdAt }
            SortOrder.CLIENT_NAME -> filtered.sortedBy { it.checkUp.header.clientInfo.companyName }
            SortOrder.STATUS -> filtered.sortedBy { it.checkUp.status.ordinal }
        }

        return filtered
    }
}

/**
 * Data class per check-up con statistiche
 */
data class CheckUpWithStats(
    val checkUp: CheckUp,
    val statistics: CheckUpStatistics
) {
    val progressPercentage: Int
        get() = if (statistics.totalItems > 0) {
            ((statistics.completedItems.toFloat() / statistics.totalItems) * 100).toInt()
        } else 0

    val formattedLastModified: String
        get() {
            // Format based on updatedAt
            val now = Clock.System.now()
            val updated = checkUp.updatedAt
            val diffMillis = (now - updated).inWholeMilliseconds

            return when {
                diffMillis < 60000 -> "Aggiornato ora"
                diffMillis < 3600000 -> "Aggiornato ${diffMillis / 60000} min fa"
                diffMillis < 86400000 -> "Aggiornato ${diffMillis / 3600000}h fa"
                else -> "Aggiornato ${diffMillis / 86400000} giorni fa"
            }
        }
}