package net.calvuz.qreport.presentation.screen.checkup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.domain.model.checkup.CheckUpStatus
import net.calvuz.qreport.presentation.components.ActiveFiltersChipRow
import net.calvuz.qreport.presentation.components.EmptyState
import net.calvuz.qreport.presentation.components.ErrorState

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

                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filtri"
                    )
                }

                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    CheckUpFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(getFilterDisplayName(filter)) },
                            onClick = {
                                viewModel.updateFilter(filter)
                                showFilterMenu = false
                            },
                            leadingIcon = if (uiState.selectedFilter == filter) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }
        )

        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            label = { Text("Cerca check-up...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Cancella")
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )

//        // Filter chips
//        FilterChipRow(
//            selectedFilter = uiState.selectedFilter,
//            onFilterSelected = viewModel::updateFilter,
//            modifier = Modifier.padding(horizontal = 16.dp)
//        )

        // Filter chips
        var filter:String? = null
        if (uiState.selectedFilter != CheckUpFilter.ALL) filter = getFilterDisplayName(uiState.selectedFilter)
        var order: String? = null
        if (uiState.checkUpSortOrder!= CheckUpSortOrder.RECENT_FIRST)
            order = getSortOrderDisplayName(uiState.checkUpSortOrder)
        if (uiState.selectedFilter != CheckUpFilter.ALL || uiState.checkUpSortOrder != CheckUpSortOrder.RECENT_FIRST) {
            ActiveFiltersChipRow(
                selectedFilter = filter,
                selectedSort = order,
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
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    ErrorState(
                        error = uiState.error.toString(),
                        onRetry = viewModel::loadCheckUps,
                        onDismiss = viewModel::dismissError
                    )
                }

                uiState.filteredCheckUps.isEmpty() -> {
                    val (title, message) = when {
                        uiState.checkUps.isEmpty() -> "Nessun Check-Up" to "Non ci sono ancora Check-Up"
                        uiState.selectedFilter != CheckUpFilter.ALL -> "Nessun risultato" to "Non ci sono Check-Up che corrispondono al filtro '${getFilterDisplayName(uiState.selectedFilter)}'"
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.filteredCheckUps,
                            key = { it.checkUp.id }
                        ) { checkUpWithStats ->
                            CheckUpCard(
                                checkUpWithStats = checkUpWithStats,
                                onClick = { onNavigateToCheckUpDetail(checkUpWithStats.checkUp.id) },
                                onDelete = { viewModel.deleteCheckUp(checkUpWithStats.checkUp.id) }
                            )
                        }
                    }
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
private fun FilterChipRow(
    selectedFilter: CheckUpFilter,
    onFilterSelected: (CheckUpFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(CheckUpFilter.entries.toTypedArray()) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(getFilterDisplayName(filter)) }
            )
        }
    }
}

@Composable
private fun CheckUpCard(
    checkUpWithStats: CheckUpWithStats,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val checkUp = checkUpWithStats.checkUp
    val stats = checkUpWithStats.statistics

    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = checkUp.header.clientInfo.companyName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip(status = checkUp.status)

                    // Delete button for drafts
                    if (checkUp.status == CheckUpStatus.DRAFT) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Elimina",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Island info
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Isola ${checkUp.header.islandInfo.serialNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (checkUp.header.clientInfo.site.isNotBlank()) {
                    Text(
                        text = "• ${checkUp.header.clientInfo.site}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progresso",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${checkUpWithStats.progressPercentage}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LinearProgressIndicator(
                    progress = { checkUpWithStats.progressPercentage / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    icon = Icons.Default.CheckCircle,
                    value = "${stats.completedItems}/${stats.totalItems}",
                    label = "Completati"
                )

                if (stats.nokItems > 0) {
                    StatItem(
                        icon = Icons.Default.Error,
                        value = stats.nokItems.toString(),
                        label = "NOK",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (stats.photosCount > 0) {
                    StatItem(
                        icon = Icons.Default.PhotoCamera,
                        value = stats.photosCount.toString(),
                        label = "Foto"
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = checkUpWithStats.formattedLastModified,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Elimina Check-up") },
            text = { Text("Sei sicuro di voler eliminare questo check-up? L'operazione non può essere annullata.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
private fun StatusChip(
    status: CheckUpStatus,
    modifier: Modifier = Modifier
) {
    val (text, containerColor) = when (status) {
        CheckUpStatus.DRAFT -> "Bozza" to MaterialTheme.colorScheme.surfaceVariant
        CheckUpStatus.IN_PROGRESS -> "In Corso" to MaterialTheme.colorScheme.primaryContainer
        CheckUpStatus.COMPLETED -> "Completato" to MaterialTheme.colorScheme.tertiaryContainer
        CheckUpStatus.EXPORTED -> "Esportato" to MaterialTheme.colorScheme.secondaryContainer
        CheckUpStatus.ARCHIVED -> "Archiviato" to MaterialTheme.colorScheme.outline
    }

    AssistChip(
        onClick = { },
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor
        ),
        modifier = modifier
    )
}
//
//@Composable
//private fun ErrorState(
//    error: String,
//    onRetry: () -> Unit,
//    onDismiss: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    val errorMessage = error // Local variable to avoid smart cast issues
//
//    Column(
//        modifier = modifier
//            .fillMaxSize()
//            .padding(32.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Icon(
//            imageVector = Icons.Default.Error,
//            contentDescription = null,
//            modifier = Modifier.size(64.dp),
//            tint = MaterialTheme.colorScheme.error
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Text(
//            text = "Errore",
//            style = MaterialTheme.typography.titleLarge,
//            color = MaterialTheme.colorScheme.error
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        Text(
//            text = errorMessage,
//            style = MaterialTheme.typography.bodyMedium,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            OutlinedButton(onClick = onDismiss) {
//                Text("Chiudi")
//            }
//
//            Button(onClick = onRetry) {
//                Text("Riprova")
//            }
//        }
//    }
//}
//
//@Composable
//private fun EmptyState(
//    filter: CheckUpFilter,
//    searchQuery: String,
//    onCreateNew: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Column(
//        modifier = modifier
//            .fillMaxSize()
//            .padding(32.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Icon(
//            imageVector = Icons.AutoMirrored.Filled.Assignment,
//            contentDescription = null,
//            modifier = Modifier.size(64.dp),
//            tint = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        val (title, message) = when {
//            searchQuery.isNotEmpty() -> "Nessun risultato" to "Non ci sono check-up che corrispondono alla ricerca '$searchQuery'"
//            filter != CheckUpFilter.ALL -> "Nessun check-up" to "Non ci sono check-up con stato '${getFilterDisplayName(filter)}'"
//            else -> "Nessun check-up" to "Non hai ancora creato nessun check-up"
//        }
//
//        Text(
//            text = title,
//            style = MaterialTheme.typography.titleLarge,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        Text(
//            text = message,
//            style = MaterialTheme.typography.bodyMedium,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//
//        if (filter == CheckUpFilter.ALL && searchQuery.isEmpty()) {
//            Spacer(modifier = Modifier.height(24.dp))
//
//            Button(onClick = onCreateNew) {
//                Icon(
//                    imageVector = Icons.Default.Add,
//                    contentDescription = null,
//                    modifier = Modifier.size(18.dp)
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text("Crea il primo check-up")
//            }
//        }
//    }
//}

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
    return when(sortOrder) {
        CheckUpSortOrder.RECENT_FIRST -> "Recenti"
        CheckUpSortOrder.OLDEST_FIRST -> "Datati"
        CheckUpSortOrder.CLIENT_NAME -> "Nome cliente"
        CheckUpSortOrder.STATUS -> "Stato"
    }
}