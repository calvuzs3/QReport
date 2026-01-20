package net.calvuz.qreport.app.app.presentation.components.simple_selection

import net.calvuz.qreport.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Selection TopBar - Gmail Style
 * Transforms normal TopBar into selection mode with back button, counter, and actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectionTopBar(
    normalTopBar: @Composable () -> Unit,
    selectionManager: SimpleSelectionManager<T>,
    primaryActions: List<SelectionAction>,
    secondaryActions: List<SelectionAction>,
    actionHandler: SimpleSelectionActionHandler<T>
) {
    val selectionState by selectionManager.selectionState.collectAsState()

    if (selectionState.isInSelectionMode) {
        // Selection mode TopBar
        TopAppBar(
            title = {
                Text(
                    text = "${selectionState.selectedCount}",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = { selectionManager.clearSelection() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.action_exit_selection)
                    )
                }
            },
            actions = {
                // Primary actions as direct icons
                primaryActions.forEach { action ->
                    val isEnabled = actionHandler.isActionEnabled(action, selectionState.selectedItems)

                    IconButton(
                        onClick = {
                            if (isEnabled) {
                                actionHandler.onActionClick(action, selectionState.selectedItems)
                            }
                        },
                        enabled = isEnabled
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.label,
                            tint = if (action.isDestructive && isEnabled) {
                                MaterialTheme.colorScheme.error
                            } else if (isEnabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }
                }

                // Secondary actions in overflow menu
                if (secondaryActions.isNotEmpty()) {
                    var showMenu by remember { mutableStateOf(false) }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.action_more_vert)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            secondaryActions.forEach { action ->
                                val isEnabled = actionHandler.isActionEnabled(action, selectionState.selectedItems)

                                DropdownMenuItem(
                                    text = { Text(action.label) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = action.icon,
                                            contentDescription = null,
                                            tint = if (action.isDestructive && isEnabled) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                LocalContentColor.current
                                            }
                                        )
                                    },
                                    onClick = {
                                        if (isEnabled) {
                                            actionHandler.onActionClick(action, selectionState.selectedItems)
                                            showMenu = false
                                        }
                                    },
                                    enabled = isEnabled
                                )
                            }
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    } else {
        // Normal TopBar
        normalTopBar()
    }
}

/**
 * Selectable Item Wrapper
 * Handles click and long press for selection
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> SelectableItem(
    item: T,
    selectionManager: SimpleSelectionManager<T>,
    onNormalClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (isSelected: Boolean) -> Unit
) {
    val selectionState by selectionManager.selectionState.collectAsState()
    val isSelected = selectionState.isSelected(item)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    selectionManager.handleClick(item, onNormalClick)
                },
                onLongClick = {
                    selectionManager.handleLongPress(item)
                }
            )
    ) {
        // Item content with selection overlay
        Box {
            content(isSelected)

            // Selection overlay
            if (selectionState.isInSelectionMode && isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                )
            }
        }

        // Selection indicator
        if (selectionState.isInSelectionMode) {
            SelectionIndicator(
                isSelected = isSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            )
        }
    }
}

/**
 * Selection indicator (checkmark circle)
 */
@Composable
private fun SelectionIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        } else {
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
                        .background(borderColor)
                )
            }
        }
    }
}

/**
 * Delete confirmation dialog
 */
@Composable
fun <T> DeleteConfirmationDialog(
    isVisible: Boolean,
    selectedItems: Set<T>,
    actionHandler: SimpleSelectionActionHandler<T>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
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
                Text(stringResource(R.string.action_confirm))
            },
            text = {
                Text(actionHandler.getDeleteConfirmationMessage(selectedItems))
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
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}