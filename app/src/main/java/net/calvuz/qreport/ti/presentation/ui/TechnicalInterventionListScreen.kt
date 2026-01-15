package net.calvuz.qreport.ti.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.app.app.presentation.components.QReportSearchBar
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention
import net.calvuz.qreport.ti.domain.model.InterventionStatus
// Selection system imports
import net.calvuz.qreport.app.app.presentation.components.selection.*
import net.calvuz.qreport.ti.domain.model.WorkLocationType
import timber.log.Timber

@Composable
fun InterventionFilter.getDisplayName(): UiText {
    return when (this) {
        InterventionFilter.ALL -> UiText.StringResource(R.string.interventions_filter_all)
        InterventionFilter.ACTIVE -> UiText.StringResource(R.string.interventions_filter_active)
        InterventionFilter.COMPLETED -> UiText.StringResource(R.string.interventions_filter_completed)
        InterventionFilter.DRAFT -> UiText.StringResource(R.string.interventions_filter_draft)
        InterventionFilter.IN_PROGRESS -> UiText.StringResource(R.string.interventions_filter_in_progress)
        InterventionFilter.PENDING_REVIEW -> UiText.StringResource(R.string.interventions_filter_pending_review)
    }
}

@Composable
fun InterventionSortOrder.getDisplayName(): UiText {
    return when (this) {
        InterventionSortOrder.UPDATED_RECENT -> UiText.StringResource(R.string.interventions_sort_updated_recent)
        InterventionSortOrder.UPDATED_OLDEST -> UiText.StringResource(R.string.interventions_sort_updated_oldest)
        InterventionSortOrder.CREATED_RECENT -> UiText.StringResource(R.string.interventions_sort_created_recent)
        InterventionSortOrder.CREATED_OLDEST -> UiText.StringResource(R.string.interventions_sort_created_oldest)
        InterventionSortOrder.CUSTOMER_NAME -> UiText.StringResource(R.string.interventions_sort_customer_name)
        InterventionSortOrder.INTERVENTION_NUMBER -> UiText.StringResource(R.string.interventions_sort_intervention_number)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicalInterventionListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateIntervention: () -> Unit = {},
    onNavigateToEditIntervention: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TechnicalInterventionListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Selection manager for multi-select functionality
    val selectionState by viewModel.selectionManager.selectionState.collectAsState()

    // Bottom sheet state
    val bottomSheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    // Batch delete dialog state
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    // Pull to refresh state
    val pullToRefreshState = rememberPullToRefreshState()

    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle pull to refresh
    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }

    // Clear selection when navigating away or data changes significantly
    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            viewModel.selectionManager.clearSelection()
        }
    }

    // Close bottom sheet when selection is cleared
    LaunchedEffect(selectionState.isInSelectionMode) {
        if (!selectionState.isInSelectionMode) {
            showBottomSheet = false
        }
    }

    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error.asString(context),
                duration = SnackbarDuration.Long
            )
            viewModel.dismissError()
        }
    }

    // Show success messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message.asString(context),
                duration = SnackbarDuration.Short
            )
            viewModel.dismissSuccessMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column {
            // Top App Bar with selection-aware title and debug mode toggle
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectionState.isInSelectionMode) {
                                stringResource(R.string.selection_summary, selectionState.selectedCount)
                            } else {
                                stringResource(R.string.interventions_screen_title)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!selectionState.isInSelectionMode && uiState.debugMode) {
                            Text(
                                text = stringResource(R.string.debug_mode_active),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectionState.isInSelectionMode) {
                                viewModel.selectionManager.clearSelection()
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (selectionState.isInSelectionMode) {
                                Icons.Default.Close
                            } else {
                                Icons.Default.ArrowBackIosNew
                            },
                            contentDescription = if (selectionState.isInSelectionMode) {
                                stringResource(R.string.clear_selection)
                            } else {
                                stringResource(R.string.action_back)
                            }
                        )
                    }
                },
                actions = {
                    if (!selectionState.isInSelectionMode) {
                        // Debug mode toggle
                        IconButton(
                            onClick = { viewModel.toggleDebugMode() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = stringResource(R.string.debug_mode_toggle),
                                tint = if (uiState.debugMode) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        // Normal mode - show filter and sort
                        var showFilterMenu by remember { mutableStateOf(false) }
                        var showSortMenu by remember { mutableStateOf(false) }

                        // Sort button
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.Sort,
                                contentDescription = stringResource(R.string.label_ordering)
                            )
                        }

                        // Filter button
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.label_filtering)
                            )
                        }

                        // Filter menu
                        FilterMenu(
                            expanded = showFilterMenu,
                            selectedFilter = uiState.selectedFilter,
                            onFilterSelected = viewModel::updateFilter,
                            onDismiss = { showFilterMenu = false }
                        )

                        // Sort menu
                        SortMenu(
                            expanded = showSortMenu,
                            selectedSort = uiState.selectedSortOrder,
                            onSortSelected = viewModel::updateSortOrder,
                            onDismiss = { showSortMenu = false }
                        )
                    }
                }
            )

            if (!selectionState.isInSelectionMode) {
                // Search bar
                QReportSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    placeholder = stringResource(R.string.interventions_search_placeholder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Content area with pull-to-refresh
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(pullToRefreshState.nestedScrollConnection)
            ) {
                when {
                    uiState.isLoading -> {
                        LoadingState(
                            message = stringResource(R.string.interventions_loading),
                        )
                    }

                    uiState.filteredInterventions.isEmpty() && uiState.searchQuery.isBlank() -> {
                        EmptyState(
                            textTitle = stringResource(R.string.interventions_empty_title),
                            textMessage = stringResource(R.string.interventions_empty_description),
                            textAction = stringResource(R.string.interventions_create_first),
                            onAction = onNavigateToCreateIntervention,
                            modifier = Modifier.fillMaxSize(),
                            iconImageVector = Icons.Default.Workspaces,
                            iconContentDescription = stringResource(R.string.interventions_create_first),
                        )
                    }

                    uiState.filteredInterventions.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                        EmptyState(
                            textTitle = stringResource(R.string.interventions_search_empty_title),
                            textMessage = stringResource(
                                R.string.interventions_search_empty_description,
                                uiState.searchQuery
                            ),
                            modifier = Modifier.fillMaxSize(),
                            iconImageVector = Icons.Default.Workspaces
                        )
                    }

                    else -> {
                        InterventionsListWithSelection(
                            interventionsWithStats = uiState.filteredInterventions,
                            selectionManager = viewModel.selectionManager,
                            onNavigateToEdit = onNavigateToEditIntervention,
                            isDeleting = uiState.isDeleting,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Pull to refresh indicator
                PullToRefreshContainer(
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = pullToRefreshState,
                )
            }
        }

        // Floating Action Button for creating new intervention
        if (!selectionState.isInSelectionMode) {
            FloatingActionButton(
                onClick = onNavigateToCreateIntervention,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.interventions_create_new)
                )
            }
        }

        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Batch actions bottom sheet
        if (showBottomSheet && selectionState.isInSelectionMode) {
            BatchActionsBottomSheet(
                selectedItems = selectionState.selectedItems,
                availableActions = viewModel.getAvailableBatchActions(),
                batchActionHandler = createInterventionBatchActionHandler(
                    viewModel = viewModel,
                    onNavigateToEdit = onNavigateToEditIntervention,
                    onShowBatchDeleteDialog = { showBatchDeleteDialog = true },
                    interventionsWithStats = uiState.filteredInterventions
                ),
                getBatchDeleteConfirmationMessage = { selectedItems ->
                    UiText.StringResource(R.string.interventions_batch_delete_confirm, selectedItems.size)
                },
                onDismiss = {
                    showBottomSheet = false
                },
                sheetState = bottomSheetState
            )
        }

        // Batch delete confirmation dialog
        BatchDeleteConfirmationDialog(
            isVisible = showBatchDeleteDialog,
            selectedItems = selectionState.selectedItems,
            batchActionHandler = createInterventionBatchActionHandler(
                viewModel = viewModel,
                onNavigateToEdit = onNavigateToEditIntervention,
                onShowBatchDeleteDialog = { showBatchDeleteDialog = true },
                interventionsWithStats = uiState.filteredInterventions
            ),
            onConfirm = {
                // Perform batch delete through ViewModel
                val selectedInterventions = selectionState.selectedItems
                viewModel.handleBatchAction(BatchAction.Delete, selectedInterventions)
                showBatchDeleteDialog = false
                showBottomSheet = false
            },
            onDismiss = { showBatchDeleteDialog = false }
        )
    }

    // Handle selection-triggered bottom sheet
    LaunchedEffect(selectionState.isInSelectionMode, selectionState.selectedCount) {
        if (selectionState.isInSelectionMode && selectionState.selectedCount > 0) {
            showBottomSheet = true
        }
    }
}

/**
 * Interventions list with selection support
 */
@Composable
private fun InterventionsListWithSelection(
    interventionsWithStats: List<TechnicalInterventionWithStats>,
    selectionManager: SelectionManager<TechnicalIntervention>,
    onNavigateToEdit: (String) -> Unit,
    isDeleting: String?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = interventionsWithStats,
            key = { it.intervention.id }
        ) { interventionWithStats ->
            SelectableListItem(
                item = interventionWithStats.intervention,
                selectionManager = selectionManager,
                onNavigateToItem = { intervention ->
                    // Edit only if single selection, no navigation for normal click
                    if (selectionManager.currentState.selectedItems.size == 1) {
                        onNavigateToEdit(intervention.id)
                    }
                }
            ) { intervention ->
                TechnicalInterventionCard(
                    intervention = intervention,
                    stats = interventionWithStats.stats,
                    onClick = {}, // Handled by SelectableListItem
                    onEdit = if (selectionManager.currentState.selectedItems.size == 1) {
                        { onNavigateToEdit(intervention.id) }
                    } else null,
                    onDelete = null, // Handled by batch operations
                    isDeleting = isDeleting == intervention.id
                )
            }
        }
    }
}

/**
 * Technical Intervention Card Component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TechnicalInterventionCard(
    intervention: TechnicalIntervention,
    stats: InterventionStatistics,
    onClick: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    isDeleting: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row: Intervention number + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = intervention.interventionNumber,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                InterventionStatusChip(status = intervention.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer information
            Text(
                text = intervention.customerData.customerName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (intervention.customerData.customerContact.isNotBlank()) {
                Text(
                    text = intervention.customerData.customerContact,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Robot information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.robot_serial_number),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = intervention.robotData.serialNumber,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = stringResource(R.string.operating_hours),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${intervention.robotData.hoursOfDuty} h",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Updated date + Work location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.updated_days_ago,
                        stats.daysSinceLastUpdate
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = when (intervention.workLocation.type) {
                        WorkLocationType.CLIENT_SITE ->
                            stringResource(R.string.work_location_client_site)
                        WorkLocationType.OUR_SITE ->
                            stringResource(R.string.work_location_our_site)
                        WorkLocationType.OTHER ->
                            intervention.workLocation.customLocation.ifBlank {
                                stringResource(R.string.work_location_other)
                            }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Loading indicator for deletion
            if (isDeleting) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Status chip component
 */
@Composable
private fun InterventionStatusChip(
    status: InterventionStatus,
    modifier: Modifier = Modifier
) {
    val (text, containerColor, contentColor) = when (status) {
        InterventionStatus.DRAFT -> Triple(
            stringResource(R.string.status_draft),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        InterventionStatus.IN_PROGRESS -> Triple(
            stringResource(R.string.status_in_progress),
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary
        )
        InterventionStatus.PENDING_REVIEW -> Triple(
            stringResource(R.string.status_pending_review),
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary
        )
        InterventionStatus.COMPLETED -> Triple(
            stringResource(R.string.status_completed),
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary
        )
        InterventionStatus.ARCHIVED -> Triple(
            stringResource(R.string.status_archived),
            MaterialTheme.colorScheme.outline,
            MaterialTheme.colorScheme.onSurface
        )
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Creates batch action handler for technical interventions
 */
@Composable
private fun createInterventionBatchActionHandler(
    viewModel: TechnicalInterventionListViewModel,
    onNavigateToEdit: (String) -> Unit,
    onShowBatchDeleteDialog: () -> Unit,
    interventionsWithStats: List<TechnicalInterventionWithStats>
): BatchActionHandler<TechnicalIntervention> {
    return remember {
        object : BatchActionHandler<TechnicalIntervention> {
            override fun onBatchAction(action: BatchAction, selectedItems: Set<TechnicalIntervention>) {
                Timber.d("🔥 Batch action: $action for ${selectedItems.size} interventions")

                when (action) {
                    BatchAction.SelectAll -> {
                        val allInterventions = interventionsWithStats.map { it.intervention }
                        viewModel.selectionManager.selectAll(allInterventions)
                    }
                    BatchAction.Edit -> {
                        if (selectedItems.size == 1) {
                            onNavigateToEdit(selectedItems.first().id)
                        }
                    }
                    BatchAction.Delete -> {
                        onShowBatchDeleteDialog()
                    }
                    else -> {
                        // Delegate other actions to ViewModel
                        viewModel.handleBatchAction(action, selectedItems)
                    }
                }
            }

            override fun isBatchActionAvailable(action: BatchAction, selectedItems: Set<TechnicalIntervention>): Boolean {
                return when (action) {
                    BatchAction.Edit -> {
                        // Edit only available for single selection
                        selectedItems.size == 1
                    }
                    BatchAction.SelectAll -> {
                        // Available if not all items are selected
                        selectedItems.size < interventionsWithStats.size
                    }
                    else -> selectedItems.isNotEmpty()
                }
            }

            override fun getBatchDeleteConfirmationMessage(selectedItems: Set<TechnicalIntervention>): UiText {
                return UiText.StringResources(R.string.interventions_batch_delete_confirm, selectedItems.size)
            }
        }
    }
}

// Filter and sort menus
@Composable
private fun FilterMenu(
    expanded: Boolean,
    selectedFilter: InterventionFilter,
    onFilterSelected: (InterventionFilter) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        InterventionFilter.entries.forEach { filter ->
            DropdownMenuItem(
                text = { Text(filter.getDisplayName().asString()) },
                onClick = {
                    onFilterSelected(filter)
                    onDismiss()
                },
                leadingIcon = if (selectedFilter == filter) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}

@Composable
private fun SortMenu(
    expanded: Boolean,
    selectedSort: InterventionSortOrder,
    onSortSelected: (InterventionSortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        InterventionSortOrder.entries.forEach { sortOrder ->
            DropdownMenuItem(
                text = { Text(sortOrder.getDisplayName().asString()) },
                onClick = {
                    onSortSelected(sortOrder)
                    onDismiss()
                },
                leadingIcon = if (selectedSort == sortOrder) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}