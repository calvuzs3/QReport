package net.calvuz.qreport.presentation.screen.client.facility

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.domain.model.client.Facility
import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.model.client.OperationalStatus
import net.calvuz.qreport.domain.usecase.client.facilityisland.FacilityOperationalSummary

/**
 * Screen per il dettaglio facility con gestione islands
 *
 * Features:
 * - Tab: Info, Islands, Maintenance
 * - Gestione completa islands (CRUD) nel tab dedicato
 * - Statistiche operative facility
 * - Navigation verso island management
 * - Azioni quick per manutenzione
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilityDetailScreen(
    facilityId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit, // Edit facility

    // Island navigation callbacks
    onNavigateToCreateIsland: (String) -> Unit, // Create island
    onNavigateToEditIsland: (String) -> Unit, // Edit island - only islandId available
    onNavigateToIslandDetail: (String) -> Unit = { }, // View Island
    onNavigateToIslandsList: (String) -> Unit = { },   // View all Islands

    modifier: Modifier = Modifier,
    viewModel: FacilityDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load facility details when screen opens
    LaunchedEffect(facilityId) {
        viewModel.loadFacilityDetails(facilityId)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = uiState.facilityName.takeIf { it.isNotBlank() } ?: "Stabilimento",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Indietro"
                    )
                }
            },
            actions = {
                // Edit button (solo se dati caricati)
                if (uiState.hasData) {
                    IconButton(onClick = { onNavigateToEdit(facilityId) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifica stabilimento"
                        )
                    }
                }

                // Refresh button
                IconButton(onClick = viewModel::refreshData) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Aggiorna"
                    )
                }
            }
        )

        when {
            uiState.isLoading -> {
                LoadingState()
            }

            uiState.error != null -> {
                ErrorState(
                    error = uiState.error!!,
                    onRetry = viewModel::refreshData,
                    onDismiss = viewModel::dismissError
                )
            }

            uiState.hasData -> {
                FacilityDetailContent(
                    uiState = uiState,
                    facilityId = facilityId,
                    onTabSelected = viewModel::selectTab,
                    onIslandFilterSelected = viewModel::updateIslandFilter,
                    onIslandClick = onNavigateToIslandDetail,
                    onCreateIsland = { onNavigateToCreateIsland(facilityId) },
                    onEditIsland = onNavigateToEditIsland,
                    onDeleteIsland = viewModel::deleteIsland,
                    onMarkMaintenanceComplete = viewModel::markMaintenanceComplete
                )
            }

            else -> {
                EmptyState()
            }
        }
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Could show snackbar or other error handling
        }
    }
}

@Composable
private fun FacilityDetailContent(
    uiState: FacilityDetailUiState,
    facilityId: String,
    onTabSelected: (FacilityDetailTab) -> Unit,
    onIslandFilterSelected: (IslandFilter) -> Unit,
    onIslandClick: (String) -> Unit,
    onCreateIsland: () -> Unit,
    onEditIsland: (String) -> Unit,
    onDeleteIsland: (String) -> Unit,
    onMarkMaintenanceComplete: (String) -> Unit
) {
    Column {
        // Header con badge status e statistics
        if (uiState.statusBadge.isNotBlank() || uiState.isPrimaryFacility) {
            HeaderSection(
                facility = uiState.facility!!,
                statusBadge = uiState.statusBadge,
                statusColor = uiState.statusBadgeColor.toLongOrNull() ?: 0xFF22C55EL,
                statisticsSummary = uiState.statisticsSummary,
                operationalSummary = uiState.operationalSummary
            )
        }

        // Tab Row
        TabRow(
            selectedTabIndex = uiState.selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            FacilityDetailTab.entries.forEach { tab ->
                val count = when (tab) {
                    FacilityDetailTab.INFO -> null
                    FacilityDetailTab.ISLANDS -> uiState.islandsCount.takeIf { it > 0 }
                    FacilityDetailTab.MAINTENANCE -> uiState.maintenanceIssuesCount.takeIf { it > 0 }
                }

                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(tab.title)
                            count?.let {
                                Badge {
                                    Text(
                                        text = it.toString(),
                                        color = if (tab == FacilityDetailTab.MAINTENANCE && it > 0)
                                            MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        // Tab Content
        when (uiState.selectedTab) {
            FacilityDetailTab.INFO -> {
                InfoTabContent(
                    facility = uiState.facility!!,
                    operationalSummary = uiState.operationalSummary,
                    modifier = Modifier.weight(1f)
                )
            }

            FacilityDetailTab.ISLANDS -> {
                IslandsTabContent(
                    islands = uiState.filteredIslands,
                    allIslands = uiState.islands,
                    selectedFilter = uiState.selectedIslandFilter,
                    onFilterSelected = onIslandFilterSelected,
                    onIslandClick = onIslandClick,
                    onCreateIsland = onCreateIsland,
                    onEditIsland = onEditIsland,
                    onDeleteIsland = onDeleteIsland,
                    modifier = Modifier.weight(1f)
                )
            }

            FacilityDetailTab.MAINTENANCE -> {
                MaintenanceTabContent(
                    islandsNeedingMaintenance = uiState.islandsNeedingMaintenance,
                    islandsUnderWarranty = uiState.islandsUnderWarranty,
                    onMarkMaintenanceComplete = onMarkMaintenanceComplete,
                    onIslandClick = onIslandClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    facility: Facility,
    statusBadge: String,
    statusColor: Long,
    statisticsSummary: String,
    operationalSummary: FacilityOperationalSummary?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top row: Status and primary badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status badge
                    if (statusBadge.isNotBlank()) {
                        AssistChip(
                            onClick = { },
                            label = { Text(statusBadge) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = androidx.compose.ui.graphics.Color(statusColor)
                            )
                        )
                    }

                    // Primary facility badge
                    if (facility.isPrimary) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("Primario")
                                }
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Statistics summary
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = facility.facilityType.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = statisticsSummary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Operational statistics (se disponibili)
            operationalSummary?.let { summary ->
                if (summary.totalIslands > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            icon = Icons.Default.PrecisionManufacturing,
                            value = summary.totalIslands.toString(),
                            label = "Isole Totali"
                        )
                        StatItem(
                            icon = Icons.Default.CheckCircle,
                            value = summary.activeIslands.toString(),
                            label = "Attive",
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatItem(
                            icon = Icons.Default.Warning,
                            value = summary.islandsDueMaintenance.toString(),
                            label = "Manutenzione",
                            color = if (summary.islandsDueMaintenance > 0)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StatItem(
                            icon = Icons.Default.Security,
                            value = summary.islandsUnderWarranty.toString(),
                            label = "Garanzia"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoTabContent(
    facility: Facility,
    operationalSummary: FacilityOperationalSummary?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            InfoCard(
                title = "Informazioni Generali",
                icon = Icons.Default.Business
            ) {
                InfoItem(
                    label = "Nome Stabilimento",
                    value = facility.displayName
                )

                facility.code?.let {
                    InfoItem(
                        label = "Codice Interno",
                        value = it
                    )
                }

                InfoItem(
                    label = "Tipo Stabilimento",
                    value = facility.facilityType.displayName
                )

                facility.description?.let {
                    InfoItem(
                        label = "Descrizione",
                        value = it
                    )
                }
            }
        }

        item {
            InfoCard(
                title = "Indirizzo",
                icon = Icons.Default.LocationOn
            ) {
                InfoItem(
                    label = "Indirizzo Completo",
                    value = facility.addressDisplay
                )

                facility.address.city?.let {
                    InfoItem(
                        label = "Città",
                        value = it
                    )
                }

                facility.address.postalCode?.let {
                    InfoItem(
                        label = "CAP",
                        value = it
                    )
                }

                facility.address.country.let {
                    InfoItem(
                        label = "Paese",
                        value = it
                    )
                }
            }
        }

        // Statistiche operative
        operationalSummary?.let { summary ->
            item {
                InfoCard(
                    title = "Statistiche Operative",
                    icon = Icons.Default.Analytics
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            InfoItem(
                                label = "Ore Totali",
                                value = formatOperatingHours(summary.totalOperatingHours)
                            )
                            InfoItem(
                                label = "Cicli Totali",
                                value = formatNumber(summary.totalCycles)
                            )
                        }

                        Column {
                            InfoItem(
                                label = "Media Ore/Isola",
                                value = formatOperatingHours(summary.averageOperatingHours)
                            )
                            InfoItem(
                                label = "Efficienza",
                                value = "${(summary.activeIslands * 100 / summary.totalIslands.coerceAtLeast(1))}%"
                            )
                        }
                    }
                }
            }
        }

        item {
            InfoCard(
                title = "Metadati",
                icon = Icons.Default.Info
            ) {
                InfoItem(
                    label = "Creato",
                    value = formatTimestamp(facility.createdAt)
                )
                InfoItem(
                    label = "Ultima modifica",
                    value = formatTimestamp(facility.updatedAt)
                )
                InfoItem(
                    label = "Stato",
                    value = if (facility.isActive) "Attivo" else "Inattivo"
                )
            }
        }
    }
}

@Composable
private fun IslandsTabContent(
    islands: List<FacilityIsland>,
    allIslands: List<FacilityIsland>,
    selectedFilter: IslandFilter,
    onFilterSelected: (IslandFilter) -> Unit,
    onIslandClick: (String) -> Unit,
    onCreateIsland: () -> Unit,
    onEditIsland: (String) -> Unit,
    onDeleteIsland: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header con azioni e filtri
        Column {
            // Actions row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Isole (${islands.size}/${allIslands.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Button(onClick = onCreateIsland) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuova Isola")
                }
            }

            // Filter chips
            if (selectedFilter != IslandFilter.ALL) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { onFilterSelected(IslandFilter.ALL) },
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
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Content
        if (islands.isEmpty()) {
            EmptyIslandsState(
                hasIslands = allIslands.isNotEmpty(),
                filter = selectedFilter,
                onCreateIsland = onCreateIsland,
                onClearFilter = { onFilterSelected(IslandFilter.ALL) },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = islands,
                    key = { it.id }
                ) { island ->
                    IslandItem(
                        island = island,
                        onIslandClick = onIslandClick,
                        onEditIsland = onEditIsland,
                        onDeleteIsland = onDeleteIsland
                    )
                }
            }
        }
    }
}

@Composable
private fun MaintenanceTabContent(
    islandsNeedingMaintenance: List<FacilityIsland>,
    islandsUnderWarranty: List<FacilityIsland>,
    onMarkMaintenanceComplete: (String) -> Unit,
    onIslandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Manutenzioni richieste
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Manutenzioni Richieste (${islandsNeedingMaintenance.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (islandsNeedingMaintenance.isEmpty()) {
                        Text(
                            text = "Tutte le isole sono in regola con le manutenzioni",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        islandsNeedingMaintenance.forEach { island ->
                            MaintenanceIslandItem(
                                island = island,
                                onMarkComplete = onMarkMaintenanceComplete,
                                onIslandClick = onIslandClick
                            )
                        }
                    }
                }
            }
        }

        // Isole sotto garanzia
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Isole in Garanzia (${islandsUnderWarranty.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (islandsUnderWarranty.isEmpty()) {
                        Text(
                            text = "Nessuna isola attualmente in garanzia",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        islandsUnderWarranty.forEach { island ->
                            WarrantyIslandItem(
                                island = island,
                                onIslandClick = onIslandClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IslandItem(
    island: FacilityIsland,
    onIslandClick: (String) -> Unit,
    onEditIsland: (String) -> Unit,
    onDeleteIsland: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = island.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${island.islandType.displayName} • S/N: ${island.serialNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (island.operatingHours > 0 || island.cycleCount > 0) {
                    Text(
                        text = "${formatOperatingHours(island.operatingHours)} • ${formatNumber(island.cycleCount)} cicli",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator
                Icon(
                    imageVector = when (island.operationalStatus) {
                        OperationalStatus.OPERATIONAL -> Icons.Default.CheckCircle
                        OperationalStatus.MAINTENANCE_DUE -> Icons.Default.Warning
                        OperationalStatus.INACTIVE -> Icons.Default.Cancel
                    },
                    contentDescription = null,
                    tint = when (island.operationalStatus) {
                        OperationalStatus.OPERATIONAL -> MaterialTheme.colorScheme.primary
                        OperationalStatus.MAINTENANCE_DUE -> MaterialTheme.colorScheme.error
                        OperationalStatus.INACTIVE -> MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(20.dp)
                )

                IconButton(
                    onClick = { onEditIsland(island.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Modifica",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Elimina",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                IconButton(
                    onClick = { onIslandClick(island.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Dettagli",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        IslandDeleteDialog(
            islandName = island.displayName,
            onConfirm = {
                onDeleteIsland(island.id)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun MaintenanceIslandItem(
    island: FacilityIsland,
    onMarkComplete: (String) -> Unit,
    onIslandClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = island.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Scadenza: ${island.nextScheduledMaintenance?.let { formatTimestamp(it) } ?: "Non programmata"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = { onMarkComplete(island.id) }
                ) {
                    Text("Completata")
                }

                IconButton(
                    onClick = { onIslandClick(island.id) }
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Dettagli")
                }
            }
        }
    }
}

@Composable
private fun WarrantyIslandItem(
    island: FacilityIsland,
    onIslandClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onIslandClick(island.id) },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = island.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Scadenza garanzia: ${island.warrantyExpiration?.let { formatTimestamp(it) } ?: "Non specificata"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Dettagli",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun EmptyIslandsState(
    hasIslands: Boolean,
    filter: IslandFilter,
    onCreateIsland: () -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.PrecisionManufacturing,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val (title, message) = when {
                !hasIslands -> "Nessuna Isola" to "Non ci sono ancora isole per questo stabilimento"
                filter != IslandFilter.ALL -> "Nessun risultato" to "Non ci sono isole che corrispondono al filtro '${getIslandFilterDisplayName(filter)}'"
                else -> "Lista vuota" to "Errore nel caricamento dati"
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when {
                !hasIslands -> {
                    Button(onClick = onCreateIsland) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aggiungi prima isola")
                    }
                }
                filter != IslandFilter.ALL -> {
                    OutlinedButton(onClick = onClearFilter) {
                        Text("Rimuovi filtro")
                    }
                }
            }
        }
    }
}

@Composable
private fun IslandDeleteDialog(
    islandName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elimina Isola") },
        text = {
            Text("Sei sicuro di voler eliminare l'isola '$islandName'? Questa operazione non può essere annullata.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Elimina")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

// Helper composables e funzioni già definite prima...
@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            content()
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
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
                text = "Caricamento dettagli stabilimento...",
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
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Text(
                text = "Errore",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Stabilimento non trovato",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

// Helper functions
private fun getIslandFilterDisplayName(filter: IslandFilter): String {
    return when (filter) {
        IslandFilter.ALL -> "Tutte"
        IslandFilter.ACTIVE -> "Attive"
        IslandFilter.INACTIVE -> "Inattive"
        IslandFilter.NEEDS_MAINTENANCE -> "Da manutenere"
        IslandFilter.UNDER_WARRANTY -> "In garanzia"
        IslandFilter.BY_TYPE -> "Per tipo"
    }
}

private fun formatOperatingHours(hours: Int): String {
    return when {
        hours >= 1000 -> "${hours / 1000}K ore"
        else -> "$hours ore"
    }
}

private fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> "${number / 1_000_000}M"
        number >= 1_000 -> "${number / 1_000}K"
        else -> number.toString()
    }
}

private fun formatTimestamp(timestamp: kotlinx.datetime.Instant): String {
    val now = kotlinx.datetime.Clock.System.now()
    val diffMillis = (now - timestamp).inWholeMilliseconds

    return when {
        diffMillis < 60000 -> "Ora"
        diffMillis < 3600000 -> "${diffMillis / 60000} min fa"
        diffMillis < 86400000 -> "${diffMillis / 3600000}h fa"
        else -> "${diffMillis / 86400000} giorni fa"
    }
}