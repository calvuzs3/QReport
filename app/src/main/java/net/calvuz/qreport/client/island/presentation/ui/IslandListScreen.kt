@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.client.island.presentation.ui

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
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.app.app.presentation.components.QReportErrorState
import net.calvuz.qreport.app.app.presentation.components.QReportFilterMenu
import net.calvuz.qreport.app.app.presentation.components.QReportFiltersChipRow
import net.calvuz.qreport.app.app.presentation.components.QReportPullToRefresh
import net.calvuz.qreport.app.app.presentation.components.QReportSearchBar
import net.calvuz.qreport.app.app.presentation.components.QReportSelectorRow
import net.calvuz.qreport.app.app.presentation.components.QReportSortOrderMenu
import net.calvuz.qreport.client.facility.presentation.model.FacilityPkg
import net.calvuz.qreport.client.island.presentation.model.FacilityOption
import net.calvuz.qreport.client.island.presentation.model.IslandFilter
import net.calvuz.qreport.client.island.presentation.model.IslandPkg
import net.calvuz.qreport.client.island.presentation.model.IslandSortOrder
import net.calvuz.qreport.client.island.presentation.ui.components.IslandListContent
import net.calvuz.qreport.client.island.presentation.ui.components.IslandStatisticsHeader
import net.calvuz.qreport.settings.presentation.model.getCardVariantDescription
import net.calvuz.qreport.settings.presentation.model.getCardVariantIcon
import timber.log.Timber

@Suppress("ParamsComparedByRef")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandListScreen(
    modifier: Modifier = Modifier,
    facilityId: String,
    facilityName: String,
    onNavigateToIslandDetail: (String) -> Unit,
    onCreateNewIsland: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: IslandListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onListEvent: (IslandListEvent) -> Unit = viewModel::onListEvent

    LaunchedEffect(facilityId) {
        Timber.d("IslandListScreen facilityId=$facilityId")
        viewModel.initialize(facilityId)
    }

    Column(modifier = modifier.fillMaxSize()) {

        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.island_screen_list_title))
                    Text(
                        text = uiState.selectedFacility
                            .takeIf { it != FacilityOption.ALL }
                            ?.getDisplayName()?.asString() ?: facilityName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.island_screen_list_action_back)
                    )
                }
            },
            actions = {
                var showFilterMenu by remember { mutableStateOf(false) }
                var showSortMenu by remember { mutableStateOf(false) }

                IconButton(onClick = { onListEvent(IslandListEvent.CycleCardVariant) }) {
                    Icon(
                        imageVector = uiState.cardVariant.getCardVariantIcon(),
                        contentDescription = uiState.cardVariant.getCardVariantDescription()
                    )
                }
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Sort,
                        contentDescription = stringResource(R.string.island_screen_list_action_sort)
                    )
                }
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.island_screen_list_action_filter)
                    )
                }
                QReportSortOrderMenu(
                    expanded = showSortMenu,
                    entries = IslandSortOrder.entries,
                    selectedSortOrder = uiState.sortOrder,
                    onSortOrderSelected = { onListEvent(IslandListEvent.SortOrderChanged(it))},
                    onDismiss = { showSortMenu = false }
                )
                QReportFilterMenu(
                    expanded = showFilterMenu,
                    entries = IslandFilter.entries,
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = { onListEvent(IslandListEvent.FilterChanged(it))},
                    onDismiss = { showFilterMenu = false }
                )
            }
        )

        uiState.statistics?.let { stats ->
            IslandStatisticsHeader(
                statistics = stats,
                modifier = Modifier.padding(16.dp)
            )
        }

        QReportSearchBar(
            query = uiState.searchQuery,
            onQueryChange = { onListEvent(IslandListEvent.SearchQueryChanged(it)) },
            placeholder = stringResource(R.string.island_screen_list_search_placeholder)
        )

        QReportSelectorRow(
            entries = uiState.availableFacilities,
            selectedItem = uiState.selectedFacility,
            onItemSelected = { onListEvent(IslandListEvent.SelectedFacilityChanged(it)) },
            icon = FacilityPkg.icon,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (uiState.selectedFilter != IslandPkg.selectedFilter || uiState.sortOrder != IslandPkg.selectedSortOrder) {
            QReportFiltersChipRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                selectedFilter = uiState.selectedFilter,
                avoidFilter = IslandPkg.selectedFilter,
                selectedSort = uiState.sortOrder,
                avoidSort = IslandPkg.selectedSortOrder,
                onClearFilter = { onListEvent(IslandListEvent.FilterChanged(IslandPkg.selectedFilter)) },
                onClearSort = { onListEvent(IslandListEvent.SortOrderChanged(IslandPkg.selectedSortOrder)) }
            )
        }

        QReportPullToRefresh(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { onListEvent(IslandListEvent.Refresh) },
            modifier = Modifier.fillMaxSize()
        ) {

            val currentError = uiState.error

            when {
                uiState.isLoading -> LoadingState()

                currentError != null -> QReportErrorState(
                    error = currentError,
                    onRetry = { onListEvent(IslandListEvent.Refresh) },
                    onDismiss = { onListEvent(IslandListEvent.DismissError)}
                )

                uiState.filteredIslands.isEmpty() -> {
                    val (title, message) = when {
                        uiState.allIslands.isEmpty() ->
                            stringResource(R.string.island_screen_list_empty_title) to
                                    stringResource(R.string.island_screen_list_empty_message)
                        uiState.selectedFilter != IslandPkg.selectedFilter ->
                            stringResource(R.string.island_screen_list_empty_filtered_title) to
                                    stringResource(R.string.island_screen_list_empty_filtered_message, uiState.selectedFilter.getDisplayName())
                        else ->
                            stringResource(R.string.island_screen_list_empty_generic_title) to
                                    stringResource(R.string.island_screen_list_empty_generic_message)
                    }
                    EmptyState(
                        textTitle = title,
                        textMessage = message,
                        iconImageVector = Icons.Default.Analytics,
                        iconContentDescription = stringResource(R.string.island_screen_list_empty_icon_description),
                        iconActionImageVector = Icons.Default.Add,
                        iconActionContentDescription = stringResource(R.string.island_screen_list_fab_new),
                        textAction = stringResource(R.string.island_screen_list_empty_action),
                        onAction = onCreateNewIsland
                    )
                }

                else -> IslandListContent(
                    islands = uiState.filteredIslands,
                    variant = uiState.cardVariant,
                    onIslandClick = onNavigateToIslandDetail,
                    onIslandDelete = { onListEvent(IslandListEvent.DeleteIsland(it)) }
                )
            }

            if (!facilityId.isNullOrBlank()) {
                FloatingActionButton(
                    onClick = onCreateNewIsland,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 16.dp, vertical = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.island_screen_list_fab_new)
                    )
                }
            }
        }
    }
}