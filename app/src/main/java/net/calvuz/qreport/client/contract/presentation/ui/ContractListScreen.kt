package net.calvuz.qreport.client.contract.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.app.app.presentation.components.QReportErrorState
import net.calvuz.qreport.app.app.presentation.components.QReportFilterMenu
import net.calvuz.qreport.app.app.presentation.components.QReportFiltersChipRow
import net.calvuz.qreport.app.app.presentation.components.QReportPullToRefresh
import net.calvuz.qreport.app.app.presentation.components.QReportSearchBar
import net.calvuz.qreport.app.app.presentation.components.QReportSelectorRow
import net.calvuz.qreport.app.app.presentation.components.QReportSortOrderMenu
import net.calvuz.qreport.app.app.presentation.components.QrLoadingState
import net.calvuz.qreport.app.app.presentation.components.simple_selection.*
import net.calvuz.qreport.client.client.presentation.model.ClientPkg
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.presentation.model.ContractFilter
import net.calvuz.qreport.client.contract.presentation.model.ContractPkg
import net.calvuz.qreport.client.contract.presentation.model.ContractSortOrder
import net.calvuz.qreport.client.contract.presentation.ui.components.ContractCard
import net.calvuz.qreport.client.facility.presentation.model.ClientOption
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.presentation.model.getCardVariantDescription
import net.calvuz.qreport.settings.presentation.model.getCardVariantIcon

@Suppress("ParamsComparedByRef")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractListScreen(
    modifier: Modifier = Modifier,
    clientId: String? = null,
    clientName: String = "",
    onNavigateBack: () -> Unit,
    onNavigateToCreateContract: (String) -> Unit,
    onNavigateToEditContract: (String) -> Unit,
    viewModel: ContractListViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current

    val selectionManager = rememberSimpleSelectionManager<Contract>()
    val selectionState by selectionManager.selectionState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(clientId) {
        if (!clientId.isNullOrBlank()) viewModel.initializeForClient(clientId)
        else viewModel.initialize()
    }

    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) selectionManager.clearSelection()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it.asString(context), duration = SnackbarDuration.Long)
            viewModel.dismissError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it.asString(context), duration = SnackbarDuration.Short)
            viewModel.dismissSuccess()
        }
    }

    val actionHandler = remember (uiState.filteredContracts){
        ContractActionHandler(
            onEdit = { contracts ->
                if (contracts.size == 1) { onNavigateToEditContract(contracts.first().id); selectionManager.clearSelection() }
            },
            onDelete = { showDeleteDialog = true },
            onPerformDelete = { contracts ->
                viewModel.bulkDeleteContracts(contracts.map { it.id })
                selectionManager.clearSelection()
            },
            onRenew = { contracts ->
                if (contracts.size == 1) { viewModel.renew(contracts.first().id); selectionManager.clearSelection() }
            },
            onSetActive = { viewModel.setActive(it, true); selectionManager.clearSelection() },
            onSetInactive = { viewModel.setActive(it, false); selectionManager.clearSelection() },
            onArchive = { viewModel.setActive(it, false); selectionManager.clearSelection() },
            onSelectAll = { selectionManager.selectAll(uiState.filteredContracts.map { it.contract }) }
        )
    }

    val primaryActions = listOf(SelectionAction.Edit, SelectionAction.Delete)
    val secondaryActions = listOf(
        SelectionAction.SelectAll, SelectionAction.Renew,
        SelectionAction.SetActive, SelectionAction.SetInactive, SelectionAction.Archive
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column {
            SelectionTopBar(
                normalTopBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = stringResource(R.string.contracts_screen_list_title),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!selectionState.isInSelectionMode) {
                                    Text(
                                        text = uiState.selectedClient
                                            .takeIf { it != ClientOption.ALL }?.companyName
                                            ?: clientName.takeIf { it.isNotBlank() }
                                            ?: stringResource(R.string.contracts_screen_list_subtitle_all),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (selectionState.isInSelectionMode) selectionManager.clearSelection()
                                else onNavigateBack()
                            }) {
                                Icon(
                                    imageVector = if (selectionState.isInSelectionMode) Icons.Default.Close else Icons.Default.ArrowBackIosNew,
                                    contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        },
                        actions = {
                            if (!selectionState.isInSelectionMode) {
                                var showFilterMenu by remember { mutableStateOf(false) }
                                var showSortMenu by remember { mutableStateOf(false) }

                                IconButton(onClick = viewModel::cycleCardVariant) {
                                    Icon(uiState.cardVariant.getCardVariantIcon(), uiState.cardVariant.getCardVariantDescription())
                                }
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Default.Sort, stringResource(R.string.label_ordering))
                                }
                                IconButton(onClick = { showFilterMenu = true }) {
                                    Icon(Icons.Default.FilterList, stringResource(R.string.label_filtering))
                                }
                                QReportFilterMenu(expanded = showFilterMenu, entries = ContractFilter.entries, selectedFilter = uiState.selectedFilter, onFilterSelected = viewModel::updateFilter, onDismiss = { showFilterMenu = false })
                                QReportSortOrderMenu(expanded = showSortMenu, entries = ContractSortOrder.entries, selectedSortOrder = uiState.selectedSortOrder, onSortOrderSelected = viewModel::updateSortOrder, onDismiss = { showSortMenu = false })
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
                QReportSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    placeholder = stringResource(R.string.contracts_screen_list_search_contract_placeholder),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                QReportSelectorRow(
                    entries = uiState.availableClients,
                    selectedItem = uiState.selectedClient,
                    onItemSelected = viewModel::updateSelectedClient,
                    icon = ClientPkg.icon,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                if (uiState.selectedFilter != ContractPkg.selectedFilter || uiState.selectedSortOrder != ContractPkg.selectedSortOrder) {
                    QReportFiltersChipRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        selectedFilter = uiState.selectedFilter,
                        avoidFilter = ContractPkg.selectedFilter,
                        onClearFilter = { viewModel.updateFilter(ContractPkg.selectedFilter) },
                        selectedSort = uiState.selectedSortOrder,
                        avoidSort = ContractPkg.selectedSortOrder,
                        onClearSort = { viewModel.updateSortOrder(ContractPkg.selectedSortOrder) }
                    )
                }
            }

            QReportPullToRefresh(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                val currentError = uiState.error
                when {
                    uiState.isLoading -> QrLoadingState()

                    currentError != null -> QReportErrorState(
                        error = currentError,
                        onRetry = viewModel::refresh,
                        onDismiss = viewModel::dismissError
                    )

                    uiState.filteredContracts.isEmpty() -> {
                        val (title, message) = when {
                            uiState.contracts.isEmpty() ->
                                stringResource(R.string.contracts_screen_list_empty_title) to
                                        stringResource(R.string.contracts_screen_list_empty_message)
                            uiState.selectedFilter != ContractFilter.ALL ->
                                stringResource(R.string.contracts_screen_list_empty_no_results_title) to
                                        stringResource(R.string.contracts_screen_list_empty_no_result_message,
                                            uiState.selectedFilter.getDisplayName().asString())
                            else ->
                                stringResource(R.string.contracts_screen_list_empty_error_title) to
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
                            onAction = { onNavigateToCreateContract(uiState.clientId) }
                        )
                    }

                    else -> ContractsListWithSelection(
                        contractsWithStats = uiState.filteredContracts,
                        variant = uiState.cardVariant,
                        selectionManager = selectionManager,
                        onNavigateToEdit = onNavigateToEditContract
                    )
                }
            }
        }

        if (!selectionState.isInSelectionMode) {
            FloatingActionButton(
                onClick = { onNavigateToCreateContract(uiState.clientId) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.contracts_screen_list_empty_action_add))
            }
        }

        DeleteConfirmationDialog(
            isVisible = showDeleteDialog,
            selectedItems = selectionState.selectedItems,
            actionHandler = actionHandler,
            onConfirm = {
                actionHandler.onPerformDelete(selectionState.selectedItems)
                selectionManager.clearSelection()
            },
            onDismiss = { showDeleteDialog = false }
        )

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Suppress("ParamsComparedByRef")
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
        items(items = contractsWithStats, key = { it.contract.id }) { cws ->
            SelectableItem(item = cws.contract, selectionManager = selectionManager, onNormalClick = { onNavigateToEdit(it.id) }) { isSelected ->
                ContractCard(modifier = Modifier.fillMaxWidth(), contract = cws.contract, stats = cws.stats, isSelected = isSelected, variant = variant)
            }
        }
    }
}