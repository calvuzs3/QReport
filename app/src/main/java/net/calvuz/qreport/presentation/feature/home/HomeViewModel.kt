package net.calvuz.qreport.presentation.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.calvuz.qreport.domain.model.*
import net.calvuz.qreport.domain.model.checkup.CheckUp
import net.calvuz.qreport.domain.model.checkup.CheckUpHeader
import net.calvuz.qreport.domain.model.checkup.CheckUpStatus
import net.calvuz.qreport.domain.model.island.IslandInfo
import net.calvuz.qreport.domain.model.island.IslandType
import net.calvuz.qreport.domain.model.settings.TechnicianInfo
import net.calvuz.qreport.domain.usecase.checkup.*
import timber.log.Timber
import javax.inject.Inject

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
    private val getCheckUpsByStatusUseCase: GetCheckUpsByStatusUseCase,
    private val createCheckUpUseCase: CreateCheckUpUseCase,
    private val getCheckUpStatsUseCase: GetCheckUpStatsUseCase
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
     * Quick action: Crea nuovo check-up
     */
    fun createQuickCheckUp(islandType: IslandType, clientName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingCheckUp = true)

            try {
                // Header basic per quick creation
                val quickHeader = createQuickHeader(clientName)

                val result = createCheckUpUseCase(
                    header = quickHeader,
                    islandType = islandType,
                    includeTemplateItems = true
                )

                result.fold(
                    onSuccess = { checkUpId ->
                        Timber.d("Quick check-up created: $checkUpId")
                        _uiState.value = _uiState.value.copy(
                            isCreatingCheckUp = false,
                            quickCreatedCheckUpId = checkUpId,
                            showQuickCreateSuccess = true
                        )
                        // Refresh data to show new check-up
                        loadDashboardData()
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to create quick check-up")
                        _uiState.value = _uiState.value.copy(
                            isCreatingCheckUp = false,
                            error = "Errore creazione check-up: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception during quick check-up creation")
                _uiState.value = _uiState.value.copy(
                    isCreatingCheckUp = false,
                    error = "Errore imprevisto: ${e.message}"
                )
            }
        }
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
                    DashboardData(
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

                    val stats = DashboardStatistics(
                        totalCheckUps = totalCheckUps,
                        activeCheckUps = activeCheckUps,
                        completedThisWeek = completedThisWeek,
                        averageCompletionTime = 0 // TODO: Calculate from real data
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        dashboardStats = stats,
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

    private suspend fun loadRecentCheckUps(): Flow<List<CheckUp>> {
        return getCheckUpsUseCase()
            .map { checkUps -> checkUps.take(5) } // Most recent 5
            .catch { e ->
                Timber.e(e, "Failed to load recent check-ups")
                emit(emptyList())
            }
    }

    private suspend fun loadInProgressCheckUps(): Flow<List<CheckUp>> {
        return getCheckUpsUseCase(status = CheckUpStatus.IN_PROGRESS)
            .catch { e ->
                Timber.e(e, "Failed to load in-progress check-ups")
                emit(emptyList())
            }
    }

    private suspend fun loadDraftCheckUps(): Flow<List<CheckUp>> {
        return getCheckUpsUseCase(status = CheckUpStatus.DRAFT)
            .catch { e ->
                Timber.e(e, "Failed to load draft check-ups")
                emit(emptyList())
            }
    }

    private suspend fun loadCompletedCheckUps(): Flow<List<CheckUp>> {
        return getCheckUpsUseCase(status = CheckUpStatus.COMPLETED)
            .catch { e ->
                Timber.e(e, "Failed to load completed check-ups")
                emit(emptyList())
            }
    }

    /**
     * Crea header rapido per quick actions
     */
    private fun createQuickHeader(clientName: String): CheckUpHeader {
        return CheckUpHeader(
            clientInfo = ClientInfo(
                companyName = clientName,
                contactPerson = "",
                site = "",
                address = "",
                phone = "",
                email = ""
            ),
            islandInfo = IslandInfo(
                serialNumber = "",
                model = "",
                installationDate = "",
                lastMaintenanceDate = "",
                operatingHours = 0,
                cycleCount = 0L
            ),
            technicianInfo = TechnicianInfo(
                name = "",
                company = "QReport",
                certification = "",
                phone = "",
                email = ""
            ),
            checkUpDate = Clock.System.now(),
            notes = "Check-up creato tramite quick action"
        )
    }
}
