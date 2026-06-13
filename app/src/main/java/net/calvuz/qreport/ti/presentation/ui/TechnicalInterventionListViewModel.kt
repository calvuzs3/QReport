@file:Suppress("HardCodedStringLiteral")
package net.calvuz.qreport.ti.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.calvuz.qreport.app.app.presentation.model.QReportFilter
import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.ti.domain.usecase.DeleteTechnicalInterventionUseCase
import net.calvuz.qreport.ti.domain.usecase.GetAllTechnicalInterventionsUseCase
import net.calvuz.qreport.ti.domain.usecase.UpdateTechnicalInterventionStatusUseCase
import net.calvuz.qreport.ti.domain.model.InterventionStatus
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention
import timber.log.Timber
import javax.inject.Inject
import kotlinx.datetime.Clock
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SelectionAction
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SimpleSelectionActionHandler
import net.calvuz.qreport.settings.data.local.AppSettingsDataStore
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.domain.repository.AppSettingsRepository

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
    val cardVariant: ListViewMode = ListViewMode.FULL
)

/** TechnicalInterventionListScreen Filter */
enum class InterventionFilter : QReportFilter {
    ACTIVE,         // DRAFT + IN_PROGRESS (default)
    COMPLETED,      // COMPLETED + ARCHIVED
    DRAFT,          // Only DRAFT
    IN_PROGRESS,    // Only IN_PROGRESS
    PENDING_REVIEW, // Only PENDING_REVIEW
    ALL;            // All statuses

    override fun getDisplayName(): UiText = when (this) {
        ACTIVE -> UiText.StringResource(R.string.interventions_filter_active)
        COMPLETED -> UiText.StringResource(R.string.interventions_filter_completed)
        DRAFT -> UiText.StringResource(R.string.interventions_filter_draft)
        IN_PROGRESS -> UiText.StringResource(R.string.interventions_filter_in_progress)
        PENDING_REVIEW -> UiText.StringResource(R.string.interventions_filter_pending_review)
        ALL -> UiText.StringResource(R.string.interventions_filter_all)
    }
}

/** TechnicalInterventionListScreen SortOrder */
enum class InterventionSortOrder : QReportSortOrder {
    UPDATED_RECENT,     // updatedAt DESC
    UPDATED_OLDEST,     // updatedAt ASC
    CREATED_RECENT,     // createdAt DESC
    CREATED_OLDEST,     // createdAt ASC
    CUSTOMER_NAME,      // customerData.customerName ASC
    INTERVENTION_NUMBER; // interventionNumber ASC

    override fun getDisplayName(): UiText = when (this) {
        UPDATED_RECENT -> UiText.StringResource(R.string.interventions_sort_updated_recent)
        UPDATED_OLDEST -> UiText.StringResource(R.string.interventions_sort_updated_oldest)
        CREATED_RECENT -> UiText.StringResource(R.string.interventions_sort_created_recent)
        CREATED_OLDEST -> UiText.StringResource(R.string.interventions_sort_created_oldest)
        CUSTOMER_NAME -> UiText.StringResource(R.string.interventions_sort_customer_name)
        INTERVENTION_NUMBER -> UiText.StringResource(R.string.interventions_sort_intervention_number)
    }
}

@HiltViewModel
class TechnicalInterventionListViewModel @Inject constructor(
    private val getAllTechnicalInterventionsUseCase: GetAllTechnicalInterventionsUseCase,
    private val deleteTechnicalInterventionUseCase: DeleteTechnicalInterventionUseCase,
    private val updateTechnicalInterventionStatusUseCase: UpdateTechnicalInterventionStatusUseCase,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TechnicalInterventionListUiState())
    val uiState: StateFlow<TechnicalInterventionListUiState> = _uiState.asStateFlow()

    // Tracks the active observation coroutine so switching filters never leaves stale collectors.
    private var loadJob: Job? = null

    companion object {
        const val KEY = AppSettingsDataStore.LIST_KEY_TI
    }

    init {
        observeCardVariant()
        loadInterventions()
    }

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    fun loadInterventions() {
        observeInterventions(_uiState.value.selectedFilter)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            Timber.d("Refreshing technical interventions")
            delay(500)
            observeInterventions(_uiState.value.selectedFilter, isRefresh = true)
        }
    }

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
        val filteredAndSorted = applyFiltersAndSort(
            currentState.interventions,
            currentState.searchQuery,
            filter,
            currentState.selectedSortOrder
        )

        _uiState.value = currentState.copy(
            selectedFilter = filter,
            filteredInterventions = filteredAndSorted
        )

        // Reload data for the new filter
        observeInterventions(filter)
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

    fun deleteInterventions(interventions: Set<TechnicalIntervention>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBatchOperating = true)

            try {
                var successCount = 0
                var failCount = 0

                interventions.forEach { intervention ->
                    when (deleteTechnicalInterventionUseCase(intervention.id)) {
                        is QrResult.Success -> {
                            successCount++
                            Timber.d("Deleted intervention: ${intervention.id}")
                        }
                        is QrResult.Error -> {
                            failCount++
                            Timber.e("Failed to delete intervention: ${intervention.id}")
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isBatchOperating = false,
                    successMessage = if (failCount == 0) {
                        UiText.StringResources(R.string.interventions_delete_success, successCount)
                    } else {
                        UiText.StringResources(R.string.interventions_delete_partial, successCount, failCount)
                    }
                )

                // Refresh list
                refresh()

            } catch (e: Exception) {
                Timber.e(e, "Exception deleting interventions")
                _uiState.value = _uiState.value.copy(
                    isBatchOperating = false,
                    error = UiText.StringResource(R.string.err_interventions_delete_failed)
                )
            }
        }
    }

    fun setActiveInterventions(interventions: Set<TechnicalIntervention>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBatchOperating = true)

            try {
                interventions.forEach { intervention ->
                    updateTechnicalInterventionStatusUseCase(
                        intervention.id,
                        InterventionStatus.IN_PROGRESS
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isBatchOperating = false,
                    successMessage = UiText.StringResources(
                        R.string.interventions_status_updated,
                        interventions.size
                    )
                )

                refresh()

            } catch (e: Exception) {
                Timber.e(e, "Exception setting interventions active")
                _uiState.value = _uiState.value.copy(
                    isBatchOperating = false,
                    error = UiText.StringResource(R.string.err_interventions_status_update_failed)
                )
            }
        }
    }

    fun setArchivedInterventions(interventions: Set<TechnicalIntervention>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBatchOperating = true)

            try {
                interventions.forEach { intervention ->
                    updateTechnicalInterventionStatusUseCase(
                        intervention.id,
                        InterventionStatus.ARCHIVED
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isBatchOperating = false,
                    successMessage = UiText.StringResources(
                        R.string.interventions_archived,
                        interventions.size
                    )
                )

                refresh()

            } catch (e: Exception) {
                Timber.e(e, "Exception archiving interventions")
                _uiState.value = _uiState.value.copy(
                    isBatchOperating = false,
                    error = UiText.StringResource(R.string.err_interventions_archive_failed)
                )
            }
        }
    }

    /**
     * Cycle through card display variants: FULL -> COMPACT -> MINIMAL -> FULL.
     * The preference is persisted via [AppSettingsRepository].
     */
    fun cycleCardVariant() {
        val current = _uiState.value.cardVariant
        val next = when (current) {
            ListViewMode.FULL -> ListViewMode.COMPACT
            ListViewMode.COMPACT -> ListViewMode.MINIMAL
            ListViewMode.MINIMAL -> ListViewMode.FULL
        }

        // Update UI immediately
        _uiState.value = _uiState.value.copy(cardVariant = next)

        // Persist in background
        viewModelScope.launch {
            try {
                appSettingsRepository.setListViewMode(
                    KEY,
                    next
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist card variant preference")
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun dismissSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    /**
     * Returns the Flow matching the given filter's status criteria.
     */
    private fun getInterventionsFlow(filter: InterventionFilter): Flow<QrResult<List<TechnicalIntervention>, QrError>> =
        when (filter) {
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

    /**
     * Observes technical interventions matching [filter].
     * Cancels any previous observation before starting a new one, so switching
     * filters or refreshing never leaves stale collectors running.
     */
    private fun observeInterventions(filter: InterventionFilter, isRefresh: Boolean = false) {
        loadJob?.cancel()

        val errorRes = if (isRefresh) {
            R.string.err_interventions_list_refresh_failed
        } else {
            R.string.err_interventions_list_load_failed
        }

        loadJob = viewModelScope.launch {
            if (!isRefresh) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            try {
                Timber.d("Observing technical interventions (filter=$filter)")

                getInterventionsFlow(filter)
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        Timber.e(exception, "Error in interventions flow")
                        if (currentCoroutineContext().isActive) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = UiText.StringResource(errorRes)
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
                                        error = UiText.StringResource(errorRes)
                                    )
                                }
                            }
                        }
                    }
            } catch (_: CancellationException) {
                Timber.d("Interventions observation cancelled")
            } catch (e: Exception) {
                Timber.e(e, "Exception observing interventions")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = UiText.StringResource(R.string.err_interventions_list_unexpected)
                )
            }
        }
    }

    /**
     * Observe the persisted card variant preference and apply it to UI state.
     */
    private fun observeCardVariant() {
        viewModelScope.launch {
            appSettingsRepository.getListViewMode(KEY)
                .catch { e ->
                    Timber.e(e, "Error observing card variant preference")
                }
                .collect { viewMode ->
                    _uiState.value = _uiState.value.copy(
                        cardVariant = viewMode
                    )
                }
        }
    }

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
    private val onDelete: () -> Unit,
    private val onSetActive: (Set<TechnicalIntervention>) -> Unit,
    private val onSetInactive: (Set<TechnicalIntervention>) -> Unit,
    private val onArchive: (Set<TechnicalIntervention>) -> Unit,
    private val onSelectAll: () -> Unit,
    private val onPerformDelete: (Set<TechnicalIntervention>) -> Unit
) : SimpleSelectionActionHandler<TechnicalIntervention> {

    override fun onActionClick(action: SelectionAction, selectedItems: Set<TechnicalIntervention>) {
        when (action) {
            SelectionAction.Edit -> onEdit(selectedItems)
            SelectionAction.Delete -> onDelete()
            SelectionAction.SetActive -> onSetActive(selectedItems)
            SelectionAction.SetInactive -> onSetInactive(selectedItems)
            SelectionAction.SelectAll -> onSelectAll()
            SelectionAction.Archive -> onArchive(selectedItems)

            is SelectionAction.Custom -> {
                // Handle any custom actions
                when (action.actionId) {
                    "duplicate" -> { /* handle duplicate */ }
                    "share" -> { /* handle share */ }
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
            SelectionAction.Archive -> selectedItems.isNotEmpty()
            SelectionAction.SelectAll -> true
            is SelectionAction.Custom -> true // Custom logic per action
            else -> false
        }
    }

    override fun getDeleteConfirmationMessage(selectedItems: Set<TechnicalIntervention>): UiText {
        return when (selectedItems.size) {
            1 -> UiText.StringResources(R.string.intervention_delete_precise_confirmation,
                selectedItems.first().interventionNumber)
            else -> UiText.StringResources(R.string.intervention_delete_confirmation,
                selectedItems.size)
        }
    }

    fun performDelete(selectedItems: Set<TechnicalIntervention>) {
        onPerformDelete(selectedItems)
    }
}