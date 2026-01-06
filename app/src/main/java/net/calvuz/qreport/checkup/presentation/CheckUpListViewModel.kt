package net.calvuz.qreport.checkup.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import net.calvuz.qreport.checkup.domain.model.CheckUpSingleStatistics
import net.calvuz.qreport.checkup.domain.model.CheckUpStatus
import net.calvuz.qreport.checkup.domain.usecase.DeleteCheckUpUseCase
import net.calvuz.qreport.checkup.domain.usecase.GetCheckUpStatsUseCase
import net.calvuz.qreport.checkup.domain.usecase.GetCheckUpsUseCase
import net.calvuz.qreport.checkup.presentation.model.CheckUpFilter
import net.calvuz.qreport.checkup.presentation.model.CheckUpSortOrder
import net.calvuz.qreport.checkup.presentation.model.CheckUpWithStats
import net.calvuz.qreport.app.error.domain.model.QrError
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel per CheckUpListScreen
 */

data class CheckUpListUiState(
    val checkUps: List<CheckUpWithStats> = emptyList(),
    val filteredCheckUps: List<CheckUpWithStats> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: QrError.Checkup? = null,
    val searchQuery: String = "",
    val selectedFilter: CheckUpFilter = CheckUpFilter.ALL,
    val checkUpSortOrder: CheckUpSortOrder = CheckUpSortOrder.RECENT_FIRST
)

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
                        if (exception is CancellationException) throw exception
                        Timber.e(exception, "Error in getCheckUpsUseCase flow")
                        if (currentCoroutineContext().isActive) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error =  QrError.Checkup.LOAD // "Errore caricamento check-ups: ${exception.message}"
                            )
                        }
                    }
                    .collect { checkUps ->
                        // Check deleting before processing
                        if (!currentCoroutineContext().isActive) {
                            Timber.d("Skipping check-ups processing - job cancelled")
                            return@collect
                        }

                        // Enrich with statistics
                        val checkUpsWithStats = checkUps.map { checkUp ->
                            val stats = try {
                                getCheckUpStatsUseCase(checkUp.id).getOrElse {
                                    Timber.w("Failed to get stats for check-up ${checkUp.id}: ${it.message}")
                                    CheckUpSingleStatistics() // Everything to 0
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Exception getting stats for check-up ${checkUp.id}")
                                CheckUpSingleStatistics() // Everything to 0
                            }

                            CheckUpWithStats(
                                checkUp = checkUp,
                                statistics = stats
                            )
                        }

                        if (currentCoroutineContext().isActive) {
                            val currentState = _uiState.value
                            val filteredAndSorted = applyFiltersAndSort(
                                checkUpsWithStats,
                                currentState.searchQuery,
                                currentState.selectedFilter,
                                currentState.checkUpSortOrder
                            )

                            _uiState.value = currentState.copy(
                                checkUps = checkUpsWithStats,
                                filteredCheckUps = filteredAndSorted,
                                isLoading = false,
                                isRefreshing = false,
                                error = null
                            )
                        } else {
                            Timber.d("Skipping UI update - job cancelled")
                        }
                    }

            } catch (_: CancellationException) {
                Timber.d("Check-ups loading cancelled (normal during navigation)")
                // Non aggiornare UI se cancellato

            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e, "Failed to load check-ups")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = QrError.Checkup.LOAD // "Errore caricamento check-ups: ${e.message}"
                    )
                } else {
                    Timber.d("Error handling skipped - job cancelled")
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

                Timber.d("Refreshing check-ups list")

                // Use .first() for one-shot operation instead of .collect
                val checkUps = getCheckUpsUseCase().first()

                if (!currentCoroutineContext().isActive) {
                    Timber.d("Skipping refresh processing - job cancelled")
                    return@launch
                }

                // Enrich with statistics
                val checkUpsWithStats = checkUps.map { checkUp ->
                    val stats = try {
                        getCheckUpStatsUseCase(checkUp.id).getOrElse {
                            Timber.w("Failed to get stats for check-up ${checkUp.id}: ${it.message}")
                            CheckUpSingleStatistics( )
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Exception getting stats for check-up ${checkUp.id}")
                        CheckUpSingleStatistics()
                    }

                    CheckUpWithStats(
                        checkUp = checkUp,
                        statistics = stats
                    )
                }

                if (currentCoroutineContext().isActive) {
                    val currentState = _uiState.value
                    val filteredAndSorted = applyFiltersAndSort(
                        checkUpsWithStats,
                        currentState.searchQuery,
                        currentState.selectedFilter,
                        currentState.checkUpSortOrder
                    )

                    _uiState.value = currentState.copy(
                        checkUps = checkUpsWithStats,
                        filteredCheckUps = filteredAndSorted,
                        isRefreshing = false,  // always reset
                        error = null
                    )

                    Timber.d("Refresh completed successfully")
                } else {
                    Timber.d("Skipping refresh UI update - job cancelled")
                }

            } catch (_: CancellationException) {
                Timber.d("Refresh cancelled")
                // Reset isRefreshing state even if cancelled
                if (currentCoroutineContext().isActive) {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e, "Failed to refresh check-ups")
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = QrError.Checkup.REFRESH // "Errore refresh: ${e.message}"
                    )
                } else {
                    Timber.d("Refresh error handling skipped - job cancelled")
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.checkUps,
            query,
            currentState.selectedFilter,
            currentState.checkUpSortOrder
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
            currentState.checkUpSortOrder
        )

        _uiState.value = currentState.copy(
            selectedFilter = filter,
            filteredCheckUps = filteredAndSorted
        )
    }

    fun updateSortOrder(checkUpSortOrder: CheckUpSortOrder) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.checkUps,
            currentState.searchQuery,
            currentState.selectedFilter,
            checkUpSortOrder
        )

        _uiState.value = currentState.copy(
            checkUpSortOrder = checkUpSortOrder,
            filteredCheckUps = filteredAndSorted
        )
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
        checkUpSortOrder: CheckUpSortOrder
    ): List<CheckUpWithStats> {
        var filtered = checkUps

        // Apply status filter
        filtered = when (filter) {
            CheckUpFilter.ALL -> filtered
            CheckUpFilter.DRAFT -> filtered.filter { it.checkUp.status == CheckUpStatus.DRAFT }
            CheckUpFilter.IN_PROGRESS -> filtered.filter { it.checkUp.status == CheckUpStatus.IN_PROGRESS }
            CheckUpFilter.COMPLETED -> filtered.filter { it.checkUp.status == CheckUpStatus.COMPLETED }
            CheckUpFilter.EXPORTED -> filtered.filter { it.checkUp.status == CheckUpStatus.EXPORTED }
            CheckUpFilter.ARCHIVED -> filtered.filter { it.checkUp.status == CheckUpStatus.ARCHIVED }
        }

        // Apply search query
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { checkUpWithStats ->
                val checkUp = checkUpWithStats.checkUp
                checkUp.header.clientInfo.companyName.contains(searchQuery, ignoreCase = true) ||
                        checkUp.header.clientInfo.site.contains(searchQuery, ignoreCase = true) ||
                        checkUp.header.islandInfo.serialNumber.contains(
                            searchQuery,
                            ignoreCase = true
                        ) ||
                        checkUp.header.islandInfo.model.contains(searchQuery, ignoreCase = true)
            }
        }

        // Apply sorting
        filtered = when (checkUpSortOrder) {
            CheckUpSortOrder.RECENT_FIRST -> filtered.sortedByDescending { it.checkUp.createdAt }
            CheckUpSortOrder.OLDEST_FIRST -> filtered.sortedBy { it.checkUp.createdAt }
            CheckUpSortOrder.CLIENT_NAME -> filtered.sortedBy { it.checkUp.header.clientInfo.companyName }
            CheckUpSortOrder.STATUS -> filtered.sortedBy { it.checkUp.status.ordinal }
        }

        return filtered
    }
}