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
 * ViewModel per CheckUpDetailScreen
 *
 * Gestisce:
 * - Caricamento check-up details
 * - Aggiornamento stati check items
 * - Gestione note e foto
 * - Calcolo statistiche e progresso
 * - Export functionality
 */

data class CheckUpDetailUiState(
    val checkUp: CheckUp? = null,
    val checkItems: List<CheckItem> = emptyList(),
    val spareParts: List<SparePart> = emptyList(),
    val progress: CheckUpProgress = CheckUpProgress(),
    val statistics: CheckUpStatistics = CheckUpStatistics(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUpdating: Boolean = false
) {
    val checkItemsByModule: Map<ModuleType, List<CheckItem>>
        get() = checkItems.groupBy { it.moduleType }
}

@HiltViewModel
class CheckUpDetailViewModel @Inject constructor(
    private val getCheckUpDetailsUseCase: GetCheckUpDetailsUseCase,
    private val updateCheckUpStatusUseCase: UpdateCheckUpStatusUseCase,
    private val exportCheckUpUseCase: ExportCheckUpUseCase
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
            try {
                Timber.d("Updating item status: $itemId -> $newStatus")

                // Optimistic update
                val currentItems = _uiState.value.checkItems
                val updatedItems = currentItems.map { item ->
                    if (item.id == itemId) {
                        item.copy(
                            status = newStatus,
                            checkedAt = if (newStatus != CheckItemStatus.PENDING) {
                                Clock.System.now()
                            } else null
                        )
                    } else item
                }

                // Update UI immediately
                val progress = calculateProgress(updatedItems)
                val statistics = calculateStatistics(updatedItems, _uiState.value.spareParts)

                _uiState.value = _uiState.value.copy(
                    checkItems = updatedItems,
                    progress = progress,
                    statistics = statistics
                )

                // TODO: Persist to database
                // updateCheckItemStatusUseCase(itemId, newStatus)

            } catch (e: Exception) {
                Timber.e(e, "Failed to update item status")
                // Revert optimistic update on error
                loadCheckUp(_uiState.value.checkUp?.id ?: "")
            }
        }
    }

    fun updateItemNotes(itemId: String, notes: String) {
        viewModelScope.launch {
            try {
                Timber.d("Updating item notes: $itemId")

                // Optimistic update
                val currentItems = _uiState.value.checkItems
                val updatedItems = currentItems.map { item ->
                    if (item.id == itemId) {
                        item.copy(notes = notes)
                    } else item
                }

                _uiState.value = _uiState.value.copy(checkItems = updatedItems)

                // TODO: Persist to database
                // updateCheckItemNotesUseCase(itemId, notes)

            } catch (e: Exception) {
                Timber.e(e, "Failed to update item notes")
                loadCheckUp(_uiState.value.checkUp?.id ?: "")
            }
        }
    }

    fun addSparePart() {
        // TODO: Implement spare part addition
        Timber.d("Add spare part requested")
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
                        // Reload to get updated status
                        loadCheckUp(checkUpId)
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
                        Timber.d("Report exported: __temp__") // (notImpl>>) ${exportedFile.filePath}")
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

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    private fun calculateProgress(checkItems: List<CheckItem>): CheckUpProgress {
        val totalItems = checkItems.size
        val completedItems = checkItems.count {
            it.status in listOf(CheckItemStatus.OK, CheckItemStatus.NOK, CheckItemStatus.NA)
        }

        val overallProgress = if (totalItems > 0) {
            completedItems.toFloat() / totalItems
        } else 0f

        // Calcola progresso per modulo usando il ModuleProgress reale (4 campi)
        val moduleProgress = checkItems.groupBy { it.moduleType }
            .mapValues { (_, items) ->
                val moduleCompleted = items.count {
                    it.status in listOf(CheckItemStatus.OK, CheckItemStatus.NOK, CheckItemStatus.NA)
                }
                val moduleCriticalIssues = items.count {
                    it.status == CheckItemStatus.NOK && it.criticality == CriticalityLevel.CRITICAL
                }
                val moduleProgressPercentage = if (items.isNotEmpty()) {
                    (moduleCompleted.toFloat() / items.size) * 100f
                } else 0f

                ModuleProgress(
                    totalItems = items.size,
                    completedItems = moduleCompleted,
                    criticalIssues = moduleCriticalIssues,
                    progressPercentage = moduleProgressPercentage
                )
            }
            .mapKeys { it.key.name }

        return CheckUpProgress(
            checkUpId = checkItems.firstOrNull()?.checkUpId ?: "",
            moduleProgress = moduleProgress,
            overallProgress = overallProgress,
            estimatedTimeRemaining = if (overallProgress > 0) {
                ((totalItems - completedItems) * 2) // Stima 2 minuti per item
            } else null
        )
    }

    private fun calculateStatistics(
        checkItems: List<CheckItem>,
        spareParts: List<SparePart>
    ): CheckUpStatistics {
        val completedItems = checkItems.count {
            it.status in listOf(CheckItemStatus.OK, CheckItemStatus.NOK, CheckItemStatus.NA)
        }
        val okItems = checkItems.count { it.status == CheckItemStatus.OK }
        val nokItems = checkItems.count { it.status == CheckItemStatus.NOK }
        val pendingItems = checkItems.count { it.status == CheckItemStatus.PENDING }
        val naItems = checkItems.count { it.status == CheckItemStatus.NA }
        val photosCount = checkItems.sumOf { it.photos.size }

        // Gestione sicura della criticità
        val criticalIssues = checkItems.count { item ->
            item.status == CheckItemStatus.NOK &&
                    (item.criticality == CriticalityLevel.CRITICAL)
        }
        val importantIssues = checkItems.count { item ->
            item.status == CheckItemStatus.NOK &&
                    (item.criticality == CriticalityLevel.IMPORTANT)
        }

        return CheckUpStatistics(
            totalItems = checkItems.size,
            completedItems = completedItems,
            okItems = okItems,
            nokItems = nokItems,
            pendingItems = pendingItems,
            naItems = naItems,
            criticalIssues = criticalIssues,
            importantIssues = importantIssues,
            photosCount = photosCount,
            sparePartsCount = spareParts.size,
            completionPercentage = if (checkItems.isNotEmpty()) (completedItems.toFloat() / checkItems.size) * 100f else 0f
        )
    }
}