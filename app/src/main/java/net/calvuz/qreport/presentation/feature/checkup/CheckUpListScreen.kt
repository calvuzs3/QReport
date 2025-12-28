package net.calvuz.qreport.presentation.feature.checkup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
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
import net.calvuz.qreport.presentation.feature.checkup.components.CheckupCard
import net.calvuz.qreport.presentation.feature.checkup.components.CheckupCardVariant

/**
 * Screen per la lista check-up con dati reali
 *
 * Features:
 * - Lista check-up dal database
 * - Ricerca e filtri
 * - Pull to refresh
 * - Stati loading/error
 * - Navigazione ai dettagli
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckUpListScreen(
    onNavigateToCheckUpDetail: (String) -> Unit,
    onNavigateToEditCheckUp: (String) -> Unit,
    onCreateNewCheckUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckUpListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar con ricerca
        TopAppBar(
            title = { Text("Check-up") },
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
                    selectedSort = uiState.checkUpSortOrder,
                    onSortSelected = viewModel::updateSortOrder,
                    onDismiss = { showSortMenu = false }
                )
            }
        )

        // Search bar usando component riutilizzabile
        QReportSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            placeholder = "Ricerca Check-up",
            modifier = Modifier.padding(16.dp)
        )

        // Filter chips
        if (uiState.selectedFilter != CheckUpFilter.ALL || uiState.checkUpSortOrder != CheckUpSortOrder.RECENT_FIRST) {
            ActiveFiltersChipRow(
                selectedFilter = getFilterDisplayName(uiState.selectedFilter),
                avoidFilter = getFilterDisplayName(CheckUpFilter.ALL),
                selectedSort = getSortOrderDisplayName(uiState.checkUpSortOrder),
                avoidSort = getSortOrderDisplayName(CheckUpSortOrder.RECENT_FIRST),
                onClearFilter = { viewModel.updateFilter(CheckUpFilter.ALL) },
                onClearSort = { viewModel.updateSortOrder(CheckUpSortOrder.RECENT_FIRST) },
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
                        error = currentError, // Smart work correctly
                        onRetry = viewModel::loadCheckUps,
                        onDismiss = viewModel::dismissError
                    )
                }

                uiState.filteredCheckUps.isEmpty() -> {
                    val (title, message) = when {
                        uiState.checkUps.isEmpty() -> "Nessun Check-Up" to "Non ci sono ancora Check-Up"
                        uiState.selectedFilter != CheckUpFilter.ALL -> "Nessun risultato" to "Non ci sono Check-Up che corrispondono al filtro '${
                            getFilterDisplayName(
                                uiState.selectedFilter
                            )
                        }'"

                        else -> "Lista vuota" to "Errore nel caricamento dati"
                    }
                    EmptyState(
                        textTitle = title,
                        textMessage = message,
                        iconImageVector = Icons.AutoMirrored.Filled.Assignment,
                        iconContentDescription = "Check-Up",
                        iconActionImageVector = Icons.Default.Add,
                        iconActionContentDescription = "Nuovo Check-Up",
                        textAction = "Nuovo Check-Up",
                        onAction = onCreateNewCheckUp
                    )
                }

                else -> {
                    CheckupListContent(
                        checkups = uiState.filteredCheckUps,
                        onClick = onNavigateToCheckUpDetail,
                        onEdit = onNavigateToEditCheckUp,
                        onDelete = {}
                    )
                }
            }

            // Pull to refresh indicator - only show when actively refreshing
            if (pullToRefreshState.isRefreshing || uiState.isRefreshing) {
                PullToRefreshContainer(
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            // FAB
            FloatingActionButton(
                onClick = onCreateNewCheckUp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nuovo Check-up"
                )
            }
        }
    }
}

@Composable
private fun CheckupListContent(
    checkups: List<CheckUpWithStats>,
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
            items = checkups,
            key = { it.checkUp.id }
        ) { checkupWithStats ->
            CheckupCard(
                checkup = checkupWithStats.checkUp,
                stats = checkupWithStats.statistics,
                onClick = { onClick(checkupWithStats.checkUp.id) },
                onEdit = { onEdit(checkupWithStats.checkUp.id) },
                //onDelete = { onDelete(checkupWithStats.checkUp.id) },
                onDelete = null,
                variant = CheckupCardVariant.FULL
            )
        }
    }
}

@Composable
private fun SortMenu(
    expanded: Boolean,
    selectedSort: CheckUpSortOrder,
    onSortSelected: (CheckUpSortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        CheckUpSortOrder.entries.forEach { checkupSortOrder ->
            DropdownMenuItem(
                text = { Text(getSortOrderDisplayName(checkupSortOrder)) },
                onClick = {
                    onSortSelected(checkupSortOrder)
                    onDismiss()
                },
                leadingIcon = if (selectedSort == checkupSortOrder) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}

@Composable
private fun FilterMenu(
    expanded: Boolean,
    selectedFilter: CheckUpFilter,
    onFilterSelected: (CheckUpFilter) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        CheckUpFilter.entries.forEach { filter ->
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

// Helper functions

private fun getFilterDisplayName(filter: CheckUpFilter): String {
    return when (filter) {
        CheckUpFilter.ALL -> "Tutti"
        CheckUpFilter.DRAFT -> "Bozze"
        CheckUpFilter.IN_PROGRESS -> "In corso"
        CheckUpFilter.COMPLETED -> "Completati"
    }
}

private fun getSortOrderDisplayName(sortOrder: CheckUpSortOrder): String {
    return when (sortOrder) {
        CheckUpSortOrder.RECENT_FIRST -> "Recenti"
        CheckUpSortOrder.OLDEST_FIRST -> "Datati"
        CheckUpSortOrder.CLIENT_NAME -> "Nome cliente"
        CheckUpSortOrder.STATUS -> "Stato"
    }
}