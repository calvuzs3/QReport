@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.client.island.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.client.island.domain.usecase.FacilityOperationalSummary
import net.calvuz.qreport.client.island.presentation.ui.components.IslandCard
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.app.app.presentation.components.ErrorState
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.app.app.presentation.components.QReportFilterMenu
import net.calvuz.qreport.app.app.presentation.components.QReportFiltersChipRow
import net.calvuz.qreport.app.app.presentation.components.QReportSearchBar
import net.calvuz.qreport.app.app.presentation.components.QReportSortOrderMenu
import net.calvuz.qreport.app.util.SizeUtils.getFormattedCycleCount
import net.calvuz.qreport.client.island.presentation.model.IslandFilter
import net.calvuz.qreport.client.island.presentation.model.IslandPkg
import net.calvuz.qreport.client.island.presentation.model.IslandSortOrder
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.presentation.model.getCardVariantDescription
import net.calvuz.qreport.settings.presentation.model.getCardVariantIcon
import timber.log.Timber

@Suppress("ParamsComparedByRef")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandListScreen(
    facilityId: String,
    facilityName: String,
    onNavigateToIslandDetail: (String) -> Unit,
    onCreateNewIsland: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IslandListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Initialize for specific facility
    LaunchedEffect(facilityId) {
        Timber.d("IslandListScreen facilityId=$facilityId")
        viewModel.initializeForFacility(facilityId)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar con navigazione back
        TopAppBar(
            title = {
                Column {
                    Text("Isole Robotizzate")
                    Text(
                        text = facilityName,
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
                QReportSortOrderMenu(
                    expanded = showSortMenu,
                    entries = IslandSortOrder.entries,
                    selectedSortOrder = uiState.sortOrder,
                    onSortOrderSelected = viewModel::updateSortOrder,
                    onDismiss = { showSortMenu = false }
                )

                // Filter menu
                QReportFilterMenu(
                    expanded = showFilterMenu,
                    entries = IslandFilter.entries,
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = viewModel::updateFilter,
                    onDismiss = { showFilterMenu = false }
                )
            }
        )

        // Statistiche header (se disponibili)
        uiState.statistics?.let { stats ->
            IslandStatisticsHeader(
                statistics = stats,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Search bar
        QReportSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            placeholder = "Cerca per serial, nome o modello...",
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Filter chips
        if (uiState.selectedFilter != IslandPkg.selectedFilter || uiState.sortOrder != IslandPkg.selectedSortOrder) {
            QReportFiltersChipRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                selectedFilter = uiState.selectedFilter,
                avoidFilter = IslandPkg.selectedFilter,
                selectedSort = uiState.sortOrder,
                avoidSort = IslandPkg.selectedSortOrder,
                onClearFilter = { viewModel.updateFilter(IslandFilter.ALL) },
                onClearSort = { viewModel.updateSortOrder(IslandSortOrder.SERIAL_NUMBER) }
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
                        onRetry = viewModel::loadIslands,
                        onDismiss = viewModel::dismissError
                    )
                }

                uiState.filteredIslands.isEmpty() -> {
                    val (title, message) = when {
                        uiState.allIslands.isEmpty() -> "Nessuna Isola" to "Non ci sono ancora Isole per questo Stabilimento"
                        uiState.selectedFilter != IslandPkg.selectedFilter -> "Nessun risultato" to "Non ci sono Isole che corrispondono al filtro '${
                            uiState.selectedFilter.getDisplayName()
                        }'"

                        else -> "Lista vuota" to "Errore nel caricamento dati"
                    }
                    EmptyState(
                        textTitle = title,
                        textMessage = message,
                        iconImageVector = Icons.Default.Analytics,
                        iconContentDescription = "Isole non trovate",
                        iconActionImageVector = Icons.Default.Add,
                        iconActionContentDescription = "Nuova Isola",
                        textAction = "Nuova Isola",
                        onAction = onCreateNewIsland
                    )
                }

                else -> {
                    IslandListContent(
                        islands = uiState.filteredIslands,
                        variant = uiState.cardVariant,
                        onIslandClick = onNavigateToIslandDetail,
                        onIslandDelete = viewModel::deleteIsland
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

            // FAB per nuova isola
            FloatingActionButton(
                onClick = onCreateNewIsland,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nuova Isola"
                )
            }
        }
    }
}

@Composable
private fun IslandStatisticsHeader(
    statistics: FacilityOperationalSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Riepilogo Isole",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    icon = Icons.Default.Analytics,
                    label = "Totali",
                    value = statistics.totalIslands.toString(),
                    color = MaterialTheme.colorScheme.primary
                )

                StatisticItem(
                    icon = Icons.Default.CheckCircle,
                    label = "Attive",
                    value = statistics.activeIslands.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )

                StatisticItem(
                    icon = Icons.Default.Warning,
                    label = "Manutenzione",
                    value = statistics.islandsDueMaintenance.toString(),
                    color = MaterialTheme.colorScheme.error
                )

                StatisticItem(
                    icon = Icons.Default.Shield,
                    label = "In Garanzia",
                    value = statistics.islandsUnderWarranty.toString(),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Ore operative totali e media
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ore Totali: ${statistics.totalOperatingHours}h",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Media: ${statistics.averageOperatingHours}h per isola",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Cicli: ${statistics.totalCycles.getFormattedCycleCount()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Performance globale",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IslandListContent(
    islands: List<IslandWithStats>,
    variant: ListViewMode,
    onIslandClick: (String) -> Unit,
    onIslandDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = islands,
            key = { it.island.id }
        ) { islandWithStats ->
            IslandCard(
                island = islandWithStats.island,
                onClick = { onIslandClick(islandWithStats.island.id) },
                onDelete = { onIslandDelete(islandWithStats.island.id) },
                variant = variant
            )
        }
    }
}