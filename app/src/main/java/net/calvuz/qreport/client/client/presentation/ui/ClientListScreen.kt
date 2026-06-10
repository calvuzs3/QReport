package net.calvuz.qreport.client.client.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import net.calvuz.qreport.app.app.presentation.components.QReportSortOrderMenu
import net.calvuz.qreport.app.app.presentation.components.QrLoadingState
import net.calvuz.qreport.client.client.presentation.model.ClientFilter
import net.calvuz.qreport.client.client.presentation.model.ClientPkg
import net.calvuz.qreport.client.client.presentation.model.ClientSortOrder
import net.calvuz.qreport.client.client.presentation.ui.components.ClientCard
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.presentation.model.getCardVariantDescription
import net.calvuz.qreport.settings.presentation.model.getCardVariantIcon

/**
 * Client list screen
 *
 * Features:
 * - Client list from db with real statistics
 * - Advanced search with SearchClientsUseCase
 * - Filter for state and type
 * - Pull to refresh
 * - Optimized loading/error/empty states
 * - ClientCard reusable
 * - Dedicated SearchBar component
 */
@Suppress("ParamsComparedByRef")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientListScreen(
    onNavigateToClientDetail: (String, String) -> Unit,
    onNavigateToEditClient: (String) -> Unit,
    onCreateNewClient: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClientListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar con azioni
        TopAppBar(
            navigationIcon = { ClientPkg.icon },
            title = { Text(stringResource(ClientPkg.titleResId)) },
            actions = {
                var showFilterMenu by remember { mutableStateOf(false) }
                var showSortOrderMenu by remember { mutableStateOf(false) }

                // View mode toggle button
                IconButton(onClick = viewModel::cycleCardVariant) {
                    Icon(
                        imageVector = uiState.cardVariant.getCardVariantIcon(),
                        contentDescription = uiState.cardVariant.getCardVariantDescription()
                    )
                }

                // Sort button
                IconButton(onClick = { showSortOrderMenu = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Sort,
                        contentDescription = stringResource(R.string.client_screen_list_action_sort)
                    )
                }

                // Filter button
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.client_screen_list_action_filter)
                    )
                }

                // Filter menu
                QReportFilterMenu(
                    expanded = showFilterMenu,
                    entries = ClientFilter.entries,
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = viewModel::updateFilter,
                    onDismiss = { showFilterMenu = false })

                // Sort menu
                QReportSortOrderMenu(
                    expanded = showSortOrderMenu,
                    entries = ClientSortOrder.entries,
                    selectedSortOrder = uiState.selectedSortOrder,
                    onSortOrderSelected = viewModel::updateSortOrder,
                    onDismiss = { showSortOrderMenu = false })
            })

        // Search bar
        QReportSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            placeholder = stringResource(R.string.client_screen_list_search_placeholder),
        )

        // Filter chips
        if (uiState.selectedFilter != ClientPkg.selectedFilter || uiState.selectedSortOrder != ClientPkg.selectedSortOrder) {
            QReportFiltersChipRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                selectedFilter = uiState.selectedFilter,
                avoidFilter = ClientPkg.selectedFilter,
                onClearFilter = { viewModel.updateFilter(ClientPkg.selectedFilter) },
                selectedSort = uiState.selectedSortOrder,
                avoidSort = ClientPkg.selectedSortOrder,
                onClearSort = { viewModel.updateSortOrder(ClientPkg.selectedSortOrder) })
        }

        QReportPullToRefresh(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize()
        ) {

            // Store error in local variable to avoid smart cast issues
            val currentError = uiState.error

            when {
                uiState.isLoading -> {
                    QrLoadingState()
                }

                currentError != null -> {
                    QReportErrorState(
                        error = currentError, // Smart cast works correctly
                        onRetry = viewModel::loadClients, onDismiss = viewModel::dismissError
                    )
                }

                uiState.filteredClients.isEmpty() -> {
                    val (title, message) = when {
                        uiState.clients.isEmpty() -> stringResource(R.string.client_screen_list_empty_title) to stringResource(
                            R.string.client_screen_list_empty_message
                        )

                        uiState.selectedFilter != ClientFilter.ALL -> stringResource(R.string.client_screen_list_empty_filtered_title) to stringResource(
                            R.string.client_screen_list_empty_filtered_message,
                            uiState.selectedFilter.getDisplayName()
                        )

                        else -> stringResource(R.string.client_screen_list_empty_generic_title) to stringResource(
                            R.string.client_screen_list_empty_generic_message
                        )
                    }
                    EmptyState(
                        textTitle = title,
                        textMessage = message,
                        iconImageVector = Icons.Outlined.Factory,
                        iconContentDescription = stringResource(R.string.client_screen_list_empty_icon_description),
                        iconActionImageVector = Icons.Default.Add,
                        iconActionContentDescription = stringResource(R.string.client_screen_list_fab_new),
                        textAction = stringResource(R.string.client_screen_list_empty_action),
                        onAction = onCreateNewClient
                    )
                }

                else -> {
                    ListContent(
                        clients = uiState.filteredClients,
                        variant = uiState.cardVariant,
                        onCreateNewClient = onCreateNewClient,
                        onClientClick = onNavigateToClientDetail,
                        onClientEdit = onNavigateToEditClient,
                        onClientDelete = viewModel::inactivateClient,
                        onClientRestore = viewModel::restoreClient
                    )
                }
            }
        }
    }
}

@Composable
private fun ListContent(
    clients: List<ClientWithStats>,
    variant: ListViewMode,
    onCreateNewClient: () -> Unit,
    onClientClick: (String, String) -> Unit,
    onClientEdit: (String) -> Unit,
    onClientDelete: (String) -> Unit,
    onClientRestore: (String) -> Unit
) {
    Column {
        ClientSummaryRow(clients, onCreateNewClient)
        ClientListContent(
            clients = clients,
            variant = variant,
            onClientClick = onClientClick,
            onClientEdit = onClientEdit,
            onClientDelete = onClientDelete,
            onClientRestore = onClientRestore
        )
    }
}

@Composable
private fun ClientSummaryRow(
    clients: List<ClientWithStats>, onCreateNewClient: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.client_screen_list_title_count, clients.size),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCreateNewClient) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.client_detail_facilities_new))
            }
        }
    }
}

@Composable
private fun ClientListContent(
    clients: List<ClientWithStats>,
    variant: ListViewMode,
    onClientClick: (String, String) -> Unit,
    onClientEdit: (String) -> Unit,
    onClientDelete: (String) -> Unit,
    onClientRestore: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = clients, key = { it.client.id }) { clientWithStats ->
            ClientCard(
                client = clientWithStats.client,
                stats = clientWithStats.stats,
                onClick = {
                    onClientClick(
                        clientWithStats.client.id, clientWithStats.client.companyName
                    )
                },
                onEdit = { onClientEdit(clientWithStats.client.id) },
//                onDelete = { onClientDelete(clientWithStats.client.id) },
                onDelete = null,
                variant = variant,
                onRestore = { onClientRestore(clientWithStats.client.id) })
        }
    }
}