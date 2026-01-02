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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.R
import net.calvuz.qreport.presentation.core.components.ActiveFiltersChipRow
import net.calvuz.qreport.presentation.core.components.EmptyState
import net.calvuz.qreport.presentation.core.components.ErrorDialog
import net.calvuz.qreport.presentation.core.components.LoadingState
import net.calvuz.qreport.presentation.core.components.QReportSearchBar
import net.calvuz.qreport.presentation.core.model.asUiText
import net.calvuz.qreport.presentation.feature.checkup.components.CheckupCard
import net.calvuz.qreport.presentation.feature.checkup.components.CheckupCardVariant
import net.calvuz.qreport.presentation.feature.checkup.model.CheckUpFilter
import net.calvuz.qreport.presentation.feature.checkup.model.CheckUpSortOrder
import net.calvuz.qreport.presentation.feature.checkup.model.CheckUpWithStats
import net.calvuz.qreport.presentation.feature.checkup.model.asString

/**
 * Check up list Screen
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
            title = { Text(stringResource(R.string.checkup_screen_list_title)) },
            actions = {
                var showFilterMenu by remember { mutableStateOf(false) }
                var showSortMenu by remember { mutableStateOf(false) }

                // Sort button
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Sort,
                        contentDescription = stringResource(R.string.checkup_screen_list_action_sort)
                    )
                }

                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.checkup_screen_list_action_filter)
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
            placeholder = stringResource(R.string.checkup_screen_list_search_placeholder),
            modifier = Modifier.padding(16.dp)
        )

        // Filter chips
        if (uiState.selectedFilter != CheckUpFilter.ALL || uiState.checkUpSortOrder != CheckUpSortOrder.RECENT_FIRST) {
            ActiveFiltersChipRow(
                selectedFilter = uiState.selectedFilter.asString(),
                avoidFilter = CheckUpFilter.ALL.asString(),
                selectedSort = uiState.checkUpSortOrder.asString(),
                avoidSort = CheckUpSortOrder.RECENT_FIRST.asString(),
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
                val currentError = /*if*/ (uiState.error )//!= null) /*{*/
                    uiState.error
//                } else {
//                    null
//                }

            when {
                uiState.isLoading -> {
                    LoadingState()
                }

                currentError != null -> {
                    ErrorDialog(
                        onDismiss = viewModel::dismissError,
                        message = "",
                        title = currentError.asUiText().asString(),
                    )
                }

                uiState.filteredCheckUps.isEmpty() -> {
                    val (title, message) = when {
                        uiState.checkUps.isEmpty() -> {
                            stringResource(R.string.checkup_screen_list_empty_title) to
                                    stringResource(R.string.checkup_screen_list_empty_message)
                        }
                        uiState.selectedFilter != CheckUpFilter.ALL -> {
                            stringResource(R.string.checkup_screen_list_empty_no_results_title) to
                                    stringResource(
                                        R.string.checkup_screen_list_empty_no_results_message,
                                        uiState.selectedFilter.asString()
                                    )
                        }
                        else -> {
                            stringResource(R.string.checkup_screen_list_empty_error_title) to
                                    stringResource(R.string.checkup_screen_list_empty_error_message)
                        }
                    }
                    EmptyState(
                        textTitle = title,
                        textMessage = message,
                        iconImageVector = Icons.AutoMirrored.Filled.Assignment,
                        iconContentDescription = stringResource(R.string.checkup_screen_list_empty_icon_content_desc),
                        iconActionImageVector = Icons.Default.Add,
                        iconActionContentDescription = stringResource(R.string.checkup_screen_list_fab_new),
                        textAction = stringResource(R.string.checkup_screen_list_action_new),
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
                    contentDescription = stringResource(R.string.checkup_screen_list_fab_new)
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
                text = { Text(checkupSortOrder.asString()) },
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
                text = { Text(filter.asString()) },
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