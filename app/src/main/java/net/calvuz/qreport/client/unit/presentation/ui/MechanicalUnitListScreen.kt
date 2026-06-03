package net.calvuz.qreport.client.unit.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.QReportConfirmDeleteDialog
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.app.app.presentation.components.QReportErrorState
import net.calvuz.qreport.app.app.presentation.components.QReportFilterMenu
import net.calvuz.qreport.app.app.presentation.components.QReportFiltersChipRow
import net.calvuz.qreport.app.app.presentation.components.QReportPullToRefresh
import net.calvuz.qreport.app.app.presentation.components.QReportSearchBar
import net.calvuz.qreport.app.app.presentation.components.QReportSelectorRow
import net.calvuz.qreport.app.app.presentation.components.QReportSortOrderMenu
import net.calvuz.qreport.client.island.presentation.ui.components.IslandOption
import net.calvuz.qreport.client.unit.presentation.model.MechanicalUnitFilter
import net.calvuz.qreport.client.unit.presentation.model.MechanicalUnitPkg
import net.calvuz.qreport.client.unit.presentation.model.MechanicalUnitSortOrder
import net.calvuz.qreport.client.unit.presentation.ui.components.MechanicalUnitListContent
import net.calvuz.qreport.settings.presentation.model.getCardVariantDescription
import net.calvuz.qreport.settings.presentation.model.getCardVariantIcon
import timber.log.Timber

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
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteName by remember { mutableStateOf("") }

    LaunchedEffect(islandId) {
        Timber.d("MechanicalUnitListScreen islandId=$islandId")
        viewModel.initializeForIsland(islandId)
    }

    Column(modifier = modifier.fillMaxSize()) {

        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.unit_screen_list_title))
                    Text(
                        text = uiState.selectedIsland.takeIf { it != IslandOption.ALL }
                            ?.getDisplayName() ?: islandName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = stringResource(R.string.unit_screen_list_action_back))
                }
            },
            actions = {
                var showFilterMenu by remember { mutableStateOf(false) }
                var showSortMenu by remember { mutableStateOf(false) }

                IconButton(onClick = viewModel::cycleCardVariant) {
                    Icon(uiState.cardVariant.getCardVariantIcon(), contentDescription = uiState.cardVariant.getCardVariantDescription())
                }
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.AutoMirrored.Default.Sort, contentDescription = stringResource(R.string.unit_screen_list_action_sort))
                }
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.unit_screen_list_action_filter))
                }
                QReportSortOrderMenu(
                    expanded = showSortMenu,
                    entries = MechanicalUnitSortOrder.entries,
                    selectedSortOrder = uiState.sortOrder,
                    onSortOrderSelected = viewModel::updateSortOrder,
                    onDismiss = { showSortMenu = false }
                )
                QReportFilterMenu(
                    expanded = showFilterMenu,
                    entries = MechanicalUnitFilter.entries,
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = viewModel::updateFilter,
                    onDismiss = { showFilterMenu = false }
                )
            }
        )

        QReportSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            placeholder = stringResource(R.string.unit_screen_list_search_placeholder)
        )

        QReportSelectorRow(
            entries = uiState.availableIslands,
            selectedItem = uiState.selectedIsland,
            onItemSelected = viewModel::updateSelectedIsland,
            icon = Icons.Default.PrecisionManufacturing,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (uiState.selectedFilter != MechanicalUnitPkg.selectedFilter || uiState.sortOrder != MechanicalUnitPkg.selectedSortOrder) {
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

        QReportPullToRefresh(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize()
        ) {

            val currentError = uiState.error

            when {
                uiState.isLoading -> LoadingState()

                currentError != null -> QReportErrorState(
                    error = currentError,
                    onRetry = viewModel::loadUnits,
                    onDismiss = viewModel::dismissError
                )

                uiState.filteredUnits.isEmpty() -> {
                    val (title, message) = when {
                        uiState.allUnits.isEmpty() ->
                            stringResource(R.string.unit_screen_list_empty_title) to
                                    stringResource(R.string.unit_screen_list_empty_message)
                        uiState.selectedFilter != MechanicalUnitPkg.selectedFilter ->
                            stringResource(R.string.unit_screen_list_empty_filtered_title) to
                                    stringResource(R.string.unit_screen_list_empty_filtered_message,
                                        stringResource(uiState.selectedFilter.labelResId))
                        else ->
                            stringResource(R.string.unit_screen_list_empty_search_title) to
                                    stringResource(R.string.unit_screen_list_empty_search_message)
                    }
                    EmptyState(
                        textTitle = title,
                        textMessage = message,
                        iconImageVector = Icons.Default.Settings,
                        iconContentDescription = stringResource(R.string.unit_screen_list_empty_icon_description),
                        iconActionImageVector = Icons.Default.Add,
                        iconActionContentDescription = stringResource(R.string.unit_screen_list_fab_add),
                        textAction = stringResource(R.string.unit_screen_list_fab_add),
                        onAction = onNavigateToAdd
                    )
                }

                else -> MechanicalUnitListContent(
                    units = uiState.filteredUnits,
                    variant = uiState.cardVariant,
                    onUnitClick = onNavigateToEdit,
                    onUnitDelete = { unitId, unitName ->
                        pendingDeleteId = unitId
                        pendingDeleteName = unitName
                    }
                )
            }

            FloatingActionButton(
                onClick = onNavigateToAdd,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.unit_screen_list_fab_add))
            }
        }
    }

    pendingDeleteId?.let { unitId ->
        QReportConfirmDeleteDialog(
            objectName = stringResource(R.string.unit_card_object_name),
            objectDesc = pendingDeleteName,
            onConfirm = { viewModel.deleteUnit(unitId); pendingDeleteId = null; pendingDeleteName = "" },
            onDismiss = { pendingDeleteId = null; pendingDeleteName = "" }
        )
    }
}