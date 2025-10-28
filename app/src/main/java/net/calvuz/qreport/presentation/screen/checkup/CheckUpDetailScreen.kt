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
 * Screen per la visualizzazione e interazione con un check-up - AGGIORNATO
 *
 * Features:
 * - Header info (cliente, isola, tecnico)
 * - Progress overview
 * - Interactive checklist con moduli espandibili
 * - Quick status change per check items
 * - Aggiunta foto e note
 * - Dialog per aggiungere spare parts
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
    // CORRETTO - Una sola volta per checkUpId
    LaunchedEffect(checkUpId) {
        if (checkUpId.isNotBlank()) {
            viewModel.loadCheckUp(checkUpId)
        }
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
                        imageVector = Icons.Default.ArrowBackIosNew,
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

                    // Spare Parts Section (always show, even if empty)
                    item {
                        SparePartsSection(
                            spareParts = uiState.spareParts,
                            onAddSparePart = viewModel::showAddSparePartDialog  // ✅ CORRETTO
                        )
                    }
                }
            }
        }
    }

    // Add Spare Part Dialog
    if (uiState.showAddSparePartDialog) {
        AddSparePartDialog(
            onDismiss = viewModel::hideAddSparePartDialog,
            onConfirm = { partNumber, description, quantity, urgency, category, estimatedCost, notes, supplierInfo ->
                viewModel.addSparePart(
                    partNumber = partNumber,
                    description = description,
                    quantity = quantity,
                    urgency = urgency,
                    category = category,
                    estimatedCost = estimatedCost,
                    notes = notes,
                    supplierInfo = supplierInfo
                )
                viewModel.hideAddSparePartDialog()
            },
            isLoading = uiState.isAddingSparePart
        )
    }
}

/**
 * Dialog per aggiungere spare parts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSparePartDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, SparePartUrgency, SparePartCategory, Double?, String, String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var partNumber by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var selectedUrgency by remember { mutableStateOf(SparePartUrgency.MEDIUM) }
    var selectedCategory by remember { mutableStateOf(SparePartCategory.OTHER) }
    var estimatedCost by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var supplierInfo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text("Aggiungi Ricambio") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = partNumber,
                        onValueChange = { partNumber = it },
                        label = { Text("Numero Parte *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrizione *") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                item {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { if (it.all { char -> char.isDigit() }) quantity = it },
                        label = { Text("Quantità *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    var urgencyExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = urgencyExpanded,
                        onExpandedChange = { urgencyExpanded = !urgencyExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedUrgency.displayName,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Urgenza") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = urgencyExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = urgencyExpanded,
                            onDismissRequest = { urgencyExpanded = false }
                        ) {
                            SparePartUrgency.entries.forEach { urgency ->
                                DropdownMenuItem(
                                    text = { Text(urgency.displayName) },
                                    onClick = {
                                        selectedUrgency = urgency
                                        urgencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    var categoryExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory.displayName,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Categoria") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            SparePartCategory.entries.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.displayName) },
                                    onClick = {
                                        selectedCategory = category
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = estimatedCost,
                        onValueChange = {
                            // Allow only numbers and decimal point
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                estimatedCost = it
                            }
                        },
                        label = { Text("Costo Stimato (€)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        prefix = { Text("€ ") }
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                item {
                    OutlinedTextField(
                        value = supplierInfo,
                        onValueChange = { supplierInfo = it },
                        label = { Text("Info Fornitore") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val quantityInt = quantity.toIntOrNull() ?: 1
                    val costDouble = estimatedCost.toDoubleOrNull()

                    onConfirm(
                        partNumber.trim(),
                        description.trim(),
                        quantityInt,
                        selectedUrgency,
                        selectedCategory,
                        costDouble,
                        notes.trim(),
                        supplierInfo.trim()
                    )
                },
                enabled = !isLoading && partNumber.isNotBlank() && description.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Aggiungi")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Annulla")
            }
        }
    )
}

// ============================================================
// COMPONENTI ESISTENTI (copiati dal file originale)
// ============================================================

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
                text = "Panoramica Progresso",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.Assignment,
                    value = statistics.totalItems.toString(),
                    label = "Totali"
                )

                StatItem(
                    icon = Icons.Default.CheckCircle,
                    value = statistics.completedItems.toString(),
                    label = "Completati",
                    color = Color(0xFF4CAF50)
                )

                StatItem(
                    icon = Icons.Default.Error,
                    value = statistics.nokItems.toString(),
                    label = "NOK",
                    color = Color(0xFFF44336)
                )

                if (statistics.criticalIssues > 0) {
                    StatItem(
                        icon = Icons.Default.Warning,
                        value = statistics.criticalIssues.toString(),
                        label = "Critici",
                        color = Color(0xFFFF5722)
                    )
                }
            }
        }
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
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleSection(
    moduleType: ModuleType,
    items: List<CheckItem>,
    onItemStatusChange: (String, CheckItemStatus) -> Unit,
    onAddPhoto: (String) -> Unit,
    onUpdateNotes: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Module header
            ListItem(
                headlineContent = {
                    Text(
                        text = moduleType.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                supportingContent = {
                    val completed = items.count {
                        it.status in listOf(CheckItemStatus.OK, CheckItemStatus.NOK, CheckItemStatus.NA)
                    }
                    Text("${completed}/${items.size} completati")
                },
                trailingContent = {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Chiudi" else "Espandi"
                        )
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            )

            // Module items
            if (isExpanded) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = sparePart.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                UrgencyChip(urgency = sparePart.urgency)
            }

            Text(
                text = "P/N: ${sparePart.partNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Qtà: ${sparePart.quantity}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = sparePart.category.displayName,
                    style = MaterialTheme.typography.bodySmall
                )

                sparePart.estimatedCost?.let { cost ->
                    Text(
                        text = "€ ${String.format("%.2f", cost)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (sparePart.notes.isNotBlank()) {
                Text(
                    text = sparePart.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun UrgencyChip(
    urgency: SparePartUrgency,
    modifier: Modifier = Modifier
) {
    val colors = when (urgency) {
        SparePartUrgency.LOW -> AssistChipDefaults.assistChipColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
            labelColor = Color(0xFF2E7D32)
        )
        SparePartUrgency.MEDIUM -> AssistChipDefaults.assistChipColors(
            containerColor = Color(0xFFFF9800).copy(alpha = 0.2f),
            labelColor = Color(0xFFE65100)
        )
        SparePartUrgency.HIGH -> AssistChipDefaults.assistChipColors(
            containerColor = Color(0xFFF44336).copy(alpha = 0.2f),
            labelColor = Color(0xFFC62828)
        )
        SparePartUrgency.IMMEDIATE -> AssistChipDefaults.assistChipColors(
            containerColor = Color(0xFF9C27B0).copy(alpha = 0.2f),
            labelColor = Color(0xFF6A1B9A)
        )
    }

    AssistChip(
        onClick = { },
        label = {
            Text(
                text = urgency.displayName,
                style = MaterialTheme.typography.labelSmall
            )
        },
        colors = colors,
        modifier = modifier
    )
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