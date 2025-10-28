package net.calvuz.qreport.presentation.screen.checkup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.domain.model.*

/**
 * Screen per la visualizzazione e interazione con un check-up
 *
 * Features:
 * - Header info (cliente, isola, tecnico)
 * - Progress overview
 * - Interactive checklist con moduli espandibili
 * - Quick status change per check items
 * - Aggiunta foto e note
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckUpDetailScreen(
    checkUpId: String,
    onNavigateBack: () -> Unit,
    onNavigateToCamera: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckUpDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Initialize with checkUpId
    LaunchedEffect(checkUpId) {
        viewModel.loadCheckUp(checkUpId)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = uiState.checkUp?.header?.clientInfo?.companyName ?: "Check-up",
                    maxLines = 1
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Indietro"
                    )
                }
            },
            actions = {
                // Status menu
                var showStatusMenu by remember { mutableStateOf(false) }

                IconButton(onClick = { showStatusMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu"
                    )
                }

                DropdownMenu(
                    expanded = showStatusMenu,
                    onDismissRequest = { showStatusMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Completa Check-up") },
                        onClick = {
                            viewModel.completeCheckUp()
                            showStatusMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Esporta Report") },
                        onClick = {
                            viewModel.exportReport()
                            showStatusMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null)
                        }
                    )
                }
            }
        )

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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = uiState.error.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.loadCheckUp(checkUpId) }) {
                            Text("Riprova")
                        }
                    }
                }
            }

            uiState.checkUp != null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Header Card
                    item {
                        CheckUpHeaderCard(
                            checkUp = uiState.checkUp!!,
                            progress = uiState.progress
                        )
                    }

                    // Progress Overview
                    item {
                        ProgressOverviewCard(
                            progress = uiState.progress,
                            statistics = uiState.statistics
                        )
                    }

                    // Check Items by Module
                    uiState.checkItemsByModule.forEach { (moduleType, items) ->
                        item {
                            ModuleSection(
                                moduleType = moduleType,
                                items = items,
                                onItemStatusChange = viewModel::updateItemStatus,
                                onAddPhoto = { itemId -> onNavigateToCamera(itemId) },
                                onUpdateNotes = viewModel::updateItemNotes
                            )
                        }
                    }

                    // Spare Parts Section (if any)
                    if (uiState.spareParts.isNotEmpty()) {
                        item {
                            SparePartsSection(
                                spareParts = uiState.spareParts,
                                onAddSparePart = viewModel::addSparePart
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckUpHeaderCard(
    checkUp: CheckUp,
    progress: CheckUpProgress,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
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
                    text = checkUp.header.clientInfo.companyName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                StatusChipForCheckUp(status = checkUp.status)
            }

            Text(
                text = "Isola: ${checkUp.header.islandInfo.model} - ${checkUp.header.islandInfo.serialNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (checkUp.header.clientInfo.site.isNotBlank()) {
                Text(
                    text = "Sede: ${checkUp.header.clientInfo.site}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { progress.overallProgress },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "Progresso: ${(progress.overallProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProgressOverviewCard(
    progress: CheckUpProgress,
    statistics: CheckUpStatistics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Panoramica",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    icon = Icons.Default.CheckCircle,
                    value = statistics.completedItems.toString(),
                    label = "Completati",
                    color = MaterialTheme.colorScheme.primary
                )

                StatisticItem(
                    icon = Icons.Default.Error,
                    value = statistics.nokItems.toString(),
                    label = "NOK",
                    color = MaterialTheme.colorScheme.error
                )

                StatisticItem(
                    icon = Icons.Default.Schedule,
                    value = statistics.pendingItems.toString(),
                    label = "In sospeso",
                    color = MaterialTheme.colorScheme.secondary
                )

                StatisticItem(
                    icon = Icons.Default.PhotoCamera,
                    value = statistics.photosCount.toString(),
                    label = "Foto",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ModuleSection(
    moduleType: ModuleType,
    items: List<CheckItem>,
    onItemStatusChange: (String, CheckItemStatus) -> Unit,
    onAddPhoto: (String) -> Unit,
    onUpdateNotes: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Header
            Card(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (expanded) 0.dp else 12.dp,
                    bottomEnd = if (expanded) 0.dp else 12.dp
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = moduleType.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = moduleType.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${items.count { it.status == CheckItemStatus.OK }}/${items.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Comprimi" else "Espandi"
                    )
                }
            }

            // Items
            if (expanded) {
                items.forEach { item ->
                    CheckItemCard(
                        item = item,
                        onStatusChange = { status -> onItemStatusChange(item.id, status) },
                        onAddPhoto = { onAddPhoto(item.id) },
                        onUpdateNotes = { notes -> onUpdateNotes(item.id, notes) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckItemCard(
    item: CheckItem,
    onStatusChange: (CheckItemStatus) -> Unit,
    onAddPhoto: () -> Unit,
    onUpdateNotes: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )

                StatusChip(
                    status = item.status,
                    onClick = { onStatusChange(getNextStatus(item.status)) }
                )
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddPhoto,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Foto (${item.photos.size})")
                }

                if (item.notes.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Default.Note,
                        contentDescription = "Ha note",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SparePartsSection(
    spareParts: List<SparePart>,
    onAddSparePart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
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
                    text = "Ricambi Necessari",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(onClick = onAddSparePart) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi ricambio")
                }
            }

            if (spareParts.isEmpty()) {
                Text(
                    text = "Nessun ricambio segnalato",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                spareParts.forEach { sparePart ->
                    SparePartItem(sparePart = sparePart)
                }
            }
        }
    }
}

@Composable
private fun SparePartItem(
    sparePart: SparePart,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = sparePart.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Quantità: ${sparePart.quantity}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Urgenza: ${sparePart.urgency}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    status: CheckItemStatus,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = when (status) {
        CheckItemStatus.OK -> AssistChipDefaults.assistChipColors(
            containerColor = Color(0xFF4CAF50),
            labelColor = Color.White
        )
        CheckItemStatus.NOK -> AssistChipDefaults.assistChipColors(
            containerColor = Color(0xFFF44336),
            labelColor = Color.White
        )
        CheckItemStatus.PENDING -> AssistChipDefaults.assistChipColors(
            containerColor = Color(0xFFFF9800),
            labelColor = Color.White
        )
        CheckItemStatus.NA -> AssistChipDefaults.assistChipColors(
            containerColor = Color(0xFF9E9E9E),
            labelColor = Color.White
        )
    }

    if (onClick != null) {
        AssistChip(
            onClick = onClick,
            label = { Text(status.displayName) },
            colors = colors,
            modifier = modifier
        )
    } else {
        AssistChip(
            onClick = { },
            label = { Text(status.displayName) },
            colors = colors,
            modifier = modifier
        )
    }
}

// ============================================================
// HELPER FUNCTIONS
// ============================================================

private fun getNextStatus(currentStatus: CheckItemStatus): CheckItemStatus {
    return when (currentStatus) {
        CheckItemStatus.PENDING -> CheckItemStatus.OK
        CheckItemStatus.OK -> CheckItemStatus.NOK
        CheckItemStatus.NOK -> CheckItemStatus.NA
        CheckItemStatus.NA -> CheckItemStatus.PENDING
    }
}

@Composable
private fun StatusChipForCheckUp(
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