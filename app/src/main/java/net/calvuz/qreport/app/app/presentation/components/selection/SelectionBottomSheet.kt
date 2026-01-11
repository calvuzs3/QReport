package net.calvuz.qreport.app.app.presentation.components.selection

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R

/**
 * Bottom Sheet for batch selection actions - opens on demand
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectionBottomSheet(
    selectionState: SelectionState<T>,
    availableActions: List<BatchAction>,
    batchActionHandler: BatchActionHandler<T>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp) // Extra padding for gesture indicator
        ) {
            // Header with selection count
            SelectionSheetHeader(
                selectionCount = selectionState.selectedCount,
                onClose = onDismiss
            )

            // Action list
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(availableActions) { action ->
                    val isAvailable = batchActionHandler.isBatchActionAvailable(
                        action,
                        selectionState.selectedItems
                    )

                    SelectionActionItem(
                        action = action,
                        isEnabled = isAvailable,
                        onClick = {
                            if (isAvailable) {
                                batchActionHandler.onBatchAction(
                                    action,
                                    selectionState.selectedItems
                                )
                                onDismiss() // Close sheet after action
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Header section of the bottom sheet
 */
@Composable
private fun SelectionSheetHeader(
    selectionCount: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .size(width = 32.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.selection_actions_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.selection_summary, selectionCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close_selection_actions)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }
}

/**
 * Individual action item in the bottom sheet
 */
@Composable
private fun SelectionActionItem(
    action: BatchAction,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isEnabled) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
    }

    val contentColor = when {
        !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        action.isDestructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = isEnabled) { onClick() },
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Action icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (action.isDestructive && isEnabled) {
                            MaterialTheme.colorScheme.errorContainer
                        } else if (isEnabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getActionIcon(action),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = when {
                        !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        action.isDestructive -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }

            // Action text
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(action.labelResId),
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Medium
                )

                // Optional subtitle for some actions
                val subtitle = getActionSubtitle(action)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }

            // Disabled indicator or arrow
            if (!isEnabled) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = stringResource(R.string.action_not_available),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Compact selection indicator that floats above the list
 */
@Composable
fun <T> FloatingSelectionIndicator(
    selectionState: SelectionState<T>,
    onOpenBottomSheet: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = selectionState.isInSelectionMode,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onOpenBottomSheet() },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Selection count badge
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${selectionState.selectedCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.items_selected),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.tap_for_actions),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clear selection button
                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear_selection),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Actions indicator
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.selection_actions),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Maps batch actions to Material icons (same as before)
 */
private fun getActionIcon(action: BatchAction): ImageVector {
    return when (action) {
        BatchAction.SelectAll -> Icons.Default.SelectAll
        BatchAction.Edit -> Icons.Default.Edit
        BatchAction.Delete -> Icons.Default.Delete
        BatchAction.Export -> Icons.Default.FileDownload
        BatchAction.ContractBatchAction.Renew -> Icons.Default.Refresh
        BatchAction.ContractBatchAction.BulkEdit -> Icons.Default.EditNote
        BatchAction.ClientBatchAction.SendNotification -> Icons.Default.Notifications
        BatchAction.ClientBatchAction.GenerateReport -> Icons.Default.Assessment
        BatchAction.CheckUpBatchAction.BulkExport -> Icons.Default.Archive
        BatchAction.CheckUpBatchAction.BulkFinalize -> Icons.Default.Done
    }
}

/**
 * Provides helpful subtitles for some actions
 */
@Composable
private fun getActionSubtitle(action: BatchAction): String? {
    return when (action) {
        BatchAction.Delete -> stringResource(R.string.action_subtitle_delete)
        BatchAction.ContractBatchAction.Renew -> stringResource(R.string.action_subtitle_renew)
        BatchAction.Export -> stringResource(R.string.action_subtitle_export)
        BatchAction.SelectAll -> stringResource(R.string.action_subtitle_select_all)
        else -> null
    }
}