package net.calvuz.qreport.ti.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.ti.domain.usecase.DeleteTechnicalInterventionUseCase
import net.calvuz.qreport.ti.domain.usecase.GetAllTechnicalInterventionsUseCase
import net.calvuz.qreport.ti.domain.usecase.UpdateTechnicalInterventionStatusUseCase
import net.calvuz.qreport.ti.domain.model.InterventionStatus
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention
import net.calvuz.qreport.app.app.presentation.components.selection.SelectionManager
import timber.log.Timber
import javax.inject.Inject
import kotlinx.datetime.Clock
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SelectionAction
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SimpleSelectionActionHandler

/** TechnicalInterventionListScreen UiState */
data class TechnicalInterventionListUiState(
    val interventions: List<TechnicalInterventionWithStats> = emptyList(),
    val filteredInterventions: List<TechnicalInterventionWithStats> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDeleting: String? = null,
    val isBatchOperating: Boolean = false,
    val error: UiText? = null,
    val successMessage: UiText? = null,
    val searchQuery: String = "",
    val selectedFilter: InterventionFilter = InterventionFilter.ACTIVE,
    val selectedSortOrder: InterventionSortOrder = InterventionSortOrder.UPDATED_RECENT,
    val debugMode: Boolean = false
)

/** TechnicalInterventionListScreen Filter */
enum class InterventionFilter {
    ACTIVE,         // DRAFT + IN_PROGRESS (default)
    COMPLETED,      // COMPLETED + ARCHIVED
    DRAFT,          // Only DRAFT
    IN_PROGRESS,    // Only IN_PROGRESS
    PENDING_REVIEW, // Only PENDING_REVIEW
    ALL             // All statuses
}

/** TechnicalInterventionListScreen SortOrder */
enum class InterventionSortOrder {
    UPDATED_RECENT,     // updatedAt DESC
    UPDATED_OLDEST,     // updatedAt ASC
    CREATED_RECENT,     // createdAt DESC
    CREATED_OLDEST,     // createdAt ASC
    CUSTOMER_NAME,      // customerData.customerName ASC
    INTERVENTION_NUMBER // interventionNumber ASC
}

@HiltViewModel
class TechnicalInterventionListViewModel @Inject constructor(
    private val getAllTechnicalInterventionsUseCase: GetAllTechnicalInterventionsUseCase,
    private val deleteTechnicalInterventionUseCase: DeleteTechnicalInterventionUseCase,
    private val updateTechnicalInterventionStatusUseCase: UpdateTechnicalInterventionStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TechnicalInterventionListUiState())
    val uiState: StateFlow<TechnicalInterventionListUiState> = _uiState.asStateFlow()

    val selectionManager = SelectionManager<TechnicalIntervention>()

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    init {
        loadInterventions()
    }

    fun loadInterventions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                Timber.d("Loading technical interventions")

                // Use default filter (ACTIVE) to get DRAFT + IN_PROGRESS
                getAllTechnicalInterventionsUseCase.getActiveInterventions()
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        Timber.e(exception, "Error in interventions flow")
                        if (currentCoroutineContext().isActive) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = UiText.StringResource(R.string.err_interventions_list_load_failed)
                            )
                        }
                    }
                    .collect { result ->
                        if (!currentCoroutineContext().isActive) {
                            Timber.d("Skipping interventions processing - job cancelled")
                            return@collect
                        }

                        when (result) {
                            is QrResult.Success -> {
                                val interventions = result.data

                                // Enrich with statistics
                                val interventionsWithStats = enrichWithStatistics(interventions)

                                if (currentCoroutineContext().isActive) {
                                    val currentState = _uiState.value
                                    val filteredAndSorted = applyFiltersAndSort(
                                        interventionsWithStats,
                                        currentState.searchQuery,
                                        currentState.selectedFilter,
                                        currentState.selectedSortOrder
                                    )

                                    _uiState.value = currentState.copy(
                                        interventions = interventionsWithStats,
                                        filteredInterventions = filteredAndSorted,
                                        isLoading = false,
                                        isRefreshing = false,
                                        error = null
                                    )

                                    Timber.d("Loaded ${interventions.size} interventions successfully")
                                }
                            }

                            is QrResult.Error -> {
                                if (currentCoroutineContext().isActive) {
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        isRefreshing = false,
                                        error = UiText.StringResource(R.string.err_interventions_list_load_failed)
                                    )
                                }
                            }
                        }
                    }
            } catch (_: CancellationException) {
                Timber.d("Interventions loading cancelled")
            } catch (e: Exception) {
                Timber.e(e, "Exception loading interventions")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiText.StringResource(R.string.err_interventions_list_unexpected)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

                Timber.d("Refreshing technical interventions")
                delay(500)

                // Use current filter for refresh
                val currentFilter = _uiState.value.selectedFilter
                val useCase = when (currentFilter) {
                    InterventionFilter.ACTIVE -> getAllTechnicalInterventionsUseCase.getActiveInterventions()
                    InterventionFilter.COMPLETED -> getAllTechnicalInterventionsUseCase.getCompletedInterventions()
                    InterventionFilter.DRAFT -> getAllTechnicalInterventionsUseCase.getInterventionsByStatus(
                        InterventionStatus.DRAFT
                    )

                    InterventionFilter.IN_PROGRESS -> getAllTechnicalInterventionsUseCase.getInterventionsByStatus(
                        InterventionStatus.IN_PROGRESS
                    )

                    InterventionFilter.PENDING_REVIEW -> getAllTechnicalInterventionsUseCase.getInterventionsByStatus(
                        InterventionStatus.PENDING_REVIEW
                    )

                    InterventionFilter.ALL -> getAllTechnicalInterventionsUseCase()
                }

                useCase.collect { result ->
                    when (result) {
                        is QrResult.Success -> {
                            val interventions = result.data

                            if (!currentCoroutineContext().isActive) {
                                Timber.d("Skipping refresh processing - job cancelled")
                                return@collect
                            }

                            val interventionsWithStats = enrichWithStatistics(interventions)

                            if (currentCoroutineContext().isActive) {
                                val currentState = _uiState.value
                                val filteredAndSorted = applyFiltersAndSort(
                                    interventionsWithStats,
                                    currentState.searchQuery,
                                    currentState.selectedFilter,
                                    currentState.selectedSortOrder
                                )

                                _uiState.value = currentState.copy(
                                    interventions = interventionsWithStats,
                                    filteredInterventions = filteredAndSorted,
                                    isRefreshing = false,
                                    error = null
                                )

                                Timber.d("Interventions refresh completed successfully")
                                return@collect // Exit after first success
                            }
                        }

                        is QrResult.Error -> {
                            if (currentCoroutineContext().isActive) {
                                Timber.e("Failed to refresh interventions: ${result.error}")
                                _uiState.value = _uiState.value.copy(
                                    isRefreshing = false,
                                    error = UiText.StringResource(R.string.err_interventions_list_refresh_failed)
                                )
                                return@collect
                            }
                        }
                    }
                }
            } catch (_: CancellationException) {
                Timber.d("Refresh cancelled")
                if (currentCoroutineContext().isActive) {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e, "Failed to refresh interventions")
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = UiText.StringResource(R.string.err_interventions_list_refresh_unexpected)
                    )
                }
            }
        }
    }

    fun deleteIntervention(interventionId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isDeleting = interventionId,
                    error = null
                )

                val result = deleteTechnicalInterventionUseCase(
                    interventionId = interventionId,
                    forceDelete = _uiState.value.debugMode
                )

                when (result) {
                    is QrResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            successMessage = UiText.StringResource(R.string.intervention_deleted_success)
                        )
                        // Refresh to remove deleted item
                        refresh()
                    }

                    is QrResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = UiText.StringResource(R.string.err_intervention_delete_failed)
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception deleting intervention")
                _uiState.value = _uiState.value.copy(
                    error = UiText.StringResource(R.string.err_intervention_delete_unexpected)
                )
            } finally {
                _uiState.value = _uiState.value.copy(isDeleting = null)
            }
        }
    }

    // ===== Batch Operations =====

    private fun setStatus(interventions: Set<TechnicalIntervention>, status: InterventionStatus) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isBatchOperating = true,
                    error = null
                )

                val interventionIds = interventions.map { it.id }
                val result = updateTechnicalInterventionStatusUseCase.updateStatusBatch(
                    interventionIds = interventionIds,
                    newStatus = status,
                    debugMode = _uiState.value.debugMode
                )

                when (result) {
                    is QrResult.Success -> {
                        val summary = result.data
                        _uiState.value = _uiState.value.copy(
                            successMessage = UiText.StringResources(
                                R.string.interventions_status_updated_success,
                                summary.successCount
                            )
                        )
                        selectionManager.clearSelection()
                        refresh()
                    }

                    is QrResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = UiText.StringResource(R.string.err_interventions_status_update_failed)
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception in batch status update")
                _uiState.value = _uiState.value.copy(
                    error = UiText.StringResource(R.string.err_interventions_status_update_unexpected)
                )
            } finally {
                _uiState.value = _uiState.value.copy(isBatchOperating = false)
            }
        }
    }

    fun setActiveInterventions(interventions: Set<TechnicalIntervention>) {
        return setStatus(interventions, InterventionStatus.IN_PROGRESS)
    }

    fun setArchivedInterventions(interventions: Set<TechnicalIntervention>) {
        return setStatus(interventions, InterventionStatus.ARCHIVED)
    }

    fun exportInterventions(interventions: Set<TechnicalIntervention>) {
        // TODO: Implement batch export functionality
        Timber.d("Export batch requested for ${interventions.size} interventions")
        _uiState.value = _uiState.value.copy(
            successMessage = UiText.StringResource(R.string.export_feature_coming_soon)
        )
    }

    private fun exportBatch(interventions: Set<TechnicalIntervention>) {
        // TODO: Implement batch export functionality
        Timber.d("Export batch requested for ${interventions.size} interventions")
        _uiState.value = _uiState.value.copy(
            successMessage = UiText.StringResource(R.string.export_feature_coming_soon)
        )
    }


    // ===== Search and filter logic =====

    fun updateSearchQuery(query: String) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.interventions,
            query,
            currentState.selectedFilter,
            currentState.selectedSortOrder
        )

        _uiState.value = currentState.copy(
            searchQuery = query,
            filteredInterventions = filteredAndSorted
        )
    }

    fun updateFilter(filter: InterventionFilter) {
        val currentState = _uiState.value

        // If filter changed, reload data
        if (filter != currentState.selectedFilter) {
            _uiState.value = currentState.copy(selectedFilter = filter)
            loadFilteredInterventions(filter)
        }
    }

    private fun loadFilteredInterventions(filter: InterventionFilter) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val useCase = when (filter) {
                    InterventionFilter.ACTIVE -> getAllTechnicalInterventionsUseCase.getActiveInterventions()
                    InterventionFilter.COMPLETED -> getAllTechnicalInterventionsUseCase.getCompletedInterventions()
                    InterventionFilter.DRAFT -> getAllTechnicalInterventionsUseCase.getInterventionsByStatus(
                        InterventionStatus.DRAFT
                    )

                    InterventionFilter.IN_PROGRESS -> getAllTechnicalInterventionsUseCase.getInterventionsByStatus(
                        InterventionStatus.IN_PROGRESS
                    )

                    InterventionFilter.PENDING_REVIEW -> getAllTechnicalInterventionsUseCase.getInterventionsByStatus(
                        InterventionStatus.PENDING_REVIEW
                    )

                    InterventionFilter.ALL -> getAllTechnicalInterventionsUseCase()
                }

                useCase.collect { result ->
                    when (result) {
                        is QrResult.Success -> {
                            val interventions = result.data
                            val interventionsWithStats = enrichWithStatistics(interventions)

                            val currentState = _uiState.value
                            val filteredAndSorted = applyFiltersAndSort(
                                interventionsWithStats,
                                currentState.searchQuery,
                                currentState.selectedFilter,
                                currentState.selectedSortOrder
                            )

                            _uiState.value = currentState.copy(
                                interventions = interventionsWithStats,
                                filteredInterventions = filteredAndSorted,
                                isLoading = false,
                                error = null
                            )
                            return@collect // Exit after first success
                        }

                        is QrResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = UiText.StringResource(R.string.err_interventions_filter_failed)
                            )
                            return@collect
                        }
                    }
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiText.StringResource(R.string.err_interventions_filter_unexpected)
                )
            }
        }
    }

    fun updateSortOrder(sortOrder: InterventionSortOrder) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.interventions,
            currentState.searchQuery,
            currentState.selectedFilter,
            sortOrder
        )

        _uiState.value = currentState.copy(
            selectedSortOrder = sortOrder,
            filteredInterventions = filteredAndSorted
        )
    }

    fun toggleDebugMode() {
        val newDebugMode = !_uiState.value.debugMode
        _uiState.value = _uiState.value.copy(debugMode = newDebugMode)

        // Clear selection when toggling debug mode to refresh batch actions
        selectionManager.clearSelection()

        Timber.d("Debug mode ${if (newDebugMode) "enabled" else "disabled"}")
    }

    // ===== Error handling =====

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun dismissSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    private fun enrichWithStatistics(interventions: List<TechnicalIntervention>): List<TechnicalInterventionWithStats> {
        return interventions.map { intervention ->
            val stats = try {
                InterventionStatistics(
                    isOverdue = intervention.status == InterventionStatus.PENDING_REVIEW,
                    daysSinceLastUpdate = kotlin.math.abs(
                        (Clock.System.now() - intervention.updatedAt).inWholeDays
                    ).toInt(),
                    canBeDeleted = intervention.status in listOf(
                        InterventionStatus.DRAFT,
                        InterventionStatus.IN_PROGRESS
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception getting stats for intervention ${intervention.id}")
                InterventionStatistics(
                    isOverdue = false,
                    daysSinceLastUpdate = 0,
                    canBeDeleted = true
                )
            }

            TechnicalInterventionWithStats(intervention = intervention, stats = stats)
        }
    }

    private fun applyFiltersAndSort(
        interventions: List<TechnicalInterventionWithStats>,
        searchQuery: String,
        filter: InterventionFilter,
        sortOrder: InterventionSortOrder
    ): List<TechnicalInterventionWithStats> {
        var filtered = interventions

        // Apply search query
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { interventionWithStats ->
                val intervention = interventionWithStats.intervention
                val customerNameMatch = intervention.customerData.customerName
                    .contains(searchQuery, ignoreCase = true)
                val ticketNumberMatch = intervention.customerData.ticketNumber
                    .contains(searchQuery, ignoreCase = true)
                val serialNumberMatch = intervention.robotData.serialNumber
                    .contains(searchQuery, ignoreCase = true)
                val interventionNumberMatch = intervention.interventionNumber
                    .contains(searchQuery, ignoreCase = true)

                customerNameMatch || ticketNumberMatch || serialNumberMatch || interventionNumberMatch
            }
        }

        // Apply status filter (already filtered at data level, but double-check for client-side filtering)
        filtered = when (filter) {
            InterventionFilter.ALL -> filtered
            InterventionFilter.ACTIVE -> filtered.filter {
                it.intervention.status in listOf(
                    InterventionStatus.DRAFT,
                    InterventionStatus.IN_PROGRESS
                )
            }

            InterventionFilter.COMPLETED -> filtered.filter {
                it.intervention.status in listOf(
                    InterventionStatus.COMPLETED,
                    InterventionStatus.ARCHIVED
                )
            }

            InterventionFilter.DRAFT -> filtered.filter { it.intervention.status == InterventionStatus.DRAFT }
            InterventionFilter.IN_PROGRESS -> filtered.filter { it.intervention.status == InterventionStatus.IN_PROGRESS }
            InterventionFilter.PENDING_REVIEW -> filtered.filter { it.intervention.status == InterventionStatus.PENDING_REVIEW }
        }

        // Apply sorting
        filtered = when (sortOrder) {
            InterventionSortOrder.UPDATED_RECENT -> filtered.sortedByDescending { it.intervention.updatedAt }
            InterventionSortOrder.UPDATED_OLDEST -> filtered.sortedBy { it.intervention.updatedAt }
            InterventionSortOrder.CREATED_RECENT -> filtered.sortedByDescending { it.intervention.createdAt }
            InterventionSortOrder.CREATED_OLDEST -> filtered.sortedBy { it.intervention.createdAt }
            InterventionSortOrder.CUSTOMER_NAME -> filtered.sortedBy { it.intervention.customerData.customerName }
            InterventionSortOrder.INTERVENTION_NUMBER -> filtered.sortedBy { it.intervention.interventionNumber }
        }

        return filtered
    }
}

// ============================================================
// DATA CLASSES
// ============================================================

data class TechnicalInterventionWithStats(
    val intervention: TechnicalIntervention,
    val stats: InterventionStatistics
)

data class InterventionStatistics(
    val isOverdue: Boolean,
    val daysSinceLastUpdate: Int,
    val canBeDeleted: Boolean
)


/**
 * Technical Intervention specific action handler
 */
class TechnicalInterventionActionHandler(
    private val onEdit: (Set<TechnicalIntervention>) -> Unit,
    private val onDelete: (Set<TechnicalIntervention>) -> Unit,
    private val onSetActive: (Set<TechnicalIntervention>) -> Unit,
    private val onSetInactive: (Set<TechnicalIntervention>) -> Unit,
    private val onArchive: (Set<TechnicalIntervention>) -> Unit,
//    private val onExport: (Set<TechnicalIntervention>) -> Unit,
    private val onSelectAll: () -> Unit
) : SimpleSelectionActionHandler<TechnicalIntervention> {

    override fun onActionClick(action: SelectionAction, selectedItems: Set<TechnicalIntervention>) {
        when (action) {
            SelectionAction.Edit -> onEdit(selectedItems)
            SelectionAction.Delete -> onDelete(selectedItems)
            SelectionAction.SetActive -> onSetActive(selectedItems)
            SelectionAction.SetInactive -> onSetInactive(selectedItems)
//            SelectionAction.Export -> onExport(selectedItems)
            SelectionAction.SelectAll -> onSelectAll()
            SelectionAction.Archive -> {onArchive(selectedItems)}


//            SelectionAction.MarkCompleted -> {
//                // Handle mark completed - set status to COMPLETED
//                // You would implement this in ViewModel
//            }

            is SelectionAction.Custom -> {
                // Handle any custom actions
                when (action.actionId) {
                    "duplicate" -> { /* handle duplicate */
                    }

                    "share" -> { /* handle share */
                    }
                    // etc.
                }
            }

            else -> {}
        }
    }

    override fun isActionEnabled(
        action: SelectionAction,
        selectedItems: Set<TechnicalIntervention>
    ): Boolean {
        return when (action) {
            SelectionAction.Edit -> selectedItems.size == 1 // Edit only for single selection
            SelectionAction.Delete -> selectedItems.isNotEmpty() && selectedItems.all {
                // Can delete only DRAFT and IN_PROGRESS
                it.status in listOf(InterventionStatus.DRAFT, InterventionStatus.IN_PROGRESS)
            }

            SelectionAction.SetActive -> selectedItems.isNotEmpty()
            SelectionAction.SetInactive -> selectedItems.isNotEmpty()
//            SelectionAction.Export -> selectedItems.isNotEmpty()
            SelectionAction.Archive -> selectedItems.isNotEmpty()
//            SelectionAction.MarkCompleted -> selectedItems.isNotEmpty()
            SelectionAction.SelectAll -> true
            is SelectionAction.Custom -> true // Custom logic per action
            else -> false
        }
    }

    override fun getDeleteConfirmationMessage(selectedItems: Set<TechnicalIntervention>): String {
        return when (selectedItems.size) {
            1 -> "Delete intervention ${selectedItems.first().interventionNumber}?"
            else -> "Delete ${selectedItems.size} interventions?"
        }
    }
}