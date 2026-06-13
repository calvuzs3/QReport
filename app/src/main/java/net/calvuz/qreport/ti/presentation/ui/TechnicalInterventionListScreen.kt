package net.calvuz.qreport.ti.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.app.app.presentation.components.QReportErrorState
import net.calvuz.qreport.app.app.presentation.components.QReportFilterMenu
import net.calvuz.qreport.app.app.presentation.components.QReportFiltersChipRow
import net.calvuz.qreport.app.app.presentation.components.QReportSortOrderMenu
import net.calvuz.qreport.app.app.presentation.components.QrLoadingState
import net.calvuz.qreport.app.app.presentation.components.QReportPullToRefresh
import net.calvuz.qreport.app.app.presentation.components.QReportSearchBar
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

@Suppress("ParamsComparedByRef")
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

    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }

    // Clear selection when navigating away or data changes significantly
    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            selectionManager.clearSelection()
        }
    }

    // Show error messages (as snackbar only when data is already available;
    // otherwise a full-screen error state is shown instead)
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            if (uiState.interventions.isNotEmpty()) {
                snackbarHostState.showSnackbar(
                    message = error.asString(context),
                    duration = SnackbarDuration.Long
                )
                viewModel.dismissError()
            }
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
                                QReportSortOrderMenu(
                                    expanded = showSortMenu,
                                    entries = InterventionSortOrder.entries,
                                    selectedSortOrder = uiState.selectedSortOrder,
                                    onSortOrderSelected = viewModel::updateSortOrder,
                                    onDismiss = { showSortMenu = false }
                                )

                                // Filter menu
                                QReportFilterMenu(
                                    expanded = showFilterMenu,
                                    entries = InterventionFilter.entries,
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

                // Summary row: count + create new intervention button
                InterventionsSummaryRow(
                    count = uiState.filteredInterventions.size,
                    onCreateNewIntervention = onNavigateToCreateIntervention
                )

                // Active filters/sort chip row
                if (uiState.selectedFilter != InterventionFilter.ACTIVE ||
                    uiState.selectedSortOrder != InterventionSortOrder.UPDATED_RECENT
                ) {
                    QReportFiltersChipRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        selectedFilter = uiState.selectedFilter,
                        avoidFilter = InterventionFilter.ACTIVE,
                        onClearFilter = { viewModel.updateFilter(InterventionFilter.ACTIVE) },
                        selectedSort = uiState.selectedSortOrder,
                        avoidSort = InterventionSortOrder.UPDATED_RECENT,
                        onClearSort = { viewModel.updateSortOrder(InterventionSortOrder.UPDATED_RECENT) },
                    )
                }
            }

            // Content area with pull-to-refresh
            QReportPullToRefresh(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {

                val currentError = uiState.error

                when {
                    uiState.isLoading -> {
                        QrLoadingState(
                            message = stringResource(R.string.interventions_loading),
                        )
                    }

                    currentError != null && uiState.interventions.isEmpty() -> {
                        QReportErrorState(
                            error = currentError,
                            onRetry = viewModel::loadInterventions,
                            onDismiss = viewModel::dismissError
                        )
                    }

                    uiState.filteredInterventions.isEmpty() && uiState.searchQuery.isBlank() -> {
                        EmptyState(
                            textTitle = stringResource(R.string.interventions_empty_title),
                            textMessage = stringResource(R.string.interventions_empty_description),
                            iconImageVector = Icons.Default.Workspaces,
                            iconContentDescription = stringResource(R.string.interventions_create_first),
                            iconActionImageVector = Icons.Default.Add,
                            iconActionContentDescription = stringResource(R.string.interventions_create_first),
                            textAction = stringResource(R.string.interventions_create_first),
                            onAction = onNavigateToCreateIntervention,
                        )
                    }

                    uiState.filteredInterventions.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                        EmptyState(
                            textTitle = stringResource(R.string.interventions_search_empty_title),
                            textMessage = stringResource(R.string.interventions_search_empty_description,uiState.searchQuery),
                            iconImageVector = Icons.Default.Workspaces,
                            iconContentDescription = stringResource(R.string.interventions_create_first),
                            iconActionImageVector = Icons.Default.Add,
                            iconActionContentDescription = stringResource(R.string.interventions_create_first),
                            textAction = stringResource(R.string.interventions_create_first),
                            onAction = onNavigateToCreateIntervention
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
 * Summary row shown below the search bar: intervention count + create new button.
 */
@Composable
private fun InterventionsSummaryRow(
    count: Int,
    onCreateNewIntervention: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.interventions_screen_title_count, count),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Button(onClick = onCreateNewIntervention) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.interventions_create_new))
        }
    }
}

/**
 * Interventions list with selection support
 */
@Suppress("ParamsComparedByRef")
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
