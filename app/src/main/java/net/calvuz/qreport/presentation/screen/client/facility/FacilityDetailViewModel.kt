package net.calvuz.qreport.presentation.screen.client.facility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import net.calvuz.qreport.domain.model.client.Facility
import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.model.island.IslandType
import net.calvuz.qreport.domain.usecase.client.facility.DeleteFacilityUseCase
import net.calvuz.qreport.domain.usecase.client.facility.GetFacilityWithIslandsUseCase
import net.calvuz.qreport.domain.usecase.client.facilityisland.GetFacilityIslandsByFacilityUseCase
import net.calvuz.qreport.domain.usecase.client.facilityisland.DeleteFacilityIslandUseCase
import net.calvuz.qreport.domain.usecase.client.facilityisland.UpdateMaintenanceUseCase
import net.calvuz.qreport.domain.usecase.client.facilityisland.FacilityOperationalSummary
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel per FacilityDetailScreen - seguendo pattern ClientDetailViewModel
 *
 * Gestisce:
 * - Caricamento dettagli facility completi con isole
 * - Navigation tra tab (Info, Islands, Maintenance)
 * - Stato loading/error/success
 * - Actions per gestione islands CRUD
 * - Statistiche operative facility
 */

data class FacilityDetailUiState(
    // Data loading
    val isLoading: Boolean = false,
    val error: String? = null,
    val facility: Facility? = null,

    // UI State
    val selectedTab: FacilityDetailTab = FacilityDetailTab.INFO,

    // Delete states
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val deleteError: String? = null,
    val showDeleteConfirmation: Boolean = false,

    // Quick access data
    val facilityName: String = "",
    val facilityType: String = "",
    val address: String = "",
    val statusBadge: String = "",
    val statusBadgeColor: String = "6C757D",
    val statisticsSummary: String = "",

    // Tab data
    val islands: List<FacilityIsland> = emptyList(),
    val filteredIslands: List<FacilityIsland> = emptyList(),
    val operationalSummary: FacilityOperationalSummary? = null,
    val selectedIslandFilter: IslandFilter = IslandFilter.ALL,

    // Maintenance data
    val islandsNeedingMaintenance: List<FacilityIsland> = emptyList(),
    val islandsUnderWarranty: List<FacilityIsland> = emptyList()
) {
    val hasData: Boolean
        get() = facility != null

    val facilityId: String?
        get() = facility?.id

    val isEmpty: Boolean
        get() = !isLoading && !hasData && error == null

    val clientId: String?
        get() = facility?.clientId

    val isPrimaryFacility: Boolean
        get() = facility?.isPrimary == true

    // Tab counts per badge
    val islandsCount: Int
        get() = islands.size

    val activeIslandsCount: Int
        get() = islands.count { it.isActive }

    val maintenanceIssuesCount: Int
        get() = islandsNeedingMaintenance.size
}

/**
 * Tab disponibili nella facility detail screen
 */
enum class FacilityDetailTab(val title: String) {
    INFO("Informazioni"),
    ISLANDS("Isole"),
    MAINTENANCE("Manutenzione")
}

/**
 * Filtri per visualizzazione isole
 */
enum class IslandFilter {
    ALL, ACTIVE, INACTIVE, NEEDS_MAINTENANCE, UNDER_WARRANTY, BY_TYPE
}

@HiltViewModel
class FacilityDetailViewModel @Inject constructor(
    private val getFacilityWithIslandsUseCase: GetFacilityWithIslandsUseCase,
    private val getFacilityIslandsByFacilityUseCase: GetFacilityIslandsByFacilityUseCase,
    private val deleteFacilityUseCase: DeleteFacilityUseCase,
    private val deleteFacilityIslandUseCase: DeleteFacilityIslandUseCase,
    private val updateMaintenanceUseCase: UpdateMaintenanceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacilityDetailUiState())
    val uiState: StateFlow<FacilityDetailUiState> = _uiState.asStateFlow()

    init {
        Timber.d("FacilityDetailViewModel initialized")
    }

    // ============================================================
    // FACILITY LOADING
    // ============================================================

    fun loadFacilityDetails(facilityId: String) {
        if (facilityId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "ID facility non valido"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                // Load facility with islands
                getFacilityWithIslandsUseCase(facilityId).fold(
                    onSuccess = { facilityWithIslands ->
                        Timber.d("Facility details loaded successfully: ${facilityWithIslands.facility.name}")
                        populateUiState(facilityWithIslands.facility, facilityWithIslands.islands)

                        // Load additional data
                        loadOperationalSummary(facilityId)
                        loadMaintenanceData(facilityId)
                    },
                    onFailure = { error ->
                        if (currentCoroutineContext().isActive) {
                            Timber.e(error, "Failed to load facility details for ID: $facilityId")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Errore caricamento stabilimento: ${error.message}"
                            )
                        }
                    }
                )
            } catch (_: CancellationException) {
                Timber.d("Load facility details cancelled")
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e, "Exception loading facility details")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Errore imprevisto: ${e.message}"
                    )
                }
            }
        }
    }

    private fun populateUiState(facility: Facility, islands: List<FacilityIsland>) {
        val activeIslands = islands.filter { it.isActive }
        val statusBadge = if (facility.isActive) "Attivo" else "Inattivo"
        val statusColor = if (facility.isActive) "22C55E" else "6B7280"

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = null,
            facility = facility,

            // Quick access data
            facilityName = facility.displayName,
            facilityType = facility.facilityType.displayName,
            address = facility.addressDisplay,
            statusBadge = statusBadge,
            statusBadgeColor = statusColor,
            statisticsSummary = "${activeIslands.size} isole attive",

            // Islands data
            islands = islands,
            filteredIslands = applyIslandFilter(islands, _uiState.value.selectedIslandFilter)
        )
    }

    private suspend fun loadOperationalSummary(facilityId: String) {
        try {
            getFacilityIslandsByFacilityUseCase.getFacilityOperationalSummary(facilityId).fold(
                onSuccess = { summary ->
                    if (currentCoroutineContext().isActive) {
                        _uiState.value = _uiState.value.copy(
                            operationalSummary = summary,
                            statisticsSummary = "${summary.activeIslands}/${summary.totalIslands} isole attive"
                        )
                    }
                },
                onFailure = { error ->
                    Timber.w(error, "Failed to load operational summary")
                }
            )
        } catch (_: CancellationException) {
            // Ignore cancellation
        } catch (e: Exception) {
            Timber.e(e, "Exception loading operational summary")
        }
    }

    private suspend fun loadMaintenanceData(facilityId: String) {
        try {
            // Load islands needing maintenance
            getFacilityIslandsByFacilityUseCase.getIslandsDueMaintenance(facilityId).fold(
                onSuccess = { maintenanceIslands ->
                    if (currentCoroutineContext().isActive) {
                        _uiState.value = _uiState.value.copy(
                            islandsNeedingMaintenance = maintenanceIslands
                        )
                    }
                },
                onFailure = { error ->
                    Timber.w(error, "Failed to load maintenance data")
                }
            )

            // Load islands under warranty
            getFacilityIslandsByFacilityUseCase.getIslandsUnderWarranty(facilityId).fold(
                onSuccess = { warrantyIslands ->
                    if (currentCoroutineContext().isActive) {
                        _uiState.value = _uiState.value.copy(
                            islandsUnderWarranty = warrantyIslands
                        )
                    }
                },
                onFailure = { error ->
                    Timber.w(error, "Failed to load warranty data")
                }
            )
        } catch (_: CancellationException) {
            // Ignore cancellation
        } catch (e: Exception) {
            Timber.e(e, "Exception loading maintenance data")
        }
    }

    // ============================================================
    // DELETE OPERATIONS
    // ============================================================

    /**
     * Mostra dialog di conferma prima di eliminare
     */
    fun showDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = true
        )
    }

    /**
     * Nasconde dialog di conferma
     */
    fun hideDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = false
        )
    }

    /**
     * ✅ FUNZIONE PRINCIPALE per delete
     */
    fun deleteFacility() {
        val facilityId = _uiState.value.facilityId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDeleting = true,
                deleteError = null,
                showDeleteConfirmation = false
            )

            try {
                deleteFacilityUseCase(facilityId).fold(
                    onSuccess = {
                        Timber.d("Facility deleted successfully: $facilityId")
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            deleteSuccess = true  // ✅ Trigger navigation back
                        )
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to delete facility: $facilityId")
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            deleteError = "Errore eliminazione: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception deleting facility")
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    deleteError = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    // ============================================================
    // TAB NAVIGATION
    // ============================================================

    fun selectTab(tab: FacilityDetailTab) {
        if (_uiState.value.selectedTab != tab) {
            _uiState.value = _uiState.value.copy(selectedTab = tab)
            Timber.d("Selected tab: ${tab.title}")
        }
    }

    // ============================================================
    // ISLANDS FILTERING
    // ============================================================

    fun updateIslandFilter(filter: IslandFilter) {
        val currentState = _uiState.value
        val filteredIslands = applyIslandFilter(currentState.islands, filter)

        _uiState.value = currentState.copy(
            selectedIslandFilter = filter,
            filteredIslands = filteredIslands
        )
    }

    private fun applyIslandFilter(islands: List<FacilityIsland>, filter: IslandFilter): List<FacilityIsland> {
        return when (filter) {
            IslandFilter.ALL -> islands
            IslandFilter.ACTIVE -> islands.filter { it.isActive }
            IslandFilter.INACTIVE -> islands.filter { !it.isActive }
            IslandFilter.NEEDS_MAINTENANCE -> islands.filter { it.needsMaintenance() }
            IslandFilter.UNDER_WARRANTY -> islands.filter { island ->
                island.warrantyExpiration?.let {
                    it > kotlinx.datetime.Clock.System.now()
                } == true
            }
            IslandFilter.BY_TYPE -> islands // Gestito dalla UI con selezione tipo
        }
    }

    // ============================================================
    // ISLAND ACTIONS
    // ============================================================

    fun deleteIsland(islandId: String) {
        viewModelScope.launch {
            try {
                Timber.d("Deleting island: $islandId")

                deleteFacilityIslandUseCase(islandId).fold(
                    onSuccess = {
                        Timber.d("Island deleted successfully")
                        // Refresh data
                        _uiState.value.facilityId?.let { facilityId ->
                            loadFacilityDetails(facilityId)
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to delete island")
                        _uiState.value = _uiState.value.copy(
                            error = "Errore eliminazione isola: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception deleting island")
                _uiState.value = _uiState.value.copy(
                    error = "Errore eliminazione isola: ${e.message}"
                )
            }
        }
    }

    fun markMaintenanceComplete(islandId: String) {
        viewModelScope.launch {
            try {
                Timber.d("Marking maintenance complete for island: $islandId")

                updateMaintenanceUseCase(
                    islandId = islandId,
                    notes = "Manutenzione completata da QReport"
                ).fold(
                    onSuccess = {
                        Timber.d("Maintenance marked complete successfully")
                        // Refresh data
                        _uiState.value.facilityId?.let { facilityId ->
                            loadFacilityDetails(facilityId)
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to mark maintenance complete")
                        _uiState.value = _uiState.value.copy(
                            error = "Errore aggiornamento manutenzione: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception marking maintenance complete")
                _uiState.value = _uiState.value.copy(
                    error = "Errore aggiornamento manutenzione: ${e.message}"
                )
            }
        }
    }

    // ============================================================
    // ACTIONS
    // ============================================================

    fun refreshData() {
        val currentFacilityId = _uiState.value.facilityId
        if (currentFacilityId != null) {
            loadFacilityDetails(currentFacilityId)
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Reset DELETE STATES
     */
    fun resetDeleteState() {
        _uiState.value = _uiState.value.copy(
            deleteSuccess = false,
            deleteError = null
        )
    }

    // ============================================================
    // NAVIGATION HELPERS
    // ============================================================

    /**
     * Ottieni facility ID per navigation
     */
    fun getFacilityIdForNavigation(): String? = _uiState.value.facilityId

    /**
     * Ottieni client ID per navigation
     */
    fun getClientIdForNavigation(): String? = _uiState.value.clientId

    /**
     * Ottieni facility name per navigation
     */
    fun getFacilityNameForNavigation(): String = _uiState.value.facilityName

    // ============================================================
    // UTILITY METHODS per UI convenience
    // ============================================================

    /**
     * Get islands by type for statistics
     */
    fun getIslandsByType(type: IslandType): List<FacilityIsland> =
        _uiState.value.islands.filter { it.islandType == type }

    /**
     * Get operational islands count
     */
    fun getOperationalIslandsCount(): Int =
        _uiState.value.islands.count { it.isActive && !it.needsMaintenance() }

    /**
     * Get total operating hours for facility
     */
    fun getTotalOperatingHours(): Int =
        _uiState.value.islands.sumOf { it.operatingHours }

    /**
     * Get total cycles for facility
     */
    fun getTotalCycles(): Long =
        _uiState.value.islands.sumOf { it.cycleCount }

    /**
     * Check if facility has complete setup
     */
    fun hasCompleteSetup(): Boolean =
        _uiState.value.facility != null && _uiState.value.islands.isNotEmpty()

    /**
     * Get primary island (future feature)
     */
    fun getPrimaryIsland(): FacilityIsland? =
        _uiState.value.islands.find { it.isActive } // Temporary: primo attivo

    /**
     * Check if any island needs immediate attention
     */
    fun hasUrgentIssues(): Boolean =
        _uiState.value.islandsNeedingMaintenance.isNotEmpty()

    /**
     * Get islands stats summary
     */
    fun getIslandsStatsSummary(): String {
        val summary = _uiState.value.operationalSummary
        return when {
            summary == null -> "Caricamento..."
            summary.totalIslands == 0 -> "Nessuna isola"
            else -> "${summary.activeIslands}/${summary.totalIslands} attive"
        }
    }

    // ============================================================
    // FUTURE: Actions per navigazioni specifiche
    // ============================================================

    /**
     * Azione per navigare al dettaglio island (futuro)
     */
    fun onIslandClick(islandId: String) {
        Timber.d("TODO: Navigate to island detail: $islandId")
        onIslandClick(islandId)
    }

    /**
     * Azione per creare nuovo CheckUp per facility (futuro)
     */
    fun onCreateFacilityCheckUpClick() {
        val facilityId = _uiState.value.facilityId
        Timber.d("TODO: Navigate to create CheckUp for facility: $facilityId")
        // TODO: Implementare navigazione quando CheckUp creation sarà integrato
    }
}