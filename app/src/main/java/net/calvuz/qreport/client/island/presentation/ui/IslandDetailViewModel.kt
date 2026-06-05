package net.calvuz.qreport.client.island.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.error.presentation.asUiText
import net.calvuz.qreport.app.error.presentation.toUiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.usecase.DeleteIslandUseCase
import net.calvuz.qreport.client.island.domain.usecase.GetIslandByIdUseCase
import net.calvuz.qreport.client.island.domain.usecase.GetIslandStatisticsUseCase
import net.calvuz.qreport.client.island.domain.usecase.SingleIslandStatistics
import net.calvuz.qreport.client.island.domain.usecase.UpdateMaintenanceUseCase
import net.calvuz.qreport.client.unit.domain.usecase.DeleteMechanicalUnitUseCase
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.usecase.GetMechanicalUnitsByIslandUseCase
import timber.log.Timber
import javax.inject.Inject

// =============================================================================
// TAB ENUM
// =============================================================================

/**
 * Tabs for the island detail screen.
 * [labelResId] is resolved at runtime via stringResource() in the composable.
 */
enum class IslandDetailTab(val labelResId: Int) {
    UNITS(R.string.island_detail_tab_units),
    MAINTENANCE(R.string.island_detail_tab_maintenance),
    INFO(R.string.island_detail_tab_info),
}

// =============================================================================
// UI STATE
// =============================================================================

data class FacilityIslandDetailUiState(
    val isLoading: Boolean = false,
    val hasData: Boolean = false,
    val island: Island? = null,
    val statistics: SingleIslandStatistics? = null,

    val selectedTab: IslandDetailTab = IslandDetailTab.UNITS,

    val units: List<MechanicalUnit> = emptyList(),
    val isLoadingUnits: Boolean = false,

    val isRefreshing: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val showDeleteConfirmation: Boolean = false,

    val isUpdatingMaintenance: Boolean = false,

    val error: UiText? = null
) {
    val islandName: String
        get() = island?.customName ?: island?.serialNumber ?: ""

    val hasOperationsInProgress: Boolean
        get() = isUpdatingMaintenance || isDeleting

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
    private val getMechanicalUnitsByIslandUseCase: GetMechanicalUnitsByIslandUseCase,
    private val updateMaintenanceUseCase: UpdateMaintenanceUseCase,
    private val deleteIslandUseCase: DeleteIslandUseCase,
    private val deleteMechanicalUnitUseCase: DeleteMechanicalUnitUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacilityIslandDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var currentIslandId: String = ""

    init {
        Timber.d("IslandDetailViewModel initialized")
    }

    fun onDetailEvent(event: IslandDetailEvent) {
        when (event) {
            is IslandDetailEvent.LoadIsland -> loadIslandDetails(event.islandId)
            is IslandDetailEvent.DeleteIsland -> deleteFacilityIsland(event.force)
            is IslandDetailEvent.DeleteUnit -> deleteUnit(event.unit)
            is IslandDetailEvent.RecordMaintenance -> recordMaintenance(
                maintenanceDate = event.maintenanceDate,
                resetOperatingHours = event.resetHours,
                notes = event.notes
            )
            IslandDetailEvent.DismissError -> dismissError()
            IslandDetailEvent.Refresh -> refreshData()
        }
    }


    // =========================================================================
    // TAB
    // =========================================================================

    fun selectTab(tab: IslandDetailTab) {
        if (_uiState.value.selectedTab != tab) {
            _uiState.update { it.copy(selectedTab = tab) }
        }
    }

    // =========================================================================
    // LOADING
    // =========================================================================

    private fun loadIslandDetails(islandId: String) {
        if (islandId.isBlank()) {
            _uiState.update {
                it.copy(error = UiText.StringResource(R.string.err_island_detail_invalid_id))
            }
            return
        }

        currentIslandId = islandId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val islandResult = getIslandByIdUseCase(islandId)) {
                is QrResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, hasData = false, error = UiText.StringResource(R.string.err_island_detail_load))
                    }
                    return@launch
                }
                is QrResult.Success -> {
                    val statsError: UiText? = when (val statsResult = getStatisticsUseCase(islandId)) {
                        is QrResult.Success -> {
                            _uiState.update {
                                it.copy(isLoading = false, island = islandResult.data, statistics = statsResult.data, hasData = true, error = null)
                            }
                            null
                        }
                        is QrResult.Error -> {
                            _uiState.update {
                                it.copy(isLoading = false, island = islandResult.data, statistics = null, hasData = true,
                                    error = UiText.StringResource(R.string.err_island_detail_stats))
                            }
                            UiText.StringResource(R.string.err_island_detail_stats)
                        }
                    }
                    Timber.d("Island loaded: ${islandResult.data.customName ?: islandResult.data.serialNumber} statsError=$statsError")
                }
            }

            loadMechanicalUnits(islandId)
        }
    }

    private suspend fun loadMechanicalUnits(islandId: String) {
        _uiState.update { it.copy(isLoadingUnits = true) }
        when (val result = getMechanicalUnitsByIslandUseCase(islandId)) {
            is QrResult.Success ->{
                _uiState.update { it.copy(units = result.data, isLoadingUnits = false) }
            }
            is QrResult.Error -> {
                Timber.w(QrError.UnitError.NotFound(islandId).toUiText().toString())
                _uiState.update { it.copy(isLoadingUnits = false) }
            }
        }
    }

    // =========================================================================
    // UNIT ACTIONS
    // =========================================================================

    private fun deleteUnit(unit: MechanicalUnit) {
        viewModelScope.launch {
            when (val result= deleteMechanicalUnitUseCase(unit.id)) {
                is QrResult.Success -> {
                    Timber.d("MechanicalUnit deleted: ${unit.id}")
                    loadMechanicalUnits(currentIslandId)
                }

                is QrResult.Error -> {
                    Timber.d("Failed to delete MechanicalUnit: ${unit.id}")
                    _uiState.update {
                        it.copy(error = result.error.asUiText())
                    }
                }
            }
        }
    }

    // =========================================================================
    // MAINTENANCE
    // =========================================================================

    fun recordMaintenance(
        maintenanceDate: Instant? = null,
        resetOperatingHours: Boolean = true,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingMaintenance = true) }
            when (updateMaintenanceUseCase(
                islandId = currentIslandId,
                maintenanceDate = maintenanceDate ?: Clock.System.now(),
                resetOperatingHours = resetOperatingHours,
                notes = notes
            )) {
                is QrResult.Success -> {
                    _uiState.update { it.copy(isUpdatingMaintenance = false) }
                    loadIslandDetails(currentIslandId)
                }
                is QrResult.Error -> _uiState.update {
                    it.copy(isUpdatingMaintenance = false, error = UiText.StringResource(R.string.err_island_detail_maintenance))
                }
            }
        }
    }

    // =========================================================================
    // DELETE ISLAND
    // =========================================================================

    private fun deleteFacilityIsland(force: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null, showDeleteConfirmation = false) }
            when (val result = deleteIslandUseCase(islandId = currentIslandId, force = force)) {
                is QrResult.Success -> {
                    Timber.d("Island deleted: $currentIslandId")
                    _uiState.update { it.copy(isDeleting = false, deleteSuccess = true) }
                }
                is QrResult.Error -> {
                    Timber.e("Failed to delete island: $currentIslandId")
                    _uiState.update {
                        it.copy(isDeleting = false, error = result.error.asUiText())
                    }
                }
            }
        }
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    private fun refreshData() {
        if (currentIslandId.isNotBlank()) loadIslandDetails(currentIslandId)
    }

    private fun dismissError() = _uiState.update { it.copy(error = null) }
    fun resetDeleteState() = _uiState.update { it.copy(deleteSuccess = false) }

    fun showDeleteConfirmation() = _uiState.update { it.copy(showDeleteConfirmation = true) }
    fun hideDeleteConfirmation() = _uiState.update { it.copy(showDeleteConfirmation = false) }
}

sealed class IslandDetailEvent {
    data class LoadIsland(val islandId: String) : IslandDetailEvent()
    data class DeleteIsland(val force: Boolean = false) : IslandDetailEvent()
    data class DeleteUnit(val unit: MechanicalUnit) : IslandDetailEvent()
    object Refresh : IslandDetailEvent()
    object DismissError : IslandDetailEvent()
    data class RecordMaintenance(
        val maintenanceDate: Instant? = null,
        val resetHours: Boolean = true,
        val notes: String? = null
    ) : IslandDetailEvent()
}