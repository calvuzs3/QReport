package net.calvuz.qreport.presentation.feature.client.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.presentation.core.components.ActiveFiltersChipRow
import net.calvuz.qreport.presentation.core.components.EmptyState
import net.calvuz.qreport.presentation.core.components.ErrorState
import net.calvuz.qreport.presentation.core.components.LoadingState
import net.calvuz.qreport.presentation.core.components.QReportSearchBar
import net.calvuz.qreport.presentation.feature.client.client.components.ClientCard
import net.calvuz.qreport.presentation.feature.client.client.components.ClientCardVariant

/**
 * Screen per la lista clienti - VERSIONE CON COMPONENTS RIUTILIZZABILI
 *
 * Features:
 * - Lista clienti dal database con statistiche reali
 * - Ricerca avanzata con SearchClientsUseCase
 * - Filtri per stato e tipo
 * - Pull to refresh
 * - Stati loading/error/empty ottimizzati
 * - ClientCard riutilizzabili
 * - SearchBar component dedicato
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
            title = { Text("Clienti") },
            actions = {
                var showFilterMenu by remember { mutableStateOf(false) }
                var showSortMenu by remember { mutableStateOf(false) }

                // Sort button
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Sort,
                        contentDescription = "Ordinamento"
                    )
                }

                // Filter button
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filtri"
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
                    selectedSort = uiState.clientSortOrder,
                    onSortSelected = viewModel::updateSortOrder,
                    onDismiss = { showSortMenu = false }
                )
            }
        )

        // Search bar usando component riutilizzabile
        QReportSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            placeholder = "Ricerca Clienti",
            modifier = Modifier.padding(16.dp)
        )

        // Filter chips
        if (uiState.selectedFilter != ClientFilter.ALL || uiState.clientSortOrder != ClientSortOrder.COMPANY_NAME) {
            ActiveFiltersChipRow(
                selectedFilter = getFilterDisplayName(uiState.selectedFilter),
                avoidFilter = getFilterDisplayName(ClientFilter.ALL),
                selectedSort = getSortOrderDisplayName(uiState.clientSortOrder),
                avoidSort = getSortOrderDisplayName(ClientSortOrder.COMPANY_NAME),
                onClearFilter = { viewModel.updateFilter(ClientFilter.ALL) },
                onClearSort = { viewModel.updateSortOrder(ClientSortOrder.COMPANY_NAME) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Content with Pull to Refresh
        val pullToRefreshState = rememberPullToRefreshState()

        // Handle pull to refresh
        LaunchedEffect(pullToRefreshState.isRefreshing) {
            if (pullToRefreshState.isRefreshing) {
                viewModel.refresh()
            }
        }

        // Reset refresh state when not refreshing
        LaunchedEffect(uiState.isRefreshing) {
            if (!uiState.isRefreshing && pullToRefreshState.isRefreshing) {
                pullToRefreshState.endRefresh()
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
                        error = currentError, // Smart cast works correctly
                        onRetry = viewModel::loadClients,
                        onDismiss = viewModel::dismissError
                    )
                }

                uiState.filteredClients.isEmpty() -> {
                    val (title, message) = when {
                        uiState.clients.isEmpty() -> "Nessun Cliente" to "Non ci sono ancora Clienti"
                        uiState.selectedFilter != ClientFilter.ALL -> "Nessun risultato" to "Non ci sono Clienti che corrispondono al filtro '${
                            getFilterDisplayName(
                                uiState.selectedFilter
                            )
                        }'"

                        else -> "Lista vuota" to "Errore nel caricamento dati"
                    }
                    EmptyState(
                        textTitle = title,
                        textMessage = message,
                        iconImageVector = Icons.Outlined.Factory,
                        iconContentDescription = "Nessun Cliente",
                        iconActionImageVector = Icons.Default.Add,
                        iconActionContentDescription = "Nuovo cliente",
                        textAction = "Nuovo Cliente",
                        onAction = onCreateNewClient
                    )
                }

                else -> {
                    ClientListContent(
                        clients = uiState.filteredClients,
                        onClientClick = onNavigateToClientDetail,
                        onClientEdit = onNavigateToEditClient,
                        onClientDelete = viewModel::deleteClient
                    )
                }
            }

            // Pull to refresh indicator
            if (pullToRefreshState.isRefreshing || uiState.isRefreshing) {
                PullToRefreshContainer(
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            // FAB per nuovo cliente
            FloatingActionButton(
                onClick = onCreateNewClient,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nuovo Cliente"
                )
            }
        }
    }
}

@Composable
private fun ClientListContent(
    clients: List<ClientWithStats>,
    onClientClick: (String, String) -> Unit,
    onClientEdit: (String) -> Unit,
    onClientDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = clients,
            key = { it.client.id }
        ) { clientWithStats ->
            ClientCard(
                client = clientWithStats.client,
                stats = clientWithStats.stats,
                onClick = {
                    onClientClick(
                        clientWithStats.client.id,
                        clientWithStats.client.companyName
                    )
                },
                onEdit = { onClientEdit(clientWithStats.client.id) },
                //onDelete = { onClientDelete(clientWithStats.client.id) },
                onDelete = null,
                variant = ClientCardVariant.FULL
            )
        }
    }
}

@Composable
private fun FilterMenu(
    expanded: Boolean,
    selectedFilter: ClientFilter,
    onFilterSelected: (ClientFilter) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        ClientFilter.entries.forEach { filter ->
            DropdownMenuItem(
                text = { Text(getFilterDisplayName(filter)) },
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
    selectedSort: ClientSortOrder,
    onSortSelected: (ClientSortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        ClientSortOrder.entries.forEach { sortOrder ->
            DropdownMenuItem(
                text = { Text(getSortOrderDisplayName(sortOrder)) },
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

// Helper functions
private fun getFilterDisplayName(filter: ClientFilter): String {
    return when (filter) {
        ClientFilter.ALL -> "Tutti"
        ClientFilter.ACTIVE -> "Attivi"
        ClientFilter.INACTIVE -> "Inattivi"
        ClientFilter.WITH_FACILITIES -> "Con Stabilimenti"
        ClientFilter.WITH_CONTACTS -> "Con Contatti"
        ClientFilter.WITH_ISLANDS -> "Con Isole"
    }
}

private fun getSortOrderDisplayName(clientSortOrder: ClientSortOrder): String {
    return when (clientSortOrder) {
        ClientSortOrder.COMPANY_NAME -> "Nome Azienda"
        ClientSortOrder.CREATED_RECENT -> "Più Recenti"
        ClientSortOrder.CREATED_OLDEST -> "Meno Recenti"
        ClientSortOrder.FACILITIES_COUNT -> "Stabilimenti"
        ClientSortOrder.CHECKUPS_COUNT -> "Check-up"
    }
}