package net.calvuz.qreport.presentation.screen.client.facility

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import net.calvuz.qreport.presentation.components.QReportSearchBar
import net.calvuz.qreport.presentation.components.FacilityCard
import net.calvuz.qreport.presentation.components.FacilityCardVariant

/**
 * Screen per la lista stabilimenti di un cliente - seguendo pattern ClientListScreen
 *
 * Features:
 * - Lista stabilimenti dal database con statistiche reali
 * - Ricerca e filtri per tipo/stato facility
 * - Pull to refresh
 * - Stati loading/error/empty ottimizzati
 * - FacilityCard riutilizzabili
 * - Gestione facility primaria
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilityListScreen(
    clientId: String,
    onNavigateToFacilityDetail: (String) -> Unit,
    onCreateNewFacility: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FacilityListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Initialize for specific client
    LaunchedEffect(clientId) {
        viewModel.initializeForClient(clientId)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar con navigazione back
        TopAppBar(
            title = { Text("Stabilimenti") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Indietro")
                }
            },
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

                // Sort menu
                FacilitySortMenu(
                    expanded = showSortMenu,
                    selectedSort = uiState.sortOrder,
                    onSortSelected = viewModel::updateSortOrder,
                    onDismiss = { showSortMenu = false }
                )

                // Filter menu
                FacilityFilterMenu(
                    expanded = showFilterMenu,
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = viewModel::updateFilter,
                    onDismiss = { showFilterMenu = false }
                )
            }
        )

        // Search bar
        QReportSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            placeholder = "Cerca stabilimenti per nome, codice o tipo...",
            modifier = Modifier.padding(16.dp)
        )

        // Filter chips
        if (uiState.selectedFilter != FacilityFilter.ALL || uiState.sortOrder != FacilitySortOrder.NAME) {
            ActiveFiltersChipRow(
                selectedFilter = uiState.selectedFilter,
                selectedSort = uiState.sortOrder,
                onClearFilter = { viewModel.updateFilter(FacilityFilter.ALL) },
                onClearSort = { viewModel.updateSortOrder(FacilitySortOrder.NAME) },
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
            val currentError = uiState.error

            when {
                uiState.isLoading -> {
                    LoadingState()
                }

                currentError != null -> {
                    ErrorState(
                        error = currentError,
                        onRetry = viewModel::loadFacilities,
                        onDismiss = viewModel::dismissError
                    )
                }

                uiState.filteredFacilities.isEmpty() -> {
                    EmptyState(
                        filter = uiState.selectedFilter,
                        searchQuery = uiState.searchQuery,
                        onCreateNew = onCreateNewFacility
                    )
                }

                else -> {
                    FacilityListContent(
                        facilities = uiState.filteredFacilities,
                        onFacilityClick = onNavigateToFacilityDetail,
                        onFacilityDelete = viewModel::deleteFacility
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

            // FAB per nuovo stabilimento
            FloatingActionButton(
                onClick = onCreateNewFacility,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nuovo Stabilimento"
                )
            }
        }
    }
}

@Composable
private fun FacilityListContent(
    facilities: List<FacilityWithStats>,
    onFacilityClick: (String) -> Unit,
    onFacilityDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = facilities,
            key = { it.facility.id }
        ) { facilityWithStats ->
            FacilityCard(
                facility = facilityWithStats.facility,
                stats = facilityWithStats.stats,
                onClick = { onFacilityClick(facilityWithStats.facility.id) },
                onDelete = { onFacilityDelete(facilityWithStats.facility.id) },
                variant = FacilityCardVariant.FULL
            )
        }
    }
}

@Composable
private fun ActiveFiltersChipRow(
    selectedFilter: FacilityFilter,
    selectedSort: FacilitySortOrder,
    onClearFilter: () -> Unit,
    onClearSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        if (selectedFilter != FacilityFilter.ALL) {
            item {
                FilterChip(
                    selected = true,
                    onClick = onClearFilter,
                    label = { Text("Filtro: ${getFacilityFilterDisplayName(selectedFilter)}") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Rimuovi filtro",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        if (selectedSort != FacilitySortOrder.NAME) {
            item {
                FilterChip(
                    selected = true,
                    onClick = onClearSort,
                    label = { Text("Ordine: ${getFacilitySortDisplayName(selectedSort)}") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Rimuovi ordinamento",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun FacilityFilterMenu(
    expanded: Boolean,
    selectedFilter: FacilityFilter,
    onFilterSelected: (FacilityFilter) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        FacilityFilter.entries.forEach { filter ->
            DropdownMenuItem(
                text = { Text(getFacilityFilterDisplayName(filter)) },
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
private fun FacilitySortMenu(
    expanded: Boolean,
    selectedSort: FacilitySortOrder,
    onSortSelected: (FacilitySortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        FacilitySortOrder.entries.forEach { sortOrder ->
            DropdownMenuItem(
                text = { Text(getFacilitySortDisplayName(sortOrder)) },
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

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Caricamento stabilimenti...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val errorMessage = error.takeIf { it.isNotBlank() } ?: "Errore sconosciuto"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Errore",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onDismiss) {
                Text("Chiudi")
            }

            Button(onClick = onRetry) {
                Text("Riprova")
            }
        }
    }
}

@Composable
private fun EmptyState(
    filter: FacilityFilter,
    searchQuery: String,
    onCreateNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Factory,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        val (title, message) = when {
            searchQuery.isNotEmpty() -> "Nessun risultato" to "Non ci sono stabilimenti che corrispondono alla ricerca '$searchQuery'"
            filter != FacilityFilter.ALL -> "Nessuno stabilimento" to "Non ci sono stabilimenti con filtro '${getFacilityFilterDisplayName(filter)}'"
            else -> "Nessuno stabilimento" to "Non hai ancora aggiunto nessuno stabilimento per questo cliente"
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (filter == FacilityFilter.ALL && searchQuery.isEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onCreateNew) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aggiungi primo stabilimento")
            }
        }
    }
}

// Helper functions per display names
private fun getFacilityFilterDisplayName(filter: FacilityFilter): String {
    return when (filter) {
        FacilityFilter.ALL -> "Tutti"
        FacilityFilter.ACTIVE -> "Attivi"
        FacilityFilter.INACTIVE -> "Inattivi"
        FacilityFilter.PRIMARY_ONLY -> "Solo Primari"
        FacilityFilter.WITH_ISLANDS -> "Con Isole"
        FacilityFilter.BY_TYPE -> "Per Tipo"
    }
}

private fun getFacilitySortDisplayName(sortOrder: FacilitySortOrder): String {
    return when (sortOrder) {
        FacilitySortOrder.NAME -> "Nome"
        FacilitySortOrder.CREATED_RECENT -> "Più Recenti"
        FacilitySortOrder.CREATED_OLDEST -> "Meno Recenti"
        FacilitySortOrder.ISLANDS_COUNT -> "Numero Isole"
        FacilitySortOrder.TYPE -> "Tipo"
    }
}