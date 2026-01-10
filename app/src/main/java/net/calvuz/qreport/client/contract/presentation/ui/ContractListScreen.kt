@file:OptIn(ExperimentalFoundationApi::class)

package net.calvuz.qreport.client.contract.presentation.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import net.calvuz.qreport.client.contract.presentation.ui.components.QrListItemCardVariant
import net.calvuz.qreport.app.app.presentation.components.ActiveFiltersChipRow
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.app.app.presentation.components.ErrorState
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.app.app.presentation.components.QReportSearchBar
import net.calvuz.qreport.app.app.presentation.components.contextmenu.CommonActionSets
import net.calvuz.qreport.app.app.presentation.components.contextmenu.DeleteDialogConfig
import net.calvuz.qreport.app.app.presentation.components.contextmenu.ListItemAction
import net.calvuz.qreport.app.app.presentation.components.contextmenu.ListItemActionHandler
import net.calvuz.qreport.app.app.presentation.components.contextmenu.ListItemWithContextMenu
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.util.DateTimeUtils.isPast
import timber.log.Timber
import kotlin.time.Duration.Companion.days

@Composable
fun ContractFilter.getDisplayName(): UiText {
    return when (this) {
        ContractFilter.ALL -> (UiText.StringResource(R.string.contract_filter_all))
        ContractFilter.ACTIVE -> (UiText.StringResource(R.string.contract_filter_active))
        ContractFilter.INACTIVE -> (UiText.StringResource(R.string.contract_filter_not_active))
    }
}

@Composable
fun ContractSortOrder.getDisplayName(): UiText {
    return when (this) {
        ContractSortOrder.NAME -> UiText.StringResource(R.string.contract_sort_order_name)
        ContractSortOrder.EXPIRE_OLDEST -> UiText.StringResource(R.string.contract_sort_order_expire_oldest)
        ContractSortOrder.EXPIRE_RECENT -> UiText.StringResource(R.string.contract_sort_order_expire_recent)
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
    onNavigateToRenewContract: (String) -> Unit= {},
    modifier: Modifier = Modifier,
    viewModel: ContractListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load contacts when screen opens
    LaunchedEffect(clientId) {
        viewModel.initializeForClient(clientId)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.contracts_screen_list_title), //"Contratti",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = clientName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            },
            actions = {
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
        )

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
            // Store error in local variable to avoid smart cast issues
            val currentError = uiState.error

            when {
                uiState.isLoading -> {
                    LoadingState()
                }

                currentError != null -> {
                    ErrorState(
                        error = currentError,
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
                    ContractsListContentWithGenericMenu(
                        contractsWithStats = uiState.filteredContracts,
                        onClick = onNavigateToContractDetail,
                        onEdit = onNavigateToEditContract,
                        onDelete = viewModel::delete,
                        onViewDetails = onNavigateToContractDetail,
                        onRenewContract = onNavigateToRenewContract
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

            // FAB per nuovo contatto
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

/**
* Updated ContractsListContent using the generic context menu system
* This replaces the previous specific implementation with the generic one
*/
@Composable
private fun ContractsListContentWithGenericMenu(
    contractsWithStats: List<ContractWithStats>,
    onClick: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onViewDetails: (String) -> Unit,
    onRenewContract: (String) -> Unit
) {
    Timber.d("ContractsListContentWithGenericMenu")

    // Create action handler for contracts using the generic system
    val actionHandler = remember {
        object : ListItemActionHandler {
            override fun onActionSelected(action: ListItemAction, itemId: String) {
                when (action) {
                    ListItemAction.Edit -> onEdit(itemId)
                    ListItemAction.Delete -> onDelete(itemId)
                    ListItemAction.ViewDetails -> onViewDetails(itemId)
                    ListItemAction.ContractAction.ViewDetails -> onViewDetails(itemId)
                    ListItemAction.ContractAction.Renew -> onRenewContract(itemId)
                    else -> {
                        // Log unsupported action if any
                        println("Unsupported action for contract: $action")
                    }
                }
            }

            override fun getDeleteDialogConfig(itemId: String): DeleteDialogConfig {
                val contract = contractsWithStats.find { it.contract.id == itemId }?.contract
                return DeleteDialogConfig(
                    titleResId = R.string.delete_dialog_contract_title,
                    messageResId = R.string.delete_dialog_contract_message,
                    itemNameProvider = { contract?.name }
                )
            }

            override fun isActionAvailable(action: ListItemAction, itemId: String): Boolean {
                val contract = contractsWithStats.find { it.contract.id == itemId }?.contract
                return when (action) {
                    ListItemAction.ContractAction.Renew -> {
                        // Only show renew if contract is expiring soon or expired
                        contract?.let { contractStats ->
                            val stats = contractsWithStats.find { it.contract.id == itemId }?.stats
                            stats?.isExpired == true ||
                                    // Add logic for contracts expiring within 30 days
                                    (contract.endDate + 30.days).isPast()  //.isBefore(kotlinx.datetime.Clock.System.now()))
                        } == true
                    }
                    else -> true
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = contractsWithStats,
            key = { it.contract.id }
        ) { contractWithStats ->
            // Use the generic context menu system
            ListItemWithContextMenu(
                item = contractWithStats.contract,
                itemId = contractWithStats.contract.id,
                availableActions = CommonActionSets.contractActions,
                actionHandler = actionHandler,
                onClick = { onClick(it.id) }
            ) { contract, onCardClick ->
                // Use the existing ContractCard without any modifications!
                Timber.d("ContractCard")
                ContractCard(
                    contract = contract,
                    onClick = { /*onCardClick*/ },
                    onEdit = null, // Handled by context menu
                    onDelete = null, // Handled by context menu
                    variant = QrListItemCardVariant.FULL
                )
            }
        }
    }
}

@Composable
private fun ContractsListContent(
    contractsWithStats: List<ContractWithStats>,
    onClick: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = contractsWithStats,
            key = { it.contract.id }
        ) { contractWithStats ->
            ContractCard(
                contract = contractWithStats.contract,
                onClick = { onClick(contractWithStats.contract.id) },
                onEdit = { onEdit(contractWithStats.contract.id) },
                //onDelete = { onClientDelete(clientWithStats.client.id) },
                onDelete = null,
                variant = QrListItemCardVariant.FULL
            )
        }
    }
}

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
