package net.calvuz.qreport.app.app.presentation.components.contextmenu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Generic wrapper that adds context menu support to any card component
 * This allows existing cards to gain long-press functionality without modification
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> CardWithContextMenu(
    item: T,
    itemId: String,
    onClick: (T) -> Unit,
    onLongClick: (String) -> Unit,
    availableActions: List<ListItemAction>,
    contextMenuState: ContextMenuState,
    onActionSelected: (ListItemAction, String) -> Unit,
    onContextMenuDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T, () -> Unit) -> Unit
) {
    Box(modifier = modifier) {
        // Render the original card content with click handling
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick(item) },
                    onLongClick = { onLongClick(itemId) }
                )
        ) {
            content(item) { /* onClick handled by combinedClickable */ }
        }

        // Show context menu if this item is selected
        if (contextMenuState.showContextMenu && contextMenuState.selectedItemId == itemId) {
            GenericContextMenu(
                expanded = true,
                onDismiss = onContextMenuDismiss,
                availableActions = availableActions,
                onActionSelected = { action -> onActionSelected(action, itemId) }
            )
        }
    }
}

/**
 * Convenience modifier that adds context menu support to any composable
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.withContextMenu(
    itemId: String,
    onNormalClick: () -> Unit,
    onLongClick: (String) -> Unit
): Modifier = composed {
    this.combinedClickable(
        onClick = onNormalClick,
        onLongClick = { onLongClick(itemId) }
    )
}

/**
 * Complete context menu integration for list screens
 * Handles all state management and rendering
 */
@Composable
fun <T> ListItemWithContextMenu(
    item: T,
    itemId: String,
    availableActions: List<ListItemAction>,
    actionHandler: ListItemActionHandler,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T, () -> Unit) -> Unit
) {
    val stateManager = remember { ContextMenuStateManager(actionHandler) }
    val contextMenuState by stateManager.state

    // Filter available actions for this specific item
    val filteredActions = remember(itemId, availableActions) {
        stateManager.getAvailableActions(itemId, availableActions)
    }

    Box(modifier = modifier) {
        CardWithContextMenu(
            item = item,
            itemId = itemId,
            onClick = onClick,
            onLongClick = stateManager::showContextMenu,
            availableActions = filteredActions,
            contextMenuState = contextMenuState,
            onActionSelected = stateManager::handleAction,
            onContextMenuDismiss = stateManager::hideContextMenu,
            content = content
        )

        // Delete confirmation dialog
        GenericDeleteConfirmationDialog(
            isVisible = contextMenuState.showDeleteDialog,
            config = contextMenuState.deleteDialogConfig,
            onConfirm = stateManager::confirmDelete,
            onDismiss = stateManager::hideDeleteDialog
        )
    }
}

/**
 * Extension function to create a list of common actions for different entity types
 */
fun createActionsList(vararg actions: ListItemAction): List<ListItemAction> = actions.toList()

/**
 * Predefined action sets for common entity types
 */
object CommonActionSets {
    val basicCrud = listOf(
        ListItemAction.ViewDetails,
        ListItemAction.Edit,
        ListItemAction.Delete
    )

    val crudWithDuplicate = listOf(
        ListItemAction.ViewDetails,
        ListItemAction.Edit,
        ListItemAction.Duplicate,
        ListItemAction.Delete
    )

    val clientActions = listOf(
        ListItemAction.ViewDetails,
        ListItemAction.Edit,
        ListItemAction.ClientAction.ViewFacilities,
        ListItemAction.ClientAction.ViewContracts,
        ListItemAction.Delete
    )

    val facilityActions = listOf(
        ListItemAction.ViewDetails,
        ListItemAction.Edit,
        ListItemAction.FacilityAction.ViewIslands,
        ListItemAction.FacilityAction.ViewCheckUps,
        ListItemAction.Delete
    )

    val contractActions = listOf(
        ListItemAction.ContractAction.ViewDetails,
        ListItemAction.Edit,
        ListItemAction.ContractAction.Renew,
        ListItemAction.Delete
    )

    val checkUpActions = listOf(
        ListItemAction.ViewDetails,
        ListItemAction.CheckUpAction.Continue,
        ListItemAction.CheckUpAction.Export,
        ListItemAction.CheckUpAction.Finalize,
        ListItemAction.Edit,
        ListItemAction.Delete
    )

    val contactActions = listOf(
        ListItemAction.ViewDetails,
        ListItemAction.Edit,
        ListItemAction.ContactAction.Call,
        ListItemAction.ContactAction.Email,
        ListItemAction.Delete
    )

    val sparePartActions = listOf(
        ListItemAction.ViewDetails,
        ListItemAction.Edit,
        ListItemAction.SparePartAction.ViewHistory,
        ListItemAction.SparePartAction.AddStock,
        ListItemAction.Delete
    )

    val islandActions = listOf(
        ListItemAction.ViewDetails,
        ListItemAction.Edit,
        ListItemAction.IslandAction.ViewMaintenance,
        ListItemAction.IslandAction.ScheduleCheckUp,
        ListItemAction.Delete
    )
}