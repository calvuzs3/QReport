package net.calvuz.qreport.app.app.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.app.app.presentation.ui.home.model.DashboardCheckupData
import net.calvuz.qreport.app.app.presentation.ui.home.model.DashboardCheckupStatistics
import net.calvuz.qreport.checkup.domain.model.CheckUp
import net.calvuz.qreport.checkup.domain.model.CheckUpStatus
import net.calvuz.qreport.checkup.domain.usecase.CreateCheckUpUseCase
import net.calvuz.qreport.checkup.domain.usecase.GetCheckUpsUseCase
import timber.log.Timber
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val isCreatingCheckUp: Boolean = false,
    val checkupStats: DashboardCheckupStatistics? = null,
    val recentCheckUps: List<CheckUp> = emptyList(),
    val inProgressCheckUps: List<CheckUp> = emptyList(),
    val selectedCheckUpId: String? = null,
    val quickCreatedCheckUpId: String? = null,
    val showQuickCreateSuccess: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel per la Home Screen
 *
 * Gestisce lo stato della dashboard principale con:
 * - Statistiche generali dell'app
 * - Check-up recenti e in corso
 * - Quick actions per nuove operazioni
 * - Navigazione rapida
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCheckUpsUseCase: GetCheckUpsUseCase,
//    private val getCheckUpsByStatusUseCase: GetCheckUpsByStatusUseCase,
    private val createCheckUpUseCase: CreateCheckUpUseCase,
//    private val getCheckUpStatsUseCase: GetCheckUpStatsUseCase
) : ViewModel() {

    // ============================================================
    // UI STATE
    // ============================================================

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // ============================================================
    // INIT
    // ============================================================

    init {
        Timber.d("HomeViewModel initialized")
        loadDashboardData()
    }

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    /**
     * Refresh completo dei dati dashboard
     */
    fun refresh() {
        Timber.d("Refreshing dashboard data")
        loadDashboardData()
    }

    /**
     * Naviga ai dettagli check-up
     */
    fun navigateToCheckUp(checkUpId: String) {
        Timber.d("Navigate to check-up: $checkUpId")
        _uiState.value = _uiState.value.copy(
            selectedCheckUpId = checkUpId
        )
    }

    /**
     * Dismisses error message
     */
    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Dismisses quick create success message
     */
    fun dismissQuickCreateSuccess() {
        _uiState.value = _uiState.value.copy(
            showQuickCreateSuccess = false,
            quickCreatedCheckUpId = null
        )
    }

    fun clearSelectedCheckUp() {
        _uiState.value = _uiState.value.copy(selectedCheckUpId = null)
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    /**
     * Carica tutti i dati per la dashboard
     */
    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Collect multiple flows and combine them
                combine(
                    loadRecentCheckUps(),
                    loadInProgressCheckUps(),
                    loadDraftCheckUps(),
                    loadCompletedCheckUps()
                ) { recent, inProgress, drafts, completed ->
                    DashboardCheckupData(
                        recentCheckUps = recent,
                        inProgressCheckUps = inProgress,
                        draftCheckUps = drafts,
                        completedCheckUps = completed
                    )
                }.collect { dashboardData ->

                    // Calculate statistics
                    val totalCheckUps = dashboardData.recentCheckUps.size
                    val activeCheckUps = dashboardData.inProgressCheckUps.size + dashboardData.draftCheckUps.size
                    val completedThisWeek = dashboardData.completedCheckUps.take(10).size // Approximation

                    val stats = DashboardCheckupStatistics(
                        totalCheckUps = totalCheckUps,
                        activeCheckUps = activeCheckUps,
                        completedThisWeek = completedThisWeek,
                        averageCompletionTime = 0 // TODO: Calculate from real data
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        checkupStats = stats,
                        recentCheckUps = dashboardData.recentCheckUps,
                        inProgressCheckUps = dashboardData.inProgressCheckUps,
                        error = null
                    )
                }

            } catch (e: Exception) {
                Timber.e(e, "Failed to load dashboard data")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore caricamento dati: ${e.message}"
                )
            }
        }
    }

    private fun loadRecentCheckUps(): Flow<List<CheckUp>> {
        return getCheckUpsUseCase()
            .map { checkUps -> checkUps.take(5) } // Most recent 5
            .catch { e ->
                Timber.e(e, "Failed to load recent check-ups")
                emit(emptyList())
            }
    }

    private fun loadInProgressCheckUps(): Flow<List<CheckUp>> {
        return getCheckUpsUseCase(status = CheckUpStatus.IN_PROGRESS)
            .catch { e ->
                Timber.e(e, "Failed to load in-progress check-ups")
                emit(emptyList())
            }
    }

    private fun loadDraftCheckUps(): Flow<List<CheckUp>> {
        return getCheckUpsUseCase(status = CheckUpStatus.DRAFT)
            .catch { e ->
                Timber.e(e, "Failed to load draft check-ups")
                emit(emptyList())
            }
    }

    private fun loadCompletedCheckUps(): Flow<List<CheckUp>> {
        return getCheckUpsUseCase(status = CheckUpStatus.COMPLETED)
            .catch { e ->
                Timber.e(e, "Failed to load completed check-ups")
                emit(emptyList())
            }
    }
}
