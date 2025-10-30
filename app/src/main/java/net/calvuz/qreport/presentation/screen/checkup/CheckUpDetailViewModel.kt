package net.calvuz.qreport.presentation.screen.checkup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.domain.model.*
import net.calvuz.qreport.domain.usecase.checkup.*
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel per CheckUpDetailScreen - VERSIONE COMPLETA
 *
 * Gestisce:
 * - Caricamento check-up details
 * - Aggiornamento stati check items
 * - Gestione note e foto
 * - Calcolo statistiche e progresso
 * - Export functionality
 * - Gestione spare parts
 */

data class CheckUpDetailUiState(
    val checkUp: CheckUp? = null,
    val checkItems: List<CheckItem> = emptyList(),
    val spareParts: List<SparePart> = emptyList(),
    val progress: CheckUpProgress = CheckUpProgress(),
    val statistics: CheckUpStatistics = CheckUpStatistics(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUpdating: Boolean = false,
    val isAddingSparePart: Boolean = false,
    val showAddSparePartDialog: Boolean = false,
    // Header editing state
    val showEditHeaderDialog: Boolean = false,
    val isUpdatingHeader: Boolean = false
) {
    val checkItemsByModule: Map<ModuleType, List<CheckItem>>
        get() = checkItems.groupBy { it.moduleType }
}

@HiltViewModel
class CheckUpDetailViewModel @Inject constructor(
    private val getCheckUpDetailsUseCase: GetCheckUpDetailsUseCase,
    private val updateCheckUpStatusUseCase: UpdateCheckUpStatusUseCase,
    private val updateCheckItemStatusUseCase: UpdateCheckItemStatusUseCase,
    private val updateCheckItemNotesUseCase: UpdateCheckItemNotesUseCase,
    private val addSparePartUseCase: AddSparePartUseCase,
    private val exportCheckUpUseCase: ExportCheckUpUseCase,
    private val updateCheckUpHeaderUseCase: UpdateCheckUpHeaderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckUpDetailUiState())
    val uiState: StateFlow<CheckUpDetailUiState> = _uiState.asStateFlow()

    init {
        Timber.d("CheckUpDetailViewModel initialized")
    }

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    fun loadCheckUp(checkUpId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                Timber.d("Loading check-up details: $checkUpId")

                getCheckUpDetailsUseCase(checkUpId).fold(
                    onSuccess = { checkUpDetails ->
                        Timber.d("Check-up loaded: ${checkUpDetails.checkUp.id}")

                        _uiState.value = _uiState.value.copy(
                            checkUp = checkUpDetails.checkUp,
                            checkItems = checkUpDetails.checkItems,
                            spareParts = checkUpDetails.spareParts,
                            progress = checkUpDetails.progress,
                            statistics = checkUpDetails.statistics,
                            isLoading = false,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to load check-up details")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Errore caricamento check-up: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception loading check-up details")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    fun updateItemStatus(itemId: String, newStatus: CheckItemStatus) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true)

            try {
                Timber.d("Updating item status: $itemId -> $newStatus")

                updateCheckItemStatusUseCase(itemId, newStatus).fold(
                    onSuccess = {
                        Timber.d("Item status updated successfully")
                        // Ricarica i dati per sincronizzare tutto
                        val checkUpId = _uiState.value.checkUp?.id
                        if (checkUpId != null) {
                            reloadCheckUpData(checkUpId)
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to update item status")
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            error = "Errore aggiornamento status: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception updating item status")
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    fun updateItemNotes(itemId: String, notes: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true)

            try {
                Timber.d("Updating item notes: $itemId")

                updateCheckItemNotesUseCase(itemId, notes).fold(
                    onSuccess = {
                        Timber.d("Item notes updated successfully")
                        // Ricarica i dati per sincronizzare tutto
                        val checkUpId = _uiState.value.checkUp?.id
                        if (checkUpId != null) {
                            reloadCheckUpData(checkUpId)
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to update item notes")
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            error = "Errore aggiornamento note: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception updating item notes")
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    fun addSparePart(
        partNumber: String,
        description: String,
        quantity: Int,
        urgency: SparePartUrgency,
        category: SparePartCategory,
        estimatedCost: Double? = null,
        notes: String = "",
        supplierInfo: String = ""
    ) {
        viewModelScope.launch {
            val checkUpId = _uiState.value.checkUp?.id
            if (checkUpId == null) {
                _uiState.value = _uiState.value.copy(
                    error = "Check-up non disponibile"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isAddingSparePart = true)

            try {
                Timber.d("Adding spare part to check-up: $checkUpId")

                addSparePartUseCase(
                    checkUpId = checkUpId,
                    partNumber = partNumber,
                    description = description,
                    quantity = quantity,
                    urgency = urgency,
                    category = category,
                    estimatedCost = estimatedCost,
                    notes = notes,
                    supplierInfo = supplierInfo
                ).fold(
                    onSuccess = { sparePartId ->
                        Timber.d("Spare part added successfully: $sparePartId")
                        // Ricarica i dati per includere il nuovo spare part
                        reloadCheckUpData(checkUpId)
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to add spare part")
                        _uiState.value = _uiState.value.copy(
                            isAddingSparePart = false,
                            error = "Errore aggiunta ricambio: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception adding spare part")
                _uiState.value = _uiState.value.copy(
                    isAddingSparePart = false,
                    error = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    fun completeCheckUp() {
        viewModelScope.launch {
            try {
                val checkUpId = _uiState.value.checkUp?.id ?: return@launch
                Timber.d("Completing check-up: $checkUpId")

                _uiState.value = _uiState.value.copy(isUpdating = true)

                updateCheckUpStatusUseCase(checkUpId, CheckUpStatus.COMPLETED).fold(
                    onSuccess = {
                        Timber.d("Check-up completed successfully")
                        // Ricarica per aggiornare il status
                        reloadCheckUpData(checkUpId)
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to complete check-up")
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            error = "Errore completamento: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception completing check-up")
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    fun exportReport() {
        viewModelScope.launch {
            try {
                val checkUpId = _uiState.value.checkUp?.id ?: return@launch
                Timber.d("Exporting report: $checkUpId")

                _uiState.value = _uiState.value.copy(isUpdating = true)

                exportCheckUpUseCase(checkUpId).fold(
                    onSuccess = { exportedFile ->
                        Timber.d("Report exported successfully")
                        _uiState.value = _uiState.value.copy(isUpdating = false)
                        // TODO: Show success message or open file
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to export report")
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            error = "Errore export: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception exporting report")
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun showAddSparePartDialog() {
        _uiState.value = _uiState.value.copy(showAddSparePartDialog = true)
    }

    fun hideAddSparePartDialog() {
        _uiState.value = _uiState.value.copy(showAddSparePartDialog = false)
    }

    // ============================================================
    // HEADER EDITING METHODS
    // ============================================================

    fun showEditHeaderDialog() {
        _uiState.value = _uiState.value.copy(showEditHeaderDialog = true)
    }

    fun hideEditHeaderDialog() {
        _uiState.value = _uiState.value.copy(showEditHeaderDialog = false)
    }

    fun updateCheckUpHeader(newHeader: CheckUpHeader) {
        viewModelScope.launch {
            val checkUpId = _uiState.value.checkUp?.id
            if (checkUpId == null) {
                _uiState.value = _uiState.value.copy(
                    error = "Check-up non disponibile"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isUpdatingHeader = true)

            try {
                Timber.d("Updating check-up header: $checkUpId")

                updateCheckUpHeaderUseCase(checkUpId, newHeader).fold(
                    onSuccess = {
                        Timber.d("Header updated successfully")
                        // Ricarica i dati per sincronizzare tutto
                        reloadCheckUpData(checkUpId)
                        _uiState.value = _uiState.value.copy(
                            showEditHeaderDialog = false
                        )
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to update header")
                        _uiState.value = _uiState.value.copy(
                            isUpdatingHeader = false,
                            error = "Errore aggiornamento header: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception updating header")
                _uiState.value = _uiState.value.copy(
                    isUpdatingHeader = false,
                    error = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    /**
     * Ricarica i dati del check-up senza mostrare il loading
     */
    private suspend fun reloadCheckUpData(checkUpId: String) {
        try {
            getCheckUpDetailsUseCase(checkUpId).fold(
                onSuccess = { checkUpDetails ->
                    _uiState.value = _uiState.value.copy(
                        checkUp = checkUpDetails.checkUp,
                        checkItems = checkUpDetails.checkItems,
                        spareParts = checkUpDetails.spareParts,
                        progress = checkUpDetails.progress,
                        statistics = checkUpDetails.statistics,
                        isUpdating = false,
                        isAddingSparePart = false,
                        isUpdatingHeader = false
                    )
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to reload check-up data")
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        isAddingSparePart = false,
                        isUpdatingHeader = false,
                        error = "Errore ricaricamento dati: ${error.message}"
                    )
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Exception reloading check-up data")
            _uiState.value = _uiState.value.copy(
                isUpdating = false,
                isAddingSparePart = false,
                isUpdatingHeader = false,
                error = "Errore imprevisto: ${e.message}"
            )
        }
    }
}