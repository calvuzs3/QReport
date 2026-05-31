package net.calvuz.qreport.client.facility.presentation.ui

import androidx.compose.foundation.layout.*
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
import net.calvuz.qreport.app.app.presentation.components.QReportSearchBar
import net.calvuz.qreport.app.app.presentation.components.QReportSelectorRow
import net.calvuz.qreport.app.app.presentation.components.QReportSortOrderMenu
import net.calvuz.qreport.client.client.presentation.model.ClientPkg
import net.calvuz.qreport.client.facility.presentation.model.FacilityFilter
import net.calvuz.qreport.client.facility.presentation.model.FacilityPkg
import net.calvuz.qreport.client.facility.presentation.model.FacilitySortOrder
import net.calvuz.qreport.client.facility.presentation.ui.components.FacilityListContent
import net.calvuz.qreport.settings.presentation.model.getCardVariantDescription
import net.calvuz.qreport.settings.presentation.model.getCardVariantIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilityListScreen(
    modifier: Modifier = Modifier,
    clientId: String? = null,
    onNavigateToFacilityDetail: (String) -> Unit,
    onCreateNewFacility: () -> Unit,
    onEditFacility: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: FacilityListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(clientId) {
        if (clientId != null) viewModel.initializeForClient(clientId)
        else viewModel.initialize()
    }

    Column(modifier = modifier.fillMaxSize()) {

        TopAppBar(
            title = { Text(stringResource(R.string.facility_screen_list_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.facility_screen_list_action_back)
                    )
                }
            },
            actions = {
                var showFilterMenu by remember { mutableStateOf(false) }
                var showSortMenu by remember { mutableStateOf(false) }

                IconButton(onClick = viewModel::cycleCardVariant) {
                    Icon(
                        imageVector = uiState.cardVariant.getCardVariantIcon(),
                        contentDescription = uiState.cardVariant.getCardVariantDescription()
                    )
                }
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Sort,
                        contentDescription = stringResource(R.string.facility_screen_list_action_sort)
                    )
                }
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.facility_screen_list_action_filter)
                    )
                }

                QReportFilterMenu(
                    expanded = showFilterMenu,
                    entries = FacilityFilter.entries,
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = viewModel::updateFilter,
                    onDismiss = { showFilterMenu = false }
                )
                QReportSortOrderMenu(
                    expanded = showSortMenu,
                    entries = FacilitySortOrder.entries,
                    selectedSortOrder = uiState.sortOrder,
                    onSortOrderSelected = viewModel::updateSortOrder,
                    onDismiss = { showSortMenu = false }
                )
            }
        )

        QReportSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            placeholder = stringResource(R.string.facility_screen_list_search_placeholder)
        )

        QReportSelectorRow(
            entries = uiState.availableClients,
            selectedItem = uiState.selectedClient,
            onItemSelected = viewModel::updateSelectedClient,
            icon = ClientPkg.icon,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (uiState.selectedFilter != FacilityPkg.selectedFilter || uiState.sortOrder != FacilityPkg.selectedSortOrder) {
            QReportFiltersChipRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                selectedFilter = uiState.selectedFilter,
                avoidFilter = FacilityPkg.selectedFilter,
                onClearFilter = { viewModel.updateFilter(FacilityPkg.selectedFilter) },
                selectedSort = uiState.sortOrder,
                avoidSort = FacilityPkg.selectedSortOrder,
                onClearSort = { viewModel.updateSortOrder(FacilityPkg.selectedSortOrder) }
            )
        }

        val pullToRefreshState = rememberPullToRefreshState()

        LaunchedEffect(uiState.isRefreshing) {
            if (!uiState.isRefreshing && pullToRefreshState.isRefreshing) {
                pullToRefreshState.endRefresh()
            }
        }
        LaunchedEffect(pullToRefreshState.isRefreshing) {
            if (pullToRefreshState.isRefreshing) viewModel.refresh()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            val currentError = uiState.error

            when {
                uiState.isLoading -> LoadingState()

                currentError != null -> QReportErrorState(
                    error = currentError,
                    onRetry = viewModel::loadFacilities,
                    onDismiss = viewModel::dismissError
                )

                uiState.filteredFacilities.isEmpty() -> {
                    val (title, message) = when {
                        uiState.facilities.isEmpty() ->
                            stringResource(R.string.facility_screen_list_empty_title) to
                                    stringResource(R.string.facility_screen_list_empty_message)
                        uiState.selectedFilter != FacilityPkg.selectedFilter ->
                            stringResource(R.string.facility_screen_list_empty_filtered_title) to
                                    stringResource(R.string.facility_screen_list_empty_filtered_message, uiState.selectedFilter.getDisplayName())
                        else ->
                            stringResource(R.string.facility_screen_list_empty_generic_title) to
                                    stringResource(R.string.facility_screen_list_empty_generic_message)
                    }
                    EmptyState(
                        textTitle = title,
                        textMessage = message,
                        iconImageVector = Icons.Outlined.Factory,
                        iconContentDescription = stringResource(R.string.facility_screen_list_empty_icon_description),
                        iconActionImageVector = Icons.Default.Add,
                        iconActionContentDescription = stringResource(R.string.facility_screen_list_fab_new),
                        textAction = stringResource(R.string.facility_screen_list_empty_action),
                        onAction = onCreateNewFacility
                    )
                }

                else -> FacilityListContent(
                    variant = uiState.cardVariant,
                    facilities = uiState.filteredFacilities,
                    onFacilityClick = onNavigateToFacilityDetail,
                    onFacilityEdit = onEditFacility,
                    onFacilityDelete = null
                )
            }

            if (pullToRefreshState.isRefreshing || uiState.isRefreshing) {
                PullToRefreshContainer(
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            FloatingActionButton(
                onClick = onCreateNewFacility,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 16.dp, vertical = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.facility_screen_list_fab_new)
                )
            }
        }
    }
}