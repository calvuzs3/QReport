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
import net.calvuz.qreport.app.app.presentation.components.list.CardVariant
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention
// Selection system imports
import net.calvuz.qreport.app.app.presentation.components.simple_selection.DeleteConfirmationDialog
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SelectableItem
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SelectionTopBar
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SelectionAction
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SimpleSelectionManager
import net.calvuz.qreport.app.app.presentation.components.simple_selection.rememberSimpleSelectionManager
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.presentation.model.getCardVariantDescription
import net.calvuz.qreport.settings.presentation.model.getCardVariantIcon
import net.calvuz.qreport.ti.presentation.ui.components.TechnicalInterventionCard

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
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onNavigateToCreateIntervention: () -> Unit = {},
    onNavigateToEditIntervention: (String) -> Unit = {},
    viewModel: TechnicalInterventionListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Simple selection manager
    val selectionManager = rememberSimpleSelectionManager<TechnicalIntervention>()
    val selectionState by selectionManager.selectionState.collectAsState()

    // Delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Action handler for Technical Interventions
    val actionHandler = remember {
        TechnicalInterventionActionHandler(
            onEdit = { interventions ->
                if (interventions.size == 1) {
                    onNavigateToEditIntervention(interventions.first().id)
                    selectionManager.clearSelection()
                }
            },
            onDelete = {
                showDeleteDialog = true
            },
            onSetActive = { interventions ->
                viewModel.setActiveInterventions(interventions)
                selectionManager.clearSelection()
            },
            onSetInactive = { interventions ->
                viewModel.setArchivedInterventions(interventions)
                selectionManager.clearSelection()
            },
            onArchive = { interventions ->
                viewModel.setArchivedInterventions(interventions)
                selectionManager.clearSelection()
            },
            onSelectAll = {
                selectionManager.selectAll(uiState.filteredInterventions.map { it.intervention })
            },
            onPerformDelete = { interventions ->
                viewModel.deleteInterventions(interventions)
                selectionManager.clearSelection()
            }
        )
    }

    // Define actions
    val primaryActions = listOf(
        SelectionAction.Edit,
        SelectionAction.Delete
    )

    val secondaryActions = listOf(
        SelectionAction.SelectAll,
        SelectionAction.SetActive,
        SelectionAction.Archive,
    )

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

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column {
            SelectionTopBar(
                normalTopBar = {
                    // Top App Bar with selection-aware title and debug mode toggle
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = if (selectionState.isInSelectionMode) {
                                        stringResource(
                                            R.string.selection_summary,
                                            selectionState.selectedCount
                                        )
                                    } else {
                                        stringResource(R.string.interventions_screen_title)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    if (selectionState.isInSelectionMode) {
                                        selectionManager.clearSelection()
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
                                        stringResource(R.string.action_exit_selection)
                                    } else {
                                        stringResource(R.string.action_back)
                                    }
                                )
                            }
                        },
                        actions = {
                            if (!selectionState.isInSelectionMode) {
                                var showFilterMenu by remember { mutableStateOf(false) }
                                var showSortMenu by remember { mutableStateOf(false) }

                                // View mode toggle button
                                IconButton(onClick = viewModel::cycleCardVariant) {
                                    Icon(
                                        imageVector = uiState.cardVariant.getCardVariantIcon(),
                                        contentDescription = uiState.cardVariant.getCardVariantDescription()
                                    )
                                }

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

                                // Sort menu
                                SortMenu(
                                    expanded = showSortMenu,
                                    selectedSort = uiState.selectedSortOrder,
                                    onSortSelected = viewModel::updateSortOrder,
                                    onDismiss = { showSortMenu = false }
                                )

                                // Filter menu
                                FilterMenu(
                                    expanded = showFilterMenu,
                                    selectedFilter = uiState.selectedFilter,
                                    onFilterSelected = viewModel::updateFilter,
                                    onDismiss = { showFilterMenu = false }
                                )
                            }
                        }
                    )
                },
                selectionManager = selectionManager,
                primaryActions = primaryActions,
                secondaryActions = secondaryActions,
                actionHandler = actionHandler
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
                            variant = uiState.cardVariant,
                            selectionManager = selectionManager,
                            onNavigateToEditIntervention = onNavigateToEditIntervention,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Pull to refresh indicator
        if (pullToRefreshState.isRefreshing || uiState.isLoading) {
            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

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

        // Delete confirmation dialog
        DeleteConfirmationDialog(
            isVisible = showDeleteDialog,
            selectedItems = selectionState.selectedItems,
            actionHandler = actionHandler,
            onConfirm = {
                actionHandler.performDelete(selectionState.selectedItems)
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )

        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Interventions list with selection support
 */
@Composable
private fun InterventionsListWithSelection(
    interventionsWithStats: List<TechnicalInterventionWithStats>,
    selectionManager: SimpleSelectionManager<TechnicalIntervention>,
    onNavigateToEditIntervention: (String) -> Unit,
    variant: ListViewMode,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier= modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = interventionsWithStats,
            key = { it.intervention.id }
        ) { interventionWithStats ->
            SelectableItem(
                item = interventionWithStats.intervention,
                selectionManager = selectionManager,
                onNormalClick = { intervention ->
                    onNavigateToEditIntervention(intervention.id)
                }
            ) { isSelected ->
                // Your existing TechnicalInterventionCard
                TechnicalInterventionCard(
                    modifier = Modifier.fillMaxWidth(),
                    intervention = interventionWithStats.intervention,
                    stats = interventionWithStats.stats,
                    isSelected = isSelected,
                    variant = variant
                )
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