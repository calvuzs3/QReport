package net.calvuz.qreport.client.facility.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.usecase.DeleteFacilityUseCase
import net.calvuz.qreport.client.facility.domain.usecase.GetFacilityWithIslandsUseCase
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandType
import net.calvuz.qreport.client.island.domain.usecase.DeleteIslandUseCase
import net.calvuz.qreport.client.island.domain.usecase.FacilityOperationalSummary
import net.calvuz.qreport.client.island.domain.usecase.GetIslandsByFacilityUseCase
import net.calvuz.qreport.client.island.domain.usecase.UpdateMaintenanceUseCase
import timber.log.Timber
import javax.inject.Inject

// =============================================================================
// TABS
// =============================================================================

/**
 * Tab labels resolved via [labelResId] at runtime in the composable,
 * following the same pattern as [ClientDetailTab].
 */
enum class FacilityDetailTab(val labelResId: Int) {
    ISLANDS(R.string.facility_detail_tab_islands), MAINTENANCE(R.string.facility_detail_tab_maintenance), INFO(
        R.string.facility_detail_tab_info
    ),
}

// =============================================================================
// UI STATE
// =============================================================================

data class FacilityDetailUiState(

    // ===== DATA =====
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val facility: Facility? = null,

    // ===== DELETE =====
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val deleteError: UiText? = null,
    val showDeleteConfirmation: Boolean = false,

    // ===== UI =====
    val selectedTab: FacilityDetailTab = FacilityDetailTab.ISLANDS,

    // ===== TAB DATA =====
    val islands: List<Island> = emptyList(),
    val filteredIslands: List<Island> = emptyList(),
    val operationalSummary: FacilityOperationalSummary? = null,
    val islandsNeedingMaintenance: List<Island> = emptyList(),
    val islandsUnderWarranty: List<Island> = emptyList()

) {
    val hasData: Boolean get() = facility != null
    val isEmpty: Boolean get() = !isLoading && !hasData && error == null
    val facilityId: String? get() = facility?.id
    val clientId: String? get() = facility?.clientId
    val facilityName: String get() = facility?.displayName ?: ""
    val isPrimaryFacility: Boolean get() = facility?.isPrimary == true
    val islandsCount: Int get() = islands.size
    val activeIslandsCount: Int get() = islands.count { it.isActive }
    val maintenanceIssuesCount: Int get() = islandsNeedingMaintenance.size
}

// =============================================================================
// VIEW MODEL
// =============================================================================

@HiltViewModel
class FacilityDetailViewModel @Inject constructor(
    private val getFacilityWithIslandsUseCase: GetFacilityWithIslandsUseCase,
    private val getIslandsByFacilityUseCase: GetIslandsByFacilityUseCase,
    private val deleteFacilityUseCase: DeleteFacilityUseCase,
    private val deleteIslandUseCase: DeleteIslandUseCase,
    private val updateMaintenanceUseCase: UpdateMaintenanceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacilityDetailUiState())
    val uiState: StateFlow<FacilityDetailUiState> = _uiState.asStateFlow()

    init {
        Timber.d("FacilityDetailViewModel initialized")
    }

    // =========================================================================
    // LOADING
    // =========================================================================

    fun loadFacilityDetails(facilityId: String) {
        if (facilityId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = UiText.StringResource(R.string.err_facility_detail_invalid_id)
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = getFacilityWithIslandsUseCase(facilityId)) {
                is QrResult.Success -> {
                    val fwi = result.data
                    Timber.d("Facility details loaded: ${fwi.facility.name}")
                    populateUiState(fwi.facility, fwi.islands)
                    loadOperationalSummary(facilityId)
                    loadMaintenanceData(facilityId)
                }

                is QrResult.Error -> {
                    if (currentCoroutineContext().isActive) {
                        Timber.e("Failed to load facility: ${result.error}")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = UiText.StringResource(R.string.err_facility_detail_load)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun populateUiState(facility: Facility, islands: List<Island>) {
        _uiState.update {
            it.copy(
                isLoading = false,
                error = null,
                facility = facility,
                islands = islands,
            )
        }
    }

    private suspend fun loadOperationalSummary(facilityId: String) {
        try {
            getIslandsByFacilityUseCase.getFacilityOperationalSummary(facilityId)
                .fold(onSuccess = { summary ->
                    if (currentCoroutineContext().isActive) {
                        _uiState.update { it.copy(operationalSummary = summary) }
                    }
                }, onFailure = { Timber.w("Failed to load operational summary") })
        } catch (_: CancellationException) {
        } catch (e: Exception) {
            Timber.e(e, "Exception loading operational summary")
        }
    }

    private suspend fun loadMaintenanceData(facilityId: String) {
        try {
            getIslandsByFacilityUseCase.getIslandsDueMaintenance(facilityId)
                .fold(onSuccess = { maintenance ->
                    if (currentCoroutineContext().isActive) {
                        _uiState.update { it.copy(islandsNeedingMaintenance = maintenance) }
                    }
                }, onFailure = { Timber.w("Failed to load maintenance data") })
            getIslandsByFacilityUseCase.getIslandsUnderWarranty(facilityId)
                .fold(onSuccess = { warranty ->
                    if (currentCoroutineContext().isActive) {
                        _uiState.update { it.copy(islandsUnderWarranty = warranty) }
                    }
                }, onFailure = { Timber.w("Failed to load warranty data") })
        } catch (_: CancellationException) {
        } catch (e: Exception) {
            Timber.e(e, "Exception loading maintenance data")
        }
    }

    // =========================================================================
    // DELETE FACILITY
    // =========================================================================

    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun deleteFacility() {
        val facilityId = _uiState.value.facilityId ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDeleting = true,
                    deleteError = null,
                    showDeleteConfirmation = false
                )
            }

            when (val result = deleteFacilityUseCase(facilityId)) {
                is QrResult.Success -> {
                    Timber.d("Facility deleted: $facilityId")
                    _uiState.update { it.copy(isDeleting = false, deleteSuccess = true) }
                }

                is QrResult.Error -> {
                    Timber.e("Failed to delete facility: ${result.error}")
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            deleteError = UiText.StringResource(R.string.err_facility_detail_delete)
                        )
                    }
                }
            }
        }
    }

    fun resetDeleteState() {
        _uiState.update { it.copy(deleteSuccess = false, deleteError = null) }
    }

    // =========================================================================
    // ISLAND ACTIONS
    // =========================================================================

    fun deleteIsland(islandId: String) {
        viewModelScope.launch {
            when (val result = deleteIslandUseCase(islandId)) {
                is QrResult.Success -> {
                    Timber.d("Island deleted: $islandId")
                    _uiState.value.facilityId?.let { loadFacilityDetails(it) }
                }

                is QrResult.Error -> {
                    Timber.e("Failed to delete island: ${result.error}")
                    _uiState.update {
                        it.copy(error = UiText.StringResource(R.string.err_facility_detail_island_delete))
                    }
                }
            }
        }
    }

    fun markMaintenanceComplete(islandId: String) {
        viewModelScope.launch {
            when (val result = updateMaintenanceUseCase(
                islandId = islandId,
                notes = "Maintenance completed via QReport"
            )) {
                is QrResult.Success -> {
                    Timber.d("Maintenance marked complete: $islandId")
                    _uiState.value.facilityId?.let { loadFacilityDetails(it) }
                }

                is QrResult.Error -> {
                    Timber.e("Failed to mark maintenance: ${result.error}")
                    _uiState.update {
                        it.copy(error = UiText.StringResource(R.string.err_facility_detail_maintenance))
                    }
                }
            }
        }
    }

    // =========================================================================
    // TAB & FILTER
    // =========================================================================

    fun selectTab(tab: FacilityDetailTab) {
        if (_uiState.value.selectedTab != tab) {
            _uiState.update { it.copy(selectedTab = tab) }
        }
    }

    // =========================================================================
    // ACTIONS / NAVIGATION HELPERS
    // =========================================================================

    fun refreshData() {
        _uiState.value.facilityId?.let { loadFacilityDetails(it) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun getFacilityIdForNavigation(): String? = _uiState.value.facilityId
    fun getClientIdForNavigation(): String? = _uiState.value.clientId
    fun getFacilityNameForNavigation(): String = _uiState.value.facilityName

    // =========================================================================
    // UTILITY
    // =========================================================================

    fun getIslandsByType(type: IslandType): List<Island> =
        _uiState.value.islands.filter { it.islandType == type }

    fun getOperationalIslandsCount(): Int =
        _uiState.value.islands.count { it.isActive && !it.needsMaintenance() }

    fun getTotalOperatingHours(): Int = _uiState.value.islands.sumOf { it.operatingHours }

    fun getTotalCycles(): Long = _uiState.value.islands.sumOf { it.cycleCount }

    fun hasCompleteSetup(): Boolean =
        _uiState.value.facility != null && _uiState.value.islands.isNotEmpty()

    fun hasUrgentIssues(): Boolean = _uiState.value.islandsNeedingMaintenance.isNotEmpty()

    fun getIslandsStatsSummary(noIslandsText: String, loadingText: String): String {
        val summary = _uiState.value.operationalSummary
        return when {
            summary == null -> loadingText
            summary.totalIslands == 0 -> noIslandsText
            else -> "${summary.activeIslands}/${summary.totalIslands}"
        }
    }

    fun onCreateFacilityCheckUpClick() {
        Timber.d("TODO: Navigate to create CheckUp for facility: ${_uiState.value.facilityId}")
    }
}