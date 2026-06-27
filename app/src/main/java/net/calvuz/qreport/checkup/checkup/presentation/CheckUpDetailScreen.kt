@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.checkup.checkup.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import net.calvuz.qreport.R
import net.calvuz.qreport.checkup.items.domain.model.CheckItem
import net.calvuz.qreport.checkup.items.domain.model.CheckItemStatus
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUpSingleStatistics
import net.calvuz.qreport.checkup.criticality.domain.model.CriticalityLevel
import net.calvuz.qreport.checkup.modules.presentation.model.resolveModuleTypeLabel
import net.calvuz.qreport.photo.domain.model.Photo
import net.calvuz.qreport.checkup.checkup.presentation.components.AssociationManagementDialog
import net.calvuz.qreport.checkup.checkup.presentation.components.CheckUpHeaderCard
import net.calvuz.qreport.checkup.items.presentation.components.CheckupItemStatusChip
import net.calvuz.qreport.checkup.items.presentation.model.CheckItemStatusExt.getColor
import net.calvuz.qreport.checkup.items.presentation.model.CheckItemStatusExt.getNextStatus
import net.calvuz.qreport.app.app.presentation.components.ErrorDialog
import net.calvuz.qreport.photo.presentation.ui.components.PhotoCountBadge
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate
import timber.log.Timber

/**
 * Check up Detail Screen
 */
@Composable
fun CheckUpDetailScreen(
    checkUpId: String,
    onNavigateBack: () -> Unit,
    onNavigateToCamera: (String) -> Unit,
    onNavigateToPhotoGallery: (String) -> Unit,
    onNavigateToExportOptions: (String) -> Unit,
    onNavigateToDeleteCheckUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckUpDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val expandedModules by viewModel.expandedModules.collectAsStateWithLifecycle()

    // ✅ Handle delete success - Navigate back automatically
    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            viewModel.resetDeleteState()
            onNavigateToDeleteCheckUp()  // Navigate back to client list
        }
    }

    // ✅ Delete confirmation dialog
    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::hideDeleteConfirmation,
            title = { Text(stringResource(R.string.checkup_screen_detail_action_delete)) },
            text = {
                Text(stringResource(R.string.checkup_screen_detail_action_delete_confirmation, uiState.checkUp?.header?.checkUpDate?.toItalianDate() as Any))
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteCheckUp,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDeleteConfirmation) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Initialize with checkUpId
    LaunchedEffect(checkUpId) {
        if (checkUpId.isNotBlank()) {
            viewModel.loadCheckUp(checkUpId)
        }
    }

    // Refresh photos when come back from camera/gallery
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
                    text = stringResource(R.string.checkup_screen_detail_title_default), // "Check-up",
                    maxLines = 1
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.checkup_screen_detail_back)
                    )
                }
            },
            actions = {
                // Status menu
                var showStatusMenu by remember { mutableStateOf(false) }

                // Delete button
                if (uiState.hasData) {
                    IconButton(
                        onClick = viewModel::showDeleteConfirmation,  // Show confirmation dialog
                        enabled = !uiState.isDeleting
                    ) {
                        if (uiState.isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                tint = MaterialTheme.colorScheme.error,
                                contentDescription = stringResource(R.string.checkup_screen_detail_delete)
                            )
                        }
                    }
                }

                IconButton(onClick = { showStatusMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.checkup_screen_detail_menu)
                    )
                }

                DropdownMenu(
                    expanded = showStatusMenu,
                    onDismissRequest = { showStatusMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.checkup_screen_detail_action_complete)) },
                        onClick = {
                            viewModel.completeCheckUp()
                            showStatusMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.checkup_screen_detail_action_export)) },
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
                val error = uiState.error

                ErrorDialog(
                    title = error!!.asString(),
                    message = error.asString(),
                    onDismiss = { viewModel.loadCheckUp(checkUpId) }
                )
            }

            uiState.checkUp != null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Header Card
                    item {
                        CheckUpHeaderCard(
                            checkUp = uiState.checkUp!!,
                            statusMaster = uiState.statusMaster,
                            progress = uiState.progress,
                            onEditHeader = viewModel::showEditHeaderDialog,
                            associations = uiState.checkUpAssociations,
                            onManageAssociation = viewModel::showAssociationDialog
                        )
                    }

                    // Progress Overview
                    item {
                        ProgressOverviewCard(
                            statistics = uiState.statistics
                        )
                    }

                    // Check Items by Module
                    uiState.checkItemsByModule.forEach { (moduleKey, items) ->
                        item(key = "module_$moduleKey") {
                            ModuleSectionWithPhotos(
                                displayLabel = resolveModuleTypeLabel(
                                    moduleKey = moduleKey,
                                    masters = uiState.moduleTypes
                                ),
                                items = items,
                                isExpanded = moduleKey in expandedModules,
                                onToggleExpansion = { viewModel.toggleModuleExpansion(moduleKey) },
                                photosByItem = uiState.photosByCheckItem,
                                photoCountsByItem = uiState.photoCountsByCheckItem,
                                onItemStatusChange = viewModel::updateItemStatus,
                                onAddPhoto = { itemId -> onNavigateToCamera(itemId) },
                                onViewPhotos = { itemId -> onNavigateToPhotoGallery(itemId) },
                                onUpdateNotes = viewModel::updateItemNotes
                            )
                        }
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
                islandTypes = associationState.islandTypes,
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


// ============================================================
// COMPONENTI ESISTENTI (copiati dal file originale)
// ============================================================

@Composable
private fun ProgressOverviewCard(
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
                text = stringResource(R.string.checkup_screen_detail_progress_title) ,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatItem(
                    icon = Icons.AutoMirrored.Default.Assignment,
                    value = statistics.totalItems.toString(),
                    label = stringResource(R.string.checkup_screen_detail_progress_total_label),
                    modifier = Modifier.weight(1f)
                )

                StatItem(
                    icon = Icons.Default.CheckCircle,
                    value = statistics.completedItems.toString(),
                    label = stringResource(R.string.checkup_screen_detail_progress_completed_label),
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )

                StatItem(
                    icon = Icons.Default.Error,
                    value = statistics.nokItems.toString(),
                    label = stringResource(R.string.checkup_screen_detail_progress_nok_label),
                    color = Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )

                StatItem(
                    icon = Icons.Default.Warning,
                    value = statistics.criticalIssues.toString(),
                    label = stringResource(R.string.checkup_screen_detail_progress_critical_label),
                    color = if (statistics.criticalIssues > 0) Color(0xFFFF5722) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
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
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ModuleSectionWithPhotos(
    displayLabel: String,
    items: List<CheckItem>,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    photosByItem: Map<String, List<Photo>>,
    photoCountsByItem: Map<String, Int>,
    onItemStatusChange: (String, CheckItemStatus) -> Unit,
    onAddPhoto: (String) -> Unit,
    onViewPhotos: (String) -> Unit,
    onUpdateNotes: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {

    val okCount = items.count { it.status == CheckItemStatus.OK }
    val nokCount = items.count { it.status == CheckItemStatus.NOK }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(
                            R.string.checkup_screen_detail_module_summary,
                            okCount,
                            items.size
                        ) + if (nokCount > 0) " • $nokCount NOK" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (nokCount > 0)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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
                        contentDescription = if (isExpanded)
                            stringResource(R.string.checkup_screen_detail_module_collapse) else stringResource(R.string.checkup_screen_detail_module_expand)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Status color stripe - quick visual scan without opening the item
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(checkItem.status.getColor())
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Check Item Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = checkItem.itemCode,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (checkItem.criticality == CriticalityLevel.CRITICAL) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = stringResource(R.string.checkup_screen_detail_item_critical),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = checkItem.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    CheckupItemStatusChip(
                        status = checkItem.status,
                        onClick = {
                            val nextStatus = checkItem.status.getNextStatus()
                            onStatusChange(checkItem.id, nextStatus)
                        }
                    )
                }

                // Photo Section
                PhotoSection(
                    photos = photos,
                    photoCount = photoCount,
                    onAddPhoto = { onAddPhoto(checkItem.id) },
                    onViewPhotos = { onViewPhotos(checkItem.id) }
                )

                // Notes button
                TextButton(
                    onClick = { showNotesDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Notes,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.checkup_screen_detail_item_notes_button))
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
        title = { Text(stringResource(R.string.checkup_screen_detail_dialog_notes_title)) },
        text = {
            TextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.checkup_screen_detail_dialog_notes_label)) },
                placeholder = { Text(stringResource(R.string.checkup_screen_detail_dialog_notes_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(notes) }) {
                Text(stringResource(R.string.checkup_screen_detail_dialog_notes_action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.checkup_screen_detail_dialog_notes_action_cancel))
            }
        },
        modifier = modifier
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
                text = stringResource(R.string.checkup_screen_detail_photo_empty),
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
                    contentDescription = stringResource(R.string.checkup_screen_detail_photo_action_take),
                    modifier = Modifier.size(16.dp)
                )
            }

            // View photos button (only if has photos)
            if (photos.isNotEmpty()) {
                IconButton(
                    onClick = onViewPhotos,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = stringResource(R.string.checkup_screen_detail_photo_action_view),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
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
                contentDescription = photo.caption.ifBlank { stringResource(R.string.checkup_screen_detail_photo_caption_default) },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // Count indicator
        if (totalCount > 3) {
            Text(
                text = stringResource(R.string.checkup_screen_detail_photo_count_more, totalCount - 3),
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
                text = stringResource(R.string.checkup_screen_detail_photo_view_all),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
