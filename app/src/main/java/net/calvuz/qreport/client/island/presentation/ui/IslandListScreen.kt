@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.client.island.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.usecase.FacilityOperationalSummary
import net.calvuz.qreport.client.island.presentation.ui.components.IslandCard
import net.calvuz.qreport.client.island.presentation.ui.components.IslandCardVariant
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.app.app.presentation.components.ErrorState
import net.calvuz.qreport.app.app.presentation.components.QReportSearchBar

/**
 * Screen per la lista isole di un facility - seguendo pattern FacilityListScreen
 *
 * Features:
 * - Lista isole dal database con statistiche reali
 * - Ricerca e filtri per tipo/stato isola
 * - Pull to refresh
 * - Stati loading/error/empty ottimizzati
 * - FacilityIslandCard con indicatori manutenzione
 * - Statistiche aggregate in header
 */
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
                IslandSortMenu(
                    expanded = showSortMenu,
                    selectedSort = uiState.sortOrder,
                    onSortSelected = viewModel::updateSortOrder,
                    onDismiss = { showSortMenu = false }
                )

                // Filter menu
                IslandFilterMenu(
                    expanded = showFilterMenu,
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
        if (uiState.selectedFilter != IslandFilter.ALL || uiState.sortOrder != IslandSortOrder.SERIAL_NUMBER) {
            ActiveFiltersChipRow(
                selectedFilter = uiState.selectedFilter,
                selectedSort = uiState.sortOrder,
                onClearFilter = { viewModel.updateFilter(IslandFilter.ALL) },
                onClearSort = { viewModel.updateSortOrder(IslandSortOrder.SERIAL_NUMBER) },
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
                        onRetry = viewModel::loadIslands,
                        onDismiss = viewModel::dismissError
                    )
                }

                uiState.filteredIslands.isEmpty() -> {
                    val (title, message) = when {
                        uiState.allIslands.isEmpty() -> "Nessuna Isola" to "Non ci sono ancora Isole per questo Stabilimento"
                        uiState.selectedFilter!= IslandFilter.ALL -> "Nessun risultato" to "Non ci sono Isole che corrispondono al filtro '${
                            getIslandFilterDisplayName(
                                uiState.selectedFilter
                            )
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
                        text = "Cicli: ${formatCycleCount(statistics.totalCycles)}",
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

// Helper per formattare cicli
private fun formatCycleCount(cycleCount: Long): String {
    return when {
        cycleCount >= 1_000_000 -> "${(cycleCount / 1_000_000).toInt()}M"
        cycleCount >= 1_000 -> "${(cycleCount / 1_000).toInt()}K"
        else -> cycleCount.toString()
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
    islands: List<Island>,
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
            key = { it.id }
        ) { island ->
            IslandCard(
                island = island,
                onClick = { onIslandClick(island.id) },
                onDelete = { onIslandDelete(island.id) },
                variant = IslandCardVariant.FULL
            )
        }
    }
}

@Composable
private fun ActiveFiltersChipRow(
    selectedFilter: IslandFilter,
    selectedSort: IslandSortOrder,
    onClearFilter: () -> Unit,
    onClearSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        if (selectedFilter != IslandFilter.ALL) {
            item {
                FilterChip(
                    selected = true,
                    onClick = onClearFilter,
                    label = { Text("Filtro: ${getIslandFilterDisplayName(selectedFilter)}") },
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

        if (selectedSort != IslandSortOrder.SERIAL_NUMBER) {
            item {
                FilterChip(
                    selected = true,
                    onClick = onClearSort,
                    label = { Text("Ordine: ${getIslandSortDisplayName(selectedSort)}") },
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
private fun IslandFilterMenu(
    expanded: Boolean,
    selectedFilter: IslandFilter,
    onFilterSelected: (IslandFilter) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        IslandFilter.entries.forEach { filter ->
            DropdownMenuItem(
                text = { Text(getIslandFilterDisplayName(filter)) },
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
private fun IslandSortMenu(
    expanded: Boolean,
    selectedSort: IslandSortOrder,
    onSortSelected: (IslandSortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        IslandSortOrder.entries.forEach { sortOrder ->
            DropdownMenuItem(
                text = { Text(getIslandSortDisplayName(sortOrder)) },
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
                text = "Caricamento isole...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper functions per display names
private fun getIslandFilterDisplayName(filter: IslandFilter): String {
    return when (filter) {
        IslandFilter.ALL -> "Tutte"
        IslandFilter.ACTIVE -> "Attive"
        IslandFilter.INACTIVE -> "Inattive"
        IslandFilter.MAINTENANCE_DUE -> "Manutenzione Dovuta"
        IslandFilter.UNDER_WARRANTY -> "In Garanzia"
        IslandFilter.HIGH_OPERATING_HOURS -> "Ore Elevate"
        IslandFilter.BY_TYPE -> "Per Tipo"
    }
}

private fun getIslandSortDisplayName(sortOrder: IslandSortOrder): String {
    return when (sortOrder) {
        IslandSortOrder.SERIAL_NUMBER -> "Serial Number"
        IslandSortOrder.TYPE -> "Tipo"
        IslandSortOrder.STATUS -> "Stato"
        IslandSortOrder.OPERATING_HOURS -> "Ore Operative"
        IslandSortOrder.MAINTENANCE_DATE -> "Data Manutenzione"
        IslandSortOrder.CREATED_RECENT -> "Più Recenti"
        IslandSortOrder.CUSTOM_NAME -> "Nome Personalizzato"
    }
}