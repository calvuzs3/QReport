package net.calvuz.qreport.client.island.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.usecase.GetIslandByIdUseCase
import net.calvuz.qreport.client.island.domain.usecase.GetIslandStatisticsUseCase
import net.calvuz.qreport.client.island.domain.usecase.UpdateMaintenanceUseCase
import net.calvuz.qreport.client.island.domain.usecase.DeleteIslandUseCase
import net.calvuz.qreport.client.island.domain.usecase.SingleIslandStatistics
import kotlinx.datetime.Instant
import net.calvuz.qreport.client.island.domain.usecase.IslandDeletionInfo
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import net.calvuz.qreport.client.unit.domain.usecase.GetMechanicalUnitsByIslandUseCase
import timber.log.Timber
import javax.inject.Inject

// =============================================================================
// TAB ENUM
// =============================================================================

/**
 * Tabs available in the island detail screen.
 * Mirrors [FacilityDetailTab] with UNITS replacing ISLANDS.
 */
enum class IslandDetailTab(val title: String) {
    UNITS("Unità"),
    MAINTENANCE("Manutenzione"),
    INFO("Informazioni"),
}

// =============================================================================
// UI STATE
// =============================================================================

data class FacilityIslandDetailUiState(
    val isLoading: Boolean = false,
    val hasData: Boolean = false,
    val island: Island? = null,
    val statistics: SingleIslandStatistics? = null,

    // Tab navigation
    val selectedTab: IslandDetailTab = IslandDetailTab.UNITS,

    // Mechanical units
    val units: List<MechanicalUnit> = emptyList(),
    val isLoadingUnits: Boolean = false,

    // Delete states (island)
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val deleteError: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val islandDeleted: Boolean = false,

    // Operation states
    val isUpdatingMaintenance: Boolean = false,

    val deletionInfo: IslandDeletionInfo? = null,

    // Errors
    val error: String? = null
) {
    val islandName: String
        get() = island?.displayName ?: "Isola"

    val hasOperationsInProgress: Boolean
        get() = isUpdatingMaintenance || isDeleting

    val needsAttention: Boolean
        get() = statistics?.needsAttention == true || island?.needsMaintenance() == true

    val statusText: String
        get() = statistics?.statusDescription
            ?: island?.islandOperationalStatus?.displayName ?: ""

    // Badge counts for tabs
    val unitsCount: Int
        get() = units.size
}

// =============================================================================
// VIEWMODEL
// =============================================================================

@HiltViewModel
class IslandDetailViewModel @Inject constructor(
    private val getIslandByIdUseCase: GetIslandByIdUseCase,
    private val getStatisticsUseCase: GetIslandStatisticsUseCase,
    private val getMechanicalUnitsUseCase: GetMechanicalUnitsByIslandUseCase,
    private val updateMaintenanceUseCase: UpdateMaintenanceUseCase,
    private val deleteIslandUseCase: DeleteIslandUseCase,
    private val mechanicalUnitRepository: MechanicalUnitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacilityIslandDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var currentIslandId: String = ""

    init {
        Timber.d("IslandDetailViewModel initialized")
    }

    // ============================================================
    // TAB NAVIGATION
    // ============================================================

    fun selectTab(tab: IslandDetailTab) {
        if (_uiState.value.selectedTab != tab) {
            _uiState.update { it.copy(selectedTab = tab) }
            Timber.d("Island detail tab selected: ${tab.title}")
        }
    }

    // ============================================================
    // LOADING
    // ============================================================

    fun loadIslandDetails(islandId: String) {
        if (islandId.isBlank()) {
            _uiState.update { it.copy(error = "ID isola non valido") }
            return
        }

        currentIslandId = islandId

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val islandResult = getIslandByIdUseCase(islandId)
                val statsResult  = getStatisticsUseCase(islandId)

                islandResult.fold(
                    onSuccess = { island ->
                        statsResult.fold(
                            onSuccess = { statistics ->
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        island = island,
                                        statistics = statistics,
                                        hasData = true,
                                        error = null
                                    )
                                }
                            },
                            onFailure = { error ->
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        island = island,
                                        statistics = null,
                                        hasData = true,
                                        error = "Statistiche non disponibili: ${error.message}"
                                    )
                                }
                            }
                        )
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Errore nel caricamento isola",
                                hasData = false
                            )
                        }
                        return@launch
                    }
                )

                // Load mechanical units independently — failure is non-critical
                loadMechanicalUnits(islandId)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Errore imprevisto: ${e.message}",
                        hasData = false
                    )
                }
            }
        }
    }

    private suspend fun loadMechanicalUnits(islandId: String) {
        _uiState.update { it.copy(isLoadingUnits = true) }
        getMechanicalUnitsUseCase(islandId).fold(
            onSuccess = { units ->
                _uiState.update { it.copy(units = units, isLoadingUnits = false) }
            },
            onFailure = { error ->
                Timber.w(error, "Failed to load mechanical units for island $islandId")
                _uiState.update { it.copy(isLoadingUnits = false) }
            }
        )
    }

    // ============================================================
    // MECHANICAL UNIT ACTIONS
    // ============================================================

    fun deleteUnit(unit: MechanicalUnit) {
        viewModelScope.launch {
            mechanicalUnitRepository.delete(unit.id).fold(
                onSuccess = {
                    Timber.d("MechanicalUnit deleted: ${unit.id}")
                    loadMechanicalUnits(currentIslandId)
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to delete MechanicalUnit: ${unit.id}")
                    _uiState.update { it.copy(error = "Errore eliminazione unità: ${error.message}") }
                }
            )
        }
    }

    // ============================================================
    // MAINTENANCE
    // ============================================================

    fun recordMaintenance(
        maintenanceDate: Instant? = null,
        resetOperatingHours: Boolean = true,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingMaintenance = true) }

            updateMaintenanceUseCase(
                islandId = currentIslandId,
                maintenanceDate = maintenanceDate ?: Clock.System.now(),
                resetOperatingHours = resetOperatingHours,
                notes = notes
            ).fold(
                onSuccess = {
                    loadIslandDetails(currentIslandId)
                    _uiState.update { it.copy(isUpdatingMaintenance = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUpdatingMaintenance = false,
                            error = "Errore aggiornamento manutenzione: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun updateNextMaintenance(nextMaintenanceDate: Instant?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingMaintenance = true) }

            updateMaintenanceUseCase.updateNextScheduledMaintenance(
                islandId = currentIslandId,
                nextMaintenanceDate = nextMaintenanceDate
            ).fold(
                onSuccess = {
                    loadIslandDetails(currentIslandId)
                    _uiState.update { it.copy(isUpdatingMaintenance = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUpdatingMaintenance = false,
                            error = "Errore programmazione manutenzione: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    // ============================================================
    // DELETE ISLAND
    // ============================================================

    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun deleteFacilityIsland(force: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isDeleting = true, deleteError = null, showDeleteConfirmation = false)
            }

            try {
                deleteIslandUseCase(islandId = currentIslandId, force = force).fold(
                    onSuccess = {
                        Timber.d("Island deleted successfully: $currentIslandId")
                        _uiState.update { it.copy(isDeleting = false, deleteSuccess = true) }
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to delete island: $currentIslandId")
                        _uiState.update {
                            it.copy(
                                isDeleting = false,
                                deleteError = "Errore eliminazione: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception deleting island")
                _uiState.update {
                    it.copy(isDeleting = false, deleteError = "Errore imprevisto: ${e.message}")
                }
            }
        }
    }

    // ============================================================
    // MISC
    // ============================================================

    fun refreshData() {
        if (currentIslandId.isNotBlank()) loadIslandDetails(currentIslandId)
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetDeleteState() {
        _uiState.update { it.copy(deleteSuccess = false, deleteError = null) }
    }
}

// =============================================================================
// EVENTS (unchanged)
// =============================================================================

sealed class FacilityIslandDetailEvent {
    object Refresh : FacilityIslandDetailEvent()
    object DismissError : FacilityIslandDetailEvent()

    data class RecordMaintenance(
        val maintenanceDate: Instant? = null,
        val resetHours: Boolean = true,
        val notes: String? = null
    ) : FacilityIslandDetailEvent()

    data class UpdateNextMaintenance(val date: Instant?) : FacilityIslandDetailEvent()
    data class DeleteIsland(val force: Boolean = false) : FacilityIslandDetailEvent()
    object GetDeletionInfo : FacilityIslandDetailEvent()
    object DismissDeletionInfo : FacilityIslandDetailEvent()
}