@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.presentation.screen.client.facilityisland

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.model.island.IslandType
import net.calvuz.qreport.domain.usecase.client.facilityisland.SingleIslandStatistics
import net.calvuz.qreport.domain.usecase.client.facilityisland.MaintenanceStatus
import net.calvuz.qreport.domain.usecase.client.facilityisland.WarrantyStatus
import net.calvuz.qreport.util.DateTimeUtils.toItalianDate
import kotlinx.datetime.Clock

/**
 * Screen per il dettaglio di una singola isola robotizzata
 *
 * Features:
 * - Info complete isola (tipo, serial, modello, ubicazione)
 * - Statistiche operative dettagliate con performance metrics
 * - Stato manutenzione con cronologia e programmazione
 * - Stato garanzia con scadenze
 * - Azioni rapide (modifica, elimina, manutenzione)
 * - Grafici performance e utilizzo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilityIslandDetailScreen(
    facilityId: String,
    islandId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String, String) -> Unit,
    onNavigateToMaintenance: (String) -> Unit = { },
    onIslandDeleted: () -> Unit = { },
    modifier: Modifier = Modifier,
    viewModel: FacilityIslandDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMaintenanceDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Load island details when screen opens
    LaunchedEffect(islandId) {
        viewModel.loadIslandDetails(islandId)
    }

    // Handle deletion completion
    LaunchedEffect(uiState.islandDeleted) {
        if (uiState.islandDeleted) {
            onIslandDeleted()
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = uiState.islandName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (uiState.statusText.isNotBlank()) {
                        Text(
                            text = uiState.statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                uiState.needsAttention -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
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
                // Quick maintenance action
                if (uiState.hasData && uiState.island?.needsMaintenance() == true) {
                    IconButton(
                        onClick = { showMaintenanceDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Manutenzione"
                        )
                    }
                }

                // Edit button
                if (uiState.hasData) {
                    IconButton(onClick = { onNavigateToEdit(facilityId, islandId) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifica isola"
                        )
                    }
                }

                // More actions menu
                if (uiState.hasData) {
                    var showMoreMenu by remember { mutableStateOf(false) }

                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Altre azioni"
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Registra Manutenzione") },
                            onClick = {
                                showMaintenanceDialog = true
                                showMoreMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Build, contentDescription = null)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Elimina Isola") },
                            onClick = {
                                showDeleteConfirmDialog = true
                                showMoreMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }

                // Refresh button
                IconButton(
                    onClick = viewModel::refreshData,
                    enabled = !uiState.hasOperationsInProgress
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Aggiorna"
                    )
                }
            }
        )

        when {
            uiState.isLoading -> {
                LoadingContent()
            }

            uiState.error != null && !uiState.hasData -> {
                ErrorContent(
                    error = uiState.error!!,
                    onRetry = { viewModel.loadIslandDetails(islandId) },
                    onDismiss = viewModel::dismissError
                )
            }

            uiState.hasData -> {
                IslandDetailContent(
                    island = uiState.island!!,
                    statistics = uiState.statistics,
                    error = uiState.error,
                    onDismissError = viewModel::dismissError,
                    onMaintenanceAction = { showMaintenanceDialog = true }
                )
            }

            else -> {
                EmptyContent()
            }
        }
    }

    // Maintenance Dialog
    if (showMaintenanceDialog) {
        MaintenanceDialog(
            island = uiState.island,
            onDismiss = { showMaintenanceDialog = false },
            onConfirm = { resetHours, notes ->
                viewModel.recordMaintenance(
                    maintenanceDate = Clock.System.now(),
                    resetOperatingHours = resetHours,
                    notes = notes
                )
                showMaintenanceDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        DeleteConfirmationDialog(
            island = uiState.island,
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = { force ->
                viewModel.deleteIsland(force)
                showDeleteConfirmDialog = false
            }
        )
    }
}

@Composable
private fun IslandDetailContent(
    island: FacilityIsland,
    statistics: SingleIslandStatistics?,
    error: String?,
    onDismissError: () -> Unit,
    onMaintenanceAction: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Error banner se presente
        error?.let { errorMessage ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissError) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Chiudi",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Basic Island Info
        item {
            IslandBasicInfoCard(island = island)
        }

        // Operational Statistics
        statistics?.let { stats ->
            item {
                OperationalStatsCard(statistics = stats)
            }
        }

        // Maintenance Status
        item {
            MaintenanceStatusCard(
                island = island,
                statistics = statistics,
                onMaintenanceAction = onMaintenanceAction
            )
        }

        // Warranty Status
        item {
            WarrantyStatusCard(
                island = island,
                statistics = statistics
            )
        }

        // Performance Metrics
        statistics?.let { stats ->
            item {
                PerformanceMetricsCard(statistics = stats)
            }
        }
    }
}

@Composable
private fun IslandBasicInfoCard(island: FacilityIsland) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Informazioni Generali",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Island type icon
                IslandTypeIcon(islandType = island.islandType)
            }

            Divider()

            // Info rows
            InfoRow(
                label = "Serial Number",
                value = island.serialNumber,
                icon = Icons.Outlined.Tag
            )

            island.model?.let { model ->
                InfoRow(
                    label = "Modello",
                    value = model,
                    icon = Icons.Outlined.Analytics
                )
            }

            InfoRow(
                label = "Tipo Isola",
                value = island.islandType.displayName,
                icon = Icons.Outlined.Category
            )

            island.location?.let { location ->
                InfoRow(
                    label = "Ubicazione",
                    value = location,
                    icon = Icons.Outlined.LocationOn
                )
            }

            island.installationDate?.let { installDate ->
                InfoRow(
                    label = "Data Installazione",
                    value = installDate.toItalianDate(),
                    icon = Icons.Outlined.CalendarToday
                )
            }
        }
    }
}

@Composable
private fun OperationalStatsCard(statistics: SingleIslandStatistics) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Statistiche Operative",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OperationalStatItem(
                    icon = Icons.Default.Schedule,
                    label = "Ore Totali",
                    value = "${statistics.operationalStats.operatingHours}h",
                    subtitle = "Media: ${statistics.operationalStats.averageHoursPerDay}h/giorno"
                )

                OperationalStatItem(
                    icon = Icons.Default.Repeat,
                    label = "Cicli",
                    value = formatCycleCount(statistics.operationalStats.cycleCount),
                    subtitle = "Media: ${statistics.operationalStats.averageCyclesPerHour}/ora"
                )

                OperationalStatItem(
                    icon = Icons.Default.TrendingUp,
                    label = "Uptime",
                    value = "${statistics.operationalStats.uptime}%",
                    subtitle = if (statistics.operationalStats.ageInDays > 0) "${statistics.operationalStats.ageInDays} giorni attivi" else ""
                )
            }
        }
    }
}

@Composable
private fun OperationalStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    subtitle: String = ""
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MaintenanceStatusCard(
    island: FacilityIsland,
    statistics: SingleIslandStatistics?,
    onMaintenanceAction: () -> Unit
) {
    val maintenanceStatus = statistics?.maintenanceStats?.status
    val needsMaintenance = island.needsMaintenance()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                needsMaintenance -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                maintenanceStatus == MaintenanceStatus.DUE_SOON -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stato Manutenzione",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (needsMaintenance || maintenanceStatus == MaintenanceStatus.DUE_SOON) {
                    FilledTonalButton(
                        onClick = onMaintenanceAction,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (needsMaintenance) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (needsMaintenance) "Urgente" else "Registra")
                    }
                }
            }

            Divider()

            // Stato manutenzione
            Text(
                text = island.maintenanceStatusText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = when {
                    needsMaintenance -> MaterialTheme.colorScheme.error
                    maintenanceStatus == MaintenanceStatus.DUE_SOON -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            // Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                island.lastMaintenanceDate?.let { lastDate ->
                    Column {
                        Text(
                            text = "Ultima Manutenzione",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = lastDate.toItalianDate(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                island.nextScheduledMaintenance?.let { nextDate ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Prossima Programmata",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = nextDate.toItalianDate(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (needsMaintenance) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WarrantyStatusCard(
    island: FacilityIsland,
    statistics: SingleIslandStatistics?
) {
    val warrantyStatus = statistics?.warrantyStats?.status
    val isUnderWarranty = island.isUnderWarranty()

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stato Garanzia",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Status indicator
                Surface(
                    color = when (warrantyStatus) {
                        WarrantyStatus.ACTIVE -> Color(0xFF00B050)
                        WarrantyStatus.EXPIRING_SOON -> Color(0xFFFFC000)
                        WarrantyStatus.EXPIRED -> Color(0xFFFF0000)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = warrantyStatus?.displayName ?: "Non specificata",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = when (warrantyStatus) {
                            WarrantyStatus.ACTIVE -> Color(0xFF00B050)
                            WarrantyStatus.EXPIRING_SOON -> Color(0xFFFFC000)
                            WarrantyStatus.EXPIRED -> Color(0xFFFF0000)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            HorizontalDivider()

            island.warrantyExpiration?.let { expiryDate ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Scadenza Garanzia",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = expiryDate.toItalianDate(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                statistics?.warrantyStats?.daysRemaining?.let { daysRemaining ->
                    if (daysRemaining > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Giorni Rimanenti",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "$daysRemaining giorni",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    daysRemaining <= 30 -> Color(0xFFFF0000)
                                    daysRemaining <= 90 -> Color(0xFFFFC000)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            } ?: run {
                Text(
                    text = "Nessuna informazione sulla garanzia disponibile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PerformanceMetricsCard(statistics: SingleIslandStatistics) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            HorizontalDivider()

            // Health Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Salute Generale",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )

                LinearProgressIndicator(
                    progress = { statistics.healthScore / 100f },
                    modifier = Modifier
                        .weight(2f)
                        .height(8.dp),
                    color = when {
                        statistics.healthScore >= 80 -> Color(0xFF00B050)
                        statistics.healthScore >= 60 -> Color(0xFFFFC000)
                        else -> Color(0xFFFF0000)
                    }
                )

                Text(
                    text = "${statistics.healthScore}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Summary text
            Text(
                text = statistics.summaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun IslandTypeIcon(islandType: IslandType) {
    val icon = when (islandType) {
        IslandType.POLY_MOVE -> Icons.Default.OpenWith
        IslandType.POLY_CAST -> Icons.Default.Opacity
        IslandType.POLY_EBT -> Icons.Default.ElectricalServices
        IslandType.POLY_TAG_BLE -> Icons.Default.Bluetooth
        IslandType.POLY_TAG_FC -> Icons.Default.TextFields
        IslandType.POLY_TAG_V -> Icons.Default.Visibility
        IslandType.POLY_SAMPLE -> Icons.Default.Science
    }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Icon(
            imageVector = icon,
            contentDescription = islandType.displayName,
            modifier = Modifier
                .padding(8.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun LoadingContent() {
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
                text = "Caricamento dettagli isola...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Errore",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

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

@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Nessun dato disponibile",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Dialogs
@Composable
private fun MaintenanceDialog(
    island: FacilityIsland?,
    onDismiss: () -> Unit,
    onConfirm: (resetHours: Boolean, notes: String) -> Unit
) {
    var resetHours by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registra Manutenzione") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Confermi di aver completato la manutenzione dell'isola ${island?.serialNumber}?")

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = resetHours,
                        onCheckedChange = { resetHours = it }
                    )
                    Text("Resetta ore operative")
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note (opzionale)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(resetHours, notes.ifBlank { null }.toString()) }) {
                Text("Conferma")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
private fun DeleteConfirmationDialog(
    island: FacilityIsland?,
    onDismiss: () -> Unit,
    onConfirm: (force: Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elimina Isola") },
        text = {
            Text("Sei sicuro di voler eliminare l'isola '${island?.displayName}'? Questa azione non può essere annullata.")
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(false) },
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

// Helper per formattare cicli
private fun formatCycleCount(cycleCount: Long): String {
    return when {
        cycleCount >= 1_000_000 -> "${(cycleCount / 1_000_000).toInt()}M"
        cycleCount >= 1_000 -> "${(cycleCount / 1_000).toInt()}K"
        else -> cycleCount.toString()
    }
}