@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.presentation.feature.checkup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import net.calvuz.qreport.domain.model.*
import net.calvuz.qreport.domain.model.checkup.CheckItem
import net.calvuz.qreport.domain.model.checkup.CheckItemStatus
import net.calvuz.qreport.domain.model.checkup.CheckUpProgress
import net.calvuz.qreport.domain.model.checkup.CheckUpSingleStatistics
import net.calvuz.qreport.domain.model.checkup.CheckUpStatus
import net.calvuz.qreport.domain.model.module.ModuleType
import net.calvuz.qreport.domain.model.photo.Photo
import net.calvuz.qreport.domain.model.spare.SparePart
import net.calvuz.qreport.domain.model.spare.SparePartCategory
import net.calvuz.qreport.domain.model.spare.SparePartUrgency
import net.calvuz.qreport.presentation.feature.checkup.components.AssociationManagementDialog
import net.calvuz.qreport.presentation.feature.checkup.components.CheckUpHeaderCard

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
@Composable
fun CheckUpDetailScreen(
    checkUpId: String,
    onNavigateBack: () -> Unit,
    onNavigateToCamera: (String) -> Unit,
    onNavigateToPhotoGallery: (String) -> Unit,
    onNavigateToExportOptions: (String) -> Unit, // ✅ NUOVO
    modifier: Modifier = Modifier,
    viewModel: CheckUpDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val expandedModules by viewModel.expandedModules.collectAsStateWithLifecycle()

    // Initialize with checkUpId
    // Una sola volta per checkUpId
    LaunchedEffect(checkUpId) {
        if (checkUpId.isNotBlank()) {
            viewModel.loadCheckUp(checkUpId)
        }
    }

    // Refresh foto quando si torna dalla camera/gallery
    LaunchedEffect(uiState.photoCountsByCheckItem) {
        // Questo trigger quando cambiano i conteggi foto
        // Utile per refresh automatico
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
                            onNavigateToExportOptions(checkUpId)
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
                            progress = uiState.progress,
                            onEditHeader = viewModel::showEditHeaderDialog,
                            associations = uiState.checkUpAssociations, // ✅ NUOVO
                            onManageAssociation = viewModel::showAssociationDialog // ✅ NUOVO
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
                        item(key = "module_${moduleType.name}") {  // ✅ Chiave stabile
                            ModuleSectionWithPhotos(
                                moduleType = moduleType,
                                items = items,
                                isExpanded = moduleType.name in expandedModules,
                                onToggleExpansion = { viewModel.toggleModuleExpansion(moduleType) },
                                photosByItem = uiState.photosByCheckItem,
                                photoCountsByItem = uiState.photoCountsByCheckItem,
                                onItemStatusChange = viewModel::updateItemStatus,
                                onAddPhoto = { itemId -> onNavigateToCamera(itemId) },
                                onViewPhotos = { itemId -> onNavigateToPhotoGallery(itemId) },
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

        // Association Management Dialog
        val associationState by viewModel.associationState.collectAsStateWithLifecycle()

        if (associationState.showDialog) {
            AssociationManagementDialog(
                currentAssociations = associationState.currentAssociations,
                availableClients = associationState.availableClients,
                availableFacilities = associationState.availableFacilities,
                availableIslands = associationState.availableIslands,
                selectedClientId = associationState.selectedClientId,
                selectedFacilityId = associationState.selectedFacilityId,
                isLoading = associationState.isLoadingClients ||
                        associationState.isLoadingFacilities ||
                        associationState.isLoadingIslands,
                onDismiss = viewModel::hideAssociationDialog,
                onClientSelected = viewModel::onClientSelected,
                onFacilitySelected = viewModel::onFacilitySelected,
                onIslandSelected = viewModel::onIslandSelected,
                onRemoveAssociation = viewModel::removeAssociation
            )
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

    // Edit Header Dialog
    if (uiState.showEditHeaderDialog && uiState.checkUp != null) {
        EditHeaderDialog(
            header = uiState.checkUp!!.header,
            onDismiss = viewModel::hideEditHeaderDialog,
            onConfirm = viewModel::updateCheckUpHeader,
            isLoading = uiState.isUpdatingHeader
        )
    }
}

/**
 * Dialog per aggiungere spare parts
 */
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
private fun ProgressOverviewCard(
    progress: CheckUpProgress,
    statistics: CheckUpSingleStatistics,
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
                    icon = Icons.AutoMirrored.Default.Assignment,
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

@Composable
private fun ModuleSectionWithPhotos(
    moduleType: ModuleType,
    items: List<CheckItem>,
    isExpanded: Boolean,  // ✅ NUOVO: riceve stato da outside
    onToggleExpansion: () -> Unit,  // ✅ NUOVO: callback per toggle expansion
    photosByItem: Map<String, List<Photo>>,
    photoCountsByItem: Map<String, Int>,
    onItemStatusChange: (String, CheckItemStatus) -> Unit,
    onAddPhoto: (String) -> Unit,
    onViewPhotos: (String) -> Unit,
    onUpdateNotes: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Module Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpansion() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = moduleType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Photo count badge
                    val totalPhotos = items.sumOf { photoCountsByItem[it.id] ?: 0 }
                    if (totalPhotos > 0) {
                        PhotoCountBadge(count = totalPhotos)
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Comprimi" else "Espandi"
                    )
                }
            }

            // Module Content con animazione
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        CheckItemCardWithPhotos(
                            checkItem = item,
                            photos = photosByItem[item.id] ?: emptyList(),
                            photoCount = photoCountsByItem[item.id] ?: 0,
                            onStatusChange = onItemStatusChange,
                            onAddPhoto = onAddPhoto,
                            onViewPhotos = onViewPhotos,
                            onUpdateNotes = onUpdateNotes
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckItemCardWithPhotos(
    checkItem: CheckItem,
    photos: List<Photo>,
    photoCount: Int,
    onStatusChange: (String, CheckItemStatus) -> Unit,
    onAddPhoto: (String) -> Unit,
    onViewPhotos: (String) -> Unit,
    onUpdateNotes: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showNotesDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Check Item Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = checkItem.itemCode,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = checkItem.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                StatusChip(
                    status = checkItem.status,
                    onClick = {
                        val nextStatus = getNextStatus(checkItem.status)
                        onStatusChange(checkItem.id, nextStatus)
                    }
                )
            }

            // ✅ NUOVO: Photo Section
            PhotoSection(
                photos = photos,
                photoCount = photoCount,
                onAddPhoto = { onAddPhoto(checkItem.id) },
                onViewPhotos = { onViewPhotos(checkItem.id) }
            )

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Notes button
                TextButton(
                    onClick = { showNotesDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Notes,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Note")
                }

                // Criticality indicator
                if (checkItem.criticality == CriticalityLevel.CRITICAL) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Critico",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Notes preview
            if (checkItem.notes.isNotBlank()) {
                Text(
                    text = checkItem.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    // Notes Dialog
    if (showNotesDialog) {
        NotesDialog(
            currentNotes = checkItem.notes,
            onDismiss = { showNotesDialog = false },
            onConfirm = { newNotes ->
                onUpdateNotes(checkItem.id, newNotes)
                showNotesDialog = false
            }
        )
    }
}

@Composable
private fun NotesDialog(
    currentNotes: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var notes by remember { mutableStateOf(currentNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica Note") },
        text = {
            TextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Note") },
                placeholder = { Text("Inserisci note per questo check item...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(notes) }) {
                Text("Salva")
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
private fun PhotoSection(
    photos: List<Photo>,
    photoCount: Int,
    onAddPhoto: () -> Unit,
    onViewPhotos: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Photo preview (first 3 photos)
        if (photos.isNotEmpty()) {
            PhotoPreviewRow(
                photos = photos.take(3),
                totalCount = photoCount,
                onViewAll = onViewPhotos,
                modifier = Modifier.weight(1f)
            )
        } else {
            Text(
                text = "Nessuna foto",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Add photo button
            IconButton(
                onClick = onAddPhoto,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Scatta foto",
                    modifier = Modifier.size(16.dp)
                )
            }

            // TODO: true for the view action button visible
            // View photos button (only if has photos)
            if ( true or photos.isNotEmpty()) {
                IconButton(
                    onClick = onViewPhotos,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Visualizza foto",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoCountBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun PhotoPreviewRow(
    photos: List<Photo>,
    totalCount: Int,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Photo thumbnails (max 3)
        photos.take(3).forEach { photo ->
            AsyncImage(
                model = photo.thumbnailPath ?: photo.filePath,
                contentDescription = photo.caption.ifBlank { "Foto check item" },
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // Count indicator
        if (totalCount > 3) {
            Text(
                text = "+${totalCount - 3}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // View all button
        TextButton(
            onClick = onViewAll,
            modifier = Modifier.height(24.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Text(
                text = "Vedi tutto",
                style = MaterialTheme.typography.labelSmall
            )
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
fun StatusChipForCheckUp(
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