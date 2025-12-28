package net.calvuz.qreport.presentation.feature.client.facilityisland

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.usecase.client.facilityisland.GetFacilityIslandByIdUseCase
import net.calvuz.qreport.domain.usecase.client.facilityisland.GetFacilityIslandStatisticsUseCase
import net.calvuz.qreport.domain.usecase.client.facilityisland.UpdateMaintenanceUseCase
import net.calvuz.qreport.domain.usecase.client.facilityisland.DeleteFacilityIslandUseCase
import net.calvuz.qreport.domain.usecase.client.facilityisland.SingleIslandStatistics
import kotlinx.datetime.Instant
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel per FacilityIslandDetailScreen
 *
 * Gestisce:
 * - Caricamento dettagli isola con statistiche
 * - Aggiornamenti stato operativo
 * - Gestione manutenzione (registro, programmazione)
 * - Eliminazione isola con validazioni
 * - Refresh dati real-time
 */
@HiltViewModel
class FacilityIslandDetailViewModel @Inject constructor(
    private val getIslandByIdUseCase: GetFacilityIslandByIdUseCase,
    private val getStatisticsUseCase: GetFacilityIslandStatisticsUseCase,
    private val updateMaintenanceUseCase: UpdateMaintenanceUseCase,
    private val deleteFacilityIslandUseCase: DeleteFacilityIslandUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacilityIslandDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var currentFacilityId: String = ""
    private var currentIslandId: String = ""

    init {
        Timber.d("FacilityIslandDetailViewModel initialized")
    }

    /**
     * Carica dettagli isola
     */
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
                // Carica isola e statistiche in parallelo
                val islandResult = getIslandByIdUseCase(islandId)
                val statsResult = getStatisticsUseCase(islandId)

                islandResult.fold(
                    onSuccess = { island ->
                        statsResult.fold(
                            onSuccess = { statistics ->
                                _uiState.update { currentState ->
                                    currentState.copy(
                                        isLoading = false,
                                        island = island,
                                        statistics = statistics,
                                        hasData = true,
                                        error = null
                                    )
                                }
                            },
                            onFailure = { error ->
                                // Mostra isola anche se le statistiche falliscono
                                _uiState.update { currentState ->
                                    currentState.copy(
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
                    }
                )
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

    /**
     * Registra manutenzione completata
     */
    fun recordMaintenance(
        maintenanceDate: Instant? = null,
        resetOperatingHours: Boolean = true,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingMaintenance = true) }

            updateMaintenanceUseCase(
                islandId = currentIslandId,
                maintenanceDate = maintenanceDate ?: kotlinx.datetime.Clock.System.now(),
                resetOperatingHours = resetOperatingHours,
                notes = notes
            ).fold(
                onSuccess = {
                    // Ricarica dati dopo aggiornamento
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

    /**
     * Aggiorna prossima manutenzione programmata
     */
    fun updateNextMaintenance(nextMaintenanceDate: Instant?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingMaintenance = true) }

            updateMaintenanceUseCase.updateNextScheduledMaintenance(
                islandId = currentIslandId,
                nextMaintenanceDate = nextMaintenanceDate
            ).fold(
                onSuccess = {
                    // Ricarica dati
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
     * Elimina isola con validazioni
     */
    fun deleteFacilityIsland(force: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDeleting = true,
                deleteError = null,
                showDeleteConfirmation = false
            )

            try {
                deleteFacilityIslandUseCase(islandId = currentIslandId, force = force).fold(
                    onSuccess = {
                        Timber.d("FacilityIsland deleted successfully: $currentIslandId")
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            deleteSuccess = true  // ✅ Trigger navigation back
                        )
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to delete FacilityIsland: $currentIslandId")

                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            deleteError = "Errore eliminazione: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception deleting FacilityIsland")
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    deleteError = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    /**
     * Refresh dati
     */
    fun refreshData() {
        if (currentIslandId.isNotBlank()) {
            loadIslandDetails(currentIslandId)
        }
    }

    /**
     * Dismisses current error
     */
    fun dismissError() {
        _uiState.update { it.copy(error = null) }
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
}

/**
 * UI State per dettaglio isola
 */
data class FacilityIslandDetailUiState(
    val isLoading: Boolean = false,
    val hasData: Boolean = false,
    val island: FacilityIsland? = null,
    val statistics: SingleIslandStatistics? = null,

    // Delete states
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val deleteError: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val islandDeleted: Boolean = false,

    // Stati operazioni
    val isUpdatingMaintenance: Boolean = false,

    // Dati aggiuntivi
    val deletionInfo: net.calvuz.qreport.domain.usecase.client.facilityisland.IslandDeletionInfo? = null,

    // Errori
    val error: String? = null
) {
    /**
     * Nome display per UI
     */
    val islandName: String
        get() = island?.displayName ?: "Isola"

    /**
     * Indica se ci sono operazioni in corso
     */
    val hasOperationsInProgress: Boolean
        get() = isUpdatingMaintenance || isDeleting

    /**
     * Indica se l'isola richiede attenzione immediata
     */
    val needsAttention: Boolean
        get() = statistics?.needsAttention == true || island?.needsMaintenance() == true

    /**
     * Testo stato per subtitle
     */
    val statusText: String
        get() = statistics?.statusDescription
            ?: island?.facilityIslandOperationalStatus?.displayName ?: ""
}

/**
 * Eventi UI
 */
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