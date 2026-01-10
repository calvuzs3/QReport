package net.calvuz.qreport.app.app.presentation.components.contextmenu

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R

/**
 * Generic context menu that can be used for any list item
 * Supports different action types and automatic styling for destructive actions
 */
@Composable
fun GenericContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    availableActions: List<ListItemAction>,
    onActionSelected: (ListItemAction) -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        availableActions.forEach { action ->
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = action.iconVector,
                            contentDescription = null,
                            tint = if (action.isDestructive) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = stringResource(action.labelResId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (action.isDestructive) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                },
                onClick = {
                    onActionSelected(action)
                    onDismiss()
                }
            )
        }
    }
}

/**
 * Generic delete confirmation dialog that works with any entity type
 */
@Composable
fun GenericDeleteConfirmationDialog(
    isVisible: Boolean,
    config: DeleteDialogConfig?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible && config != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = ListItemAction.Delete.iconVector,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = stringResource(config.titleResId),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                val itemName = config.itemNameProvider()
                val message = if (itemName != null) {
                    stringResource(config.messageResId, itemName)
                } else {
                    stringResource(config.messageResId)
                }
                Text(
                    text = message,
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
                    Text(
                        text = stringResource(R.string.context_menu_delete_confirm),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.context_menu_delete_cancel),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            modifier = modifier
        )
    }
}

/**
 * UI State for managing context menu and delete dialog
 */
data class ContextMenuState(
    val selectedItemId: String? = null,
    val showDeleteDialog: Boolean = false,
    val deleteDialogConfig: DeleteDialogConfig? = null
) {
    val showContextMenu: Boolean get() = selectedItemId != null && !showDeleteDialog
}

/**
 * Helper class to manage context menu state
 */
class ContextMenuStateManager(
    private val actionHandler: ListItemActionHandler
) {
    private val _state = mutableStateOf(ContextMenuState())
    val state: State<ContextMenuState> = _state

    fun showContextMenu(itemId: String) {
        _state.value = ContextMenuState(selectedItemId = itemId)
    }

    fun hideContextMenu() {
        _state.value = ContextMenuState()
    }

    fun showDeleteDialog(itemId: String) {
        val config = actionHandler.getDeleteDialogConfig(itemId)
        _state.value = _state.value.copy(
            showDeleteDialog = true,
            deleteDialogConfig = config
        )
    }

    fun hideDeleteDialog() {
        _state.value = _state.value.copy(
            showDeleteDialog = false,
            deleteDialogConfig = null
        )
    }

    fun handleAction(action: ListItemAction, itemId: String) {
        when (action) {
            ListItemAction.Delete -> showDeleteDialog(itemId)
            else -> {
                hideContextMenu()
                actionHandler.onActionSelected(action, itemId)
            }
        }
    }

    fun confirmDelete() {
        val itemId = _state.value.selectedItemId
        if (itemId != null) {
            hideContextMenu()
            actionHandler.onActionSelected(ListItemAction.Delete, itemId)
        }
    }

    fun getAvailableActions(itemId: String, allActions: List<ListItemAction>): List<ListItemAction> {
        return allActions.filter { action ->
            actionHandler.isActionAvailable(action, itemId)
        }
    }
}