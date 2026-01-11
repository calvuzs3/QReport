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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.R
import net.calvuz.qreport.client.contract.presentation.ui.components.ContractCard
import net.calvuz.qreport.app.app.presentation.components.ActiveFiltersChipRow
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.app.app.presentation.components.ErrorState
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.app.app.presentation.components.QReportSearchBar
import net.calvuz.qreport.app.app.presentation.components.list.QrListItemCard.QrListItemCardVariant
import net.calvuz.qreport.app.error.presentation.UiText
// Selection system imports
import net.calvuz.qreport.app.app.presentation.components.selection.*
import timber.log.Timber

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
    clientId: String,
    clientName: String,
    onNavigateBack: () -> Unit,
    onNavigateToCreateContract: (String) -> Unit,
    onNavigateToEditContract: (String) -> Unit,
    onNavigateToContractDetail: (String) -> Unit,
    onNavigateToRenewContract: (String) -> Unit = {},
    onNavigateToBulkEdit: (List<String>) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ContractListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Selection manager for multi-select functionality
    val selectionManager = rememberSelectionManager<String>()
    val selectionState by selectionManager.selectionState.collectAsState()

    // Bottom sheet state
    val bottomSheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    // Batch delete dialog state
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    // Load contracts when screen opens
    LaunchedEffect(clientId) {
        viewModel.initializeForClient(clientId)
    }

    // Clear selection when navigating away or data changes significantly
    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            selectionManager.clearSelection()
        }
    }

    // Close bottom sheet when selection is cleared
    LaunchedEffect(selectionState.isInSelectionMode) {
        if (!selectionState.isInSelectionMode) {
            showBottomSheet = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column {
            // Top App Bar with selection-aware title
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectionState.isInSelectionMode) {
                                stringResource(R.string.selection_summary, selectionState.selectedCount)
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

            // Content with Pull to Refresh
            val pullToRefreshState = rememberPullToRefreshState()

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
                            selectionManager = selectionManager,
                            onNavigateToDetail = onNavigateToContractDetail
                        )
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
            }
        }

        // Floating selection indicator (appears above the list, doesn't cover it)
        FloatingSelectionIndicator(
            selectionState = selectionState,
            onOpenBottomSheet = {
                showBottomSheet = true
            },
            onClearSelection = {
                selectionManager.clearSelection()
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Selection Bottom Sheet (opens on demand)
        if (showBottomSheet) {
            SelectionBottomSheet(
                selectionState = selectionState,
                availableActions = BatchActionSets.contractActions,
                batchActionHandler = createContractBatchActionHandler(
                    viewModel = viewModel,
                    onBulkEdit = onNavigateToBulkEdit,
                    onRenewContract = onNavigateToRenewContract,
                    onShowBatchDeleteDialog = { showBatchDeleteDialog = true },
                    contractsWithStats = uiState.filteredContracts,
                    selectionManager = selectionManager
                ),
                onDismiss = {
                    showBottomSheet = false
                },
                sheetState = bottomSheetState
            )
        }

        // Batch delete confirmation dialog (same as before)
        BatchDeleteConfirmationDialog(
            isVisible = showBatchDeleteDialog,
            selectedItems = selectionState.selectedItems,
            batchActionHandler = createContractBatchActionHandler(
                viewModel = viewModel,
                onBulkEdit = onNavigateToBulkEdit,
                onRenewContract = onNavigateToRenewContract,
                onShowBatchDeleteDialog = { showBatchDeleteDialog = true },
                contractsWithStats = uiState.filteredContracts,
                selectionManager = selectionManager
            ),
            onConfirm = {
                // Perform batch delete
                val selectedContracts = selectionState.selectedItems
                selectedContracts.forEach { contractId ->
                    viewModel.delete(contractId)
                }
                selectionManager.clearSelection()
                showBottomSheet = false
            },
            onDismiss = { showBatchDeleteDialog = false }
        )
    }
}

/**
 * Contracts list with selection support (unchanged)
 */
@Composable
private fun ContractsListWithSelection(
    contractsWithStats: List<ContractWithStats>,
    selectionManager: SelectionManager<String>,
    onNavigateToDetail: (String) -> Unit,
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
            SelectableListItem(
                item = contractWithStats.contract.id,
                selectionManager = selectionManager,
                onNavigateToItem = { onNavigateToDetail(it) }
            ) { contractId ->
                // Find the contract data
                val contract = contractsWithStats.find { it.contract.id == contractId }?.contract
                if (contract != null) {
                    ContractCard(
                        contract = contract,
                        onClick = {}, // Handled by SelectableListItem
                        onEdit = null,
                        onDelete = null,
                        variant = QrListItemCardVariant.FULL
                    )
                }
            }
        }
    }
}

/**
 * Creates batch action handler for contracts (same as before)
 */
@Composable
private fun createContractBatchActionHandler(
    viewModel: ContractListViewModel,
    onBulkEdit: (List<String>) -> Unit,
    onRenewContract: (String) -> Unit,
    onShowBatchDeleteDialog: () -> Unit,
    contractsWithStats: List<ContractWithStats>,
    selectionManager: SelectionManager<String>
): BatchActionHandler<String> {
    return remember {
        object : BatchActionHandler<String> {
            override fun onBatchAction(action: BatchAction, selectedItems: Set<String>) {
                Timber.d("🔥 Batch action: $action for ${selectedItems.size} contracts")

                when (action) {
                    BatchAction.SelectAll -> {
                        val allContractIds = contractsWithStats.map { it.contract.id }
                        selectionManager.selectAll(allContractIds)
                    }
                    BatchAction.Edit -> {
                        onBulkEdit(selectedItems.toList())
                    }
                    BatchAction.Delete -> {
                        onShowBatchDeleteDialog()
                    }
                    BatchAction.ContractBatchAction.Renew -> {
                        // Renew all selected contracts
                        selectedItems.forEach { contractId ->
                            onRenewContract(contractId)
                        }
                        selectionManager.clearSelection()
                    }
                    BatchAction.ContractBatchAction.BulkEdit -> {
                        onBulkEdit(selectedItems.toList())
                    }
                    BatchAction.Export -> {
                        // TODO: Implement batch export
                        Timber.d("Batch export for ${selectedItems.size} contracts")
                        selectionManager.clearSelection()
                    }
                    else -> {
                        Timber.w("Unsupported batch action: $action")
                    }
                }
            }

            override fun isBatchActionAvailable(action: BatchAction, selectedItems: Set<String>): Boolean {
                return when (action) {
                    BatchAction.ContractBatchAction.Renew -> {
                        // Only available if all selected contracts are eligible for renewal
                        selectedItems.all { contractId ->
                            viewModel.isContractEligibleForRenewal(contractId)
                        }
                    }
                    BatchAction.SelectAll -> {
                        // Available if not all items are selected
                        selectedItems.size < contractsWithStats.size
                    }
                    else -> selectedItems.isNotEmpty()
                }
            }

            override fun getBatchDeleteConfirmationMessage(selectedItems: Set<String>): UiText {
                return UiText.StringResources(R.string.contracts_screen_list_bulk_delete_action_confirm, selectedItems.size)
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