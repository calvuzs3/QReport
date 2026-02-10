package net.calvuz.qreport.client.contract.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
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
import net.calvuz.qreport.app.app.presentation.components.ActiveFiltersChipRow
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.app.app.presentation.components.ErrorState
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.app.app.presentation.components.QReportSearchBar
import net.calvuz.qreport.app.app.presentation.components.list.CardVariant
import net.calvuz.qreport.app.error.presentation.UiText
// Selection system imports
import net.calvuz.qreport.app.app.presentation.components.simple_selection.DeleteConfirmationDialog
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SelectableItem
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SelectionTopBar
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SelectionAction
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SimpleSelectionManager
import net.calvuz.qreport.app.app.presentation.components.simple_selection.rememberSimpleSelectionManager
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.presentation.ui.components.ContractCard
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.presentation.model.getCardVariantDescription
import net.calvuz.qreport.settings.presentation.model.getCardVariantIcon

@Composable
fun ContractFilter.getDisplayName(): UiText {
    return when (this) {
        ContractFilter.ALL -> (UiText.StringResource(R.string.contracts_list_filter_all))
        ContractFilter.ACTIVE -> (UiText.StringResource(R.string.contracts_list_filter_active))
        ContractFilter.INACTIVE -> (UiText.StringResource(R.string.contracts_list_filter_inactive))
    }
}

@Composable
fun ContractSortOrder.getDisplayName(): UiText {
    return when (this) {
        ContractSortOrder.NAME -> UiText.StringResource(R.string.contracts_list_sort_name)
        ContractSortOrder.EXPIRE_OLDEST -> UiText.StringResource(R.string.contracts_list_sort_expire_oldest)
        ContractSortOrder.EXPIRE_RECENT -> UiText.StringResource(R.string.contracts_list_sort_expire_recent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractListScreen(
    modifier: Modifier = Modifier,
    clientId: String,
    clientName: String,
    onNavigateBack: () -> Unit,
    onNavigateToCreateContract: (String) -> Unit,
    onNavigateToEditContract: (String) -> Unit,
    onNavigateToContractDetail: (String) -> Unit,
    onNavigateToRenewContract: (String) -> Unit = {},
    onNavigateToBulkEdit: (List<String>) -> Unit = {},
    viewModel: ContractListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Simple selection manager
    val selectionManager = rememberSimpleSelectionManager<Contract>()
    val selectionState by selectionManager.selectionState.collectAsState()

    // Delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Action handler for Technical Interventions
    val actionHandler = remember {
        ContractActionHandler(
            onEdit = { contracts ->
                if (contracts.size == 1) {
                    onNavigateToEditContract(contracts.first().id)
                    selectionManager.clearSelection()
                }
            },
            onDelete = {
                showDeleteDialog = true
            },
            onRenew = { contracts ->
                if (contracts.size == 1) {
                    viewModel.renew(contracts.first().id)
                    selectionManager.clearSelection()
                }
            },
            onSetActive = { contracts ->
                viewModel.setActive(contracts, true)
                selectionManager.clearSelection()
            },
            onSetInactive = { contracts ->
                viewModel.setActive(contracts, false)
                selectionManager.clearSelection()
            },
            onArchive = { contracts ->
                viewModel.setActive(contracts, false)
                selectionManager.clearSelection()
            },
//            onExport = { contracts ->
//                viewModel.exportInterventions(contracts)
//                selectionManager.clearSelection()
//            },
            onSelectAll = {
                selectionManager.selectAll(uiState.filteredContracts.map { it.contract })
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
        SelectionAction.Renew,
        SelectionAction.SetActive,
        SelectionAction.SetInactive,
        SelectionAction.Archive,
//        SelectionAction.Export,
//        SelectionAction.MarkCompleted
    )

    // Content with Pull to Refresh
    val pullToRefreshState = rememberPullToRefreshState()

    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }

    // Reset refresh state when not refreshing
    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing && pullToRefreshState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }

    // Handle pull to refresh
    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            viewModel.refresh()
        }
    }

    // Clear selection when navigating away or data changes significantly
    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            selectionManager.clearSelection()
        }
    }


    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMessage ->
            snackbarHostState.showSnackbar(
                message = errorMessage.asString(context),
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
            viewModel.dismissSuccess()
        }
    }

    // Load contracts when screen opens
    LaunchedEffect(clientId) {
        viewModel.initializeForClient(clientId)
    }


    Box(modifier = modifier.fillMaxSize()) {
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
                                        stringResource(R.string.contracts_screen_list_title)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!selectionState.isInSelectionMode) {
                                    Text(
                                        text = clientName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
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
                                        stringResource(R.string.clear_selection)
                                    } else {
                                        stringResource(R.string.action_back)
                                    }
                                )
                            }
                        },
                        actions = {
                            if (!selectionState.isInSelectionMode) {
                                // Normal mode - show filter and sort
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
                },
                selectionManager = selectionManager,
                actionHandler = actionHandler,
                primaryActions = primaryActions,
                secondaryActions = secondaryActions
            )

            if (!selectionState.isInSelectionMode) {
                // Search bar
                QReportSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    placeholder = stringResource(R.string.contracts_screen_list_search_contract_placeholder),
                    modifier = Modifier.padding(16.dp)
                )

                // Filter chips
                if (uiState.selectedFilter != ContractFilter.ALL || uiState.selectedSortOrder != ContractSortOrder.NAME) {
                    ActiveFiltersChipRow(
                        selectedFilter = uiState.selectedFilter.getDisplayName().asString(),
                        avoidFilter = ContractFilter.ACTIVE.getDisplayName().asString(),
                        selectedSort = uiState.selectedSortOrder.getDisplayName().asString(),
                        avoidSort = ContractSortOrder.EXPIRE_RECENT.getDisplayName().asString(),
                        onClearFilter = { viewModel.updateFilter(ContractFilter.ACTIVE) },
                        onClearSort = { viewModel.updateSortOrder(ContractSortOrder.EXPIRE_RECENT) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Content area with pull-to-refresh
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(pullToRefreshState.nestedScrollConnection)
            ) {
                val currentError = uiState.error

                when {
                    uiState.isLoading -> {
                        LoadingState()
                    }

                    currentError != null -> {
                        ErrorState(
                            error = currentError.asString(),
                            onRetry = viewModel::refresh,
                            onDismiss = viewModel::dismissError
                        )
                    }

                    uiState.filteredContracts.isEmpty() -> {
                        val (title, message) = when {
                            uiState.contracts.isEmpty() ->
                                stringResource(R.string.contracts_screen_list_empty_title) to stringResource(
                                    R.string.contracts_screen_list_empty_message
                                )

                            uiState.selectedFilter != ContractFilter.ALL ->
                                stringResource(R.string.contracts_screen_list_empty_no_results_title) to
                                        stringResource(
                                            R.string.contracts_screen_list_empty_no_result_message,
                                            uiState.selectedFilter.getDisplayName().asString()
                                        )

                            else -> stringResource(R.string.contracts_screen_list_empty_error_title) to
                                    stringResource(R.string.checkup_screen_list_empty_error_message)
                        }
                        EmptyState(
                            textTitle = title,
                            textMessage = message,
                            iconImageVector = Icons.Outlined.AssignmentTurnedIn,
                            iconContentDescription = stringResource(R.string.contracts_screen_list_empty_icon_content_description),
                            iconActionImageVector = Icons.Default.Add,
                            iconActionContentDescription = stringResource(R.string.contracts_screen_list_empty_action_add),
                            textAction = stringResource(R.string.contracts_screen_list_empty_action_add),
                            onAction = { onNavigateToCreateContract(clientId) }
                        )
                    }

                    else -> {
                        // Contracts list - NO padding bottom because floating indicator doesn't cover the list
                        ContractsListWithSelection(
                            contractsWithStats = uiState.filteredContracts,
                            variant = uiState.cardVariant,
                            selectionManager = selectionManager,
                            onNavigateToEdit = onNavigateToEditContract
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

        // FAB for new contract (hidden in selection mode)
        if (!selectionState.isInSelectionMode) {
            FloatingActionButton(
                onClick = { onNavigateToCreateContract(clientId) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.contracts_screen_list_empty_action_add)
                )
            }
        }

        // Delete confirmation dialog
        DeleteConfirmationDialog(
            isVisible = showDeleteDialog,
            selectedItems = selectionState.selectedItems,
            actionHandler = actionHandler,
            onConfirm = {
                actionHandler.onActionClick(SelectionAction.Delete, selectionState.selectedItems)
                selectionManager.clearSelection()
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
 * Contracts list with selection support (unchanged)
 */
@Composable
private fun ContractsListWithSelection(
    contractsWithStats: List<ContractWithStats>,
    selectionManager: SimpleSelectionManager<Contract>,
    onNavigateToEdit: (String) -> Unit,
    variant: ListViewMode,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = contractsWithStats,
            key = { it.contract.id }
        ) { contractWithStats ->
            SelectableItem(
                item = contractWithStats.contract,
                selectionManager = selectionManager,
                onNormalClick = { contract ->
                    onNavigateToEdit(contract.id)
                },
            ) { isSelected ->

                ContractCard(
                    modifier = Modifier.fillMaxWidth(),
                    contract = contractWithStats.contract,
                    stats = contractWithStats.stats,
                    isSelected = isSelected,
                    variant = variant
                )
            }
        }
    }
}


// Keep existing filter and sort menus unchanged
@Composable
private fun FilterMenu(
    expanded: Boolean,
    selectedFilter: ContractFilter,
    onFilterSelected: (ContractFilter) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        ContractFilter.entries.forEach { filter ->
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
    selectedSort: ContractSortOrder,
    onSortSelected: (ContractSortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        ContractSortOrder.entries.forEach { sortOrder ->
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