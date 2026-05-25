package net.calvuz.qreport.client.unit.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import net.calvuz.qreport.app.app.presentation.components.ConfirmDeleteDialog
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.app.app.presentation.components.ErrorState
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.app.app.presentation.components.QReportFilterMenu
import net.calvuz.qreport.app.app.presentation.components.QReportFiltersChipRow
import net.calvuz.qreport.app.app.presentation.components.QReportSearchBar
import net.calvuz.qreport.app.app.presentation.components.QReportSelectorRow
import net.calvuz.qreport.app.app.presentation.components.QReportSortOrderMenu
import net.calvuz.qreport.client.island.presentation.ui.components.IslandOption
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.presentation.model.MechanicalUnitFilter
import net.calvuz.qreport.client.unit.presentation.model.MechanicalUnitPkg
import net.calvuz.qreport.client.unit.presentation.model.MechanicalUnitSortOrder
import net.calvuz.qreport.client.unit.presentation.ui.components.MechanicalUnitListContent
import net.calvuz.qreport.settings.presentation.model.getCardVariantDescription
import net.calvuz.qreport.settings.presentation.model.getCardVariantIcon
import timber.log.Timber

/**
 * Screen that lists all [MechanicalUnit]s for a given island.
 *
 * @param islandId        ID of the parent island.
 * @param islandName      Display name of the parent island (shown in title).
 * @param onNavigateBack  Back navigation callback.
 * @param onNavigateToAdd Navigate to the add form for a new unit.
 * @param onNavigateToEdit Navigate to the edit form for an existing unit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MechanicalUnitListScreen(
    islandId: String,
    islandName: String,
    onNavigateBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (unitId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MechanicalUnitListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Unit pending deletion — drives the confirm dialog
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteName by remember { mutableStateOf("") }

    LaunchedEffect(islandId) {
        Timber.d("MechanicalUnitListScreen islandId=$islandId")
        viewModel.initializeForIsland(islandId)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("Unità Meccaniche")
                    Text(
                        text = uiState.selectedIsland
                            .takeIf { it != IslandOption.ALL }
                            ?.getDisplayName() ?: islandName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Indietro")
                }
            },
            actions = {
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
                    Icon(Icons.AutoMirrored.Default.Sort, contentDescription = "Ordinamento")
                }

                // Filter button
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filtri")
                }

                // Sort menu
                QReportSortOrderMenu(
                    expanded = showSortMenu,
                    entries = MechanicalUnitSortOrder.entries,
                    selectedSortOrder = uiState.sortOrder,
                    onSortOrderSelected = viewModel::updateSortOrder,
                    onDismiss = { showSortMenu = false }
                )

                // Filter menu
                QReportFilterMenu(
                    expanded = showFilterMenu,
                    entries = MechanicalUnitFilter.entries,
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
            placeholder = "Cerca per nome, seriale o modello...",
        )

        // Island selector
        QReportSelectorRow(
            entries = uiState.availableIslands,
            selectedItem = uiState.selectedIsland,
            onItemSelected = viewModel::updateSelectedIsland,
            icon = Icons.Default.PrecisionManufacturing,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Active filter / sort chips
        if (uiState.selectedFilter != MechanicalUnitPkg.selectedFilter ||
            uiState.sortOrder != MechanicalUnitPkg.selectedSortOrder) {
            QReportFiltersChipRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                selectedFilter = uiState.selectedFilter,
                avoidFilter = MechanicalUnitPkg.selectedFilter,
                onClearFilter = { viewModel.updateFilter(MechanicalUnitPkg.selectedFilter) },
                selectedSort = uiState.sortOrder,
                avoidSort = MechanicalUnitPkg.selectedSortOrder,
                onClearSort = { viewModel.updateSortOrder(MechanicalUnitPkg.selectedSortOrder) }
            )
        }

        // Content with Pull to Refresh
        val pullToRefreshState = rememberPullToRefreshState()

        LaunchedEffect(uiState.isRefreshing) {
            if (!uiState.isRefreshing && pullToRefreshState.isRefreshing) {
                pullToRefreshState.endRefresh()
            }
        }

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
                uiState.isLoading -> LoadingState()

                currentError != null -> ErrorState(
                    error = currentError,
                    onRetry = viewModel::loadUnits,
                    onDismiss = viewModel::dismissError
                )

                uiState.filteredUnits.isEmpty() -> {
                    val (title, message) = when {
                        uiState.allUnits.isEmpty() ->
                            "Nessuna unità meccanica" to "Aggiungi la prima unità con il pulsante +"
                        uiState.selectedFilter != MechanicalUnitPkg.selectedFilter ->
                            "Nessun risultato" to "Nessuna unità corrisponde al filtro '${uiState.selectedFilter.getDisplayName()}'"
                        else ->
                            "Nessun risultato" to "Nessuna unità corrisponde alla ricerca"
                    }
                    EmptyState(
                        textTitle = title,
                        textMessage = message,
                        iconImageVector = Icons.Default.Settings,
                        iconContentDescription = "Nessuna unità",
                        iconActionImageVector = Icons.Default.Add,
                        iconActionContentDescription = "Aggiungi unità",
                        textAction = "Aggiungi unità",
                        onAction = onNavigateToAdd
                    )
                }

                else -> {
                    MechanicalUnitListContent(
                        units = uiState.filteredUnits,
                        variant = uiState.cardVariant,
                        onUnitClick = onNavigateToEdit,
                        onUnitDelete = { unitId, unitName ->
                            pendingDeleteId = unitId
                            pendingDeleteName = unitName
                        }
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

            FloatingActionButton(
                onClick = onNavigateToAdd,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi unità")
            }
        }
    }

    // Rendered outside the Column so it overlays the whole screen
    pendingDeleteId?.let { unitId ->
        ConfirmDeleteDialog(
            objectName = "unità meccanica",
            objectDesc = pendingDeleteName,
            onConfirm = {
                viewModel.deleteUnit(unitId)
                pendingDeleteId = null
                pendingDeleteName = ""
            },
            onDismiss = {
                pendingDeleteId = null
                pendingDeleteName = ""
            }
        )
    }
}
