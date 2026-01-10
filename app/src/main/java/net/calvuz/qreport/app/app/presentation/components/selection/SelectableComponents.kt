package net.calvuz.qreport.app.app.presentation.components.selection

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R

/**
 * Wrapper that makes any card selectable with checkbox
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> SelectableCard(
    item: T,
    isInSelectionMode: Boolean,
    isSelected: Boolean,
    onSelectionToggle: () -> Unit,
    onLongPress: () -> Unit,
    onNormalClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isInSelectionMode) {
                        onSelectionToggle()
                    } else {
                        onNormalClick()
                    }
                },
                onLongClick = onLongPress
            )
    ) {
        // Original card content with selection overlay
        Box {
            // Original card
            content(item)

            // Selection overlay
            if (isInSelectionMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            } else {
                                Color.Transparent
                            }
                        )
                )
            }
        }

        // Checkbox
        AnimatedVisibility(
            visible = isInSelectionMode,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            SelectionCheckbox(
                isSelected = isSelected,
                onSelectionChange = { onSelectionToggle() }
            )
        }
    }
}

/**
 * Custom checkbox for selection
 */
@Composable
private fun SelectionCheckbox(
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }

    val iconColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onSelectionChange(!isSelected) },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isSelected,
            enter = scaleIn(initialScale = 0.6f),
            exit = scaleOut(targetScale = 0.6f)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }

        if (!isSelected) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

/**
 * Bottom action bar that appears when items are selected
 */
@Composable
fun <T> BottomSelectionActionBar(
    selectionState: SelectionState<T>,
    availableActions: List<BatchAction>,
    batchActionHandler: BatchActionHandler<T>,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    AnimatedVisibility(
        visible = selectionState.isInSelectionMode,
        enter = slideInVertically {
            with(density) { 80.dp.roundToPx() }
        } + fadeIn(),
        exit = slideOutVertically {
            with(density) { 80.dp.roundToPx() }
        } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column {
                // Selection summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Selection count
                    Text(
                        text = stringResource(
                            R.string.selection_summary,
                            selectionState.selectedCount
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Clear selection
                    IconButton(onClick = onClearSelection) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear_selection)
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    availableActions.forEach { action ->
                        val isAvailable = batchActionHandler.isBatchActionAvailable(
                            action,
                            selectionState.selectedItems
                        )

                        ActionChip(
                            action = action,
                            isEnabled = isAvailable,
                            onClick = {
                                if (isAvailable) {
                                    batchActionHandler.onBatchAction(
                                        action,
                                        selectionState.selectedItems
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual action chip in the bottom bar
 */
@Composable
private fun ActionChip(
    action: BatchAction,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        !isEnabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        action.isDestructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    val contentColor = when {
        !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        action.isDestructive -> MaterialTheme.colorScheme.onError
        else -> MaterialTheme.colorScheme.onPrimary
    }

    ElevatedButton(
        onClick = onClick,
        enabled = isEnabled,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getActionIcon(action),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(action.labelResId),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/**
 * Maps batch actions to Material icons
 */
private fun getActionIcon(action: BatchAction): ImageVector {
    return when (action) {
        BatchAction.SelectAll -> Icons.Default.SelectAll
        BatchAction.Edit -> Icons.Default.Edit
        BatchAction.Delete -> Icons.Default.Delete
        BatchAction.Export -> Icons.Default.FileDownload
        BatchAction.ContractBatchAction.Renew -> Icons.Default.Refresh
        BatchAction.ContractBatchAction.BulkEdit -> Icons.Default.Edit
        BatchAction.ClientBatchAction.SendNotification -> Icons.Default.Notifications
        BatchAction.ClientBatchAction.GenerateReport -> Icons.Default.Assessment
        BatchAction.CheckUpBatchAction.BulkExport -> Icons.Default.FileDownload
        BatchAction.CheckUpBatchAction.BulkFinalize -> Icons.Default.Done
    }
}

/**
 * Complete selectable list item that combines card and selection logic
 */
@Composable
fun <T> SelectableListItem(
    item: T,
    selectionManager: SelectionManager<T>,
    onNavigateToItem: (T) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    val selectionState by selectionManager.selectionState.collectAsState()
    val isSelected = selectionState.isSelected(item)

    SelectableCard(
        item = item,
        isInSelectionMode = selectionState.isInSelectionMode,
        isSelected = isSelected,
        onSelectionToggle = { selectionManager.toggleSelection(item) },
        onLongPress = { selectionManager.handleLongPress(item) },
        onNormalClick = { selectionManager.handleNormalClick(item, onNavigateToItem) },
        modifier = modifier,
        content = content
    )
}

/**
 * Batch delete confirmation dialog
 */
@Composable
fun <T> BatchDeleteConfirmationDialog(
    isVisible: Boolean,
    selectedItems: Set<T>,
    batchActionHandler: BatchActionHandler<T>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.batch_delete_dialog_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = batchActionHandler.getBatchDeleteConfirmationMessage(selectedItems),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.batch_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.batch_delete_cancel))
                }
            },
            modifier = modifier
        )
    }
}