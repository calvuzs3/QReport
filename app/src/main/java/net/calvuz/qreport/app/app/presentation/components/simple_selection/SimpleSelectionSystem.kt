package net.calvuz.qreport.app.app.presentation.components.simple_selection

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple Selection System - Gmail Style
 *
 * Features:
 * - TopBar transformation with back button
 * - Primary actions as direct icons
 * - Secondary actions in overflow menu
 * - Feature-specific action customization
 */

/**
 * Simple selection state
 */
data class SimpleSelectionState<T>(
    val selectedItems: Set<T> = emptySet(),
    val isInSelectionMode: Boolean = false
) {
    val selectedCount: Int get() = selectedItems.size
    val hasSelection: Boolean get() = selectedItems.isNotEmpty()

    fun isSelected(item: T): Boolean = selectedItems.contains(item)
}

/**
 * Simple selection manager
 */
class SimpleSelectionManager<T> {
    private val _selectionState = MutableStateFlow(SimpleSelectionState<T>())
    val selectionState: StateFlow<SimpleSelectionState<T>> = _selectionState.asStateFlow()

    val currentState: SimpleSelectionState<T> get() = _selectionState.value

    fun enterSelectionMode(item: T) {
        _selectionState.value = SimpleSelectionState(
            selectedItems = setOf(item),
            isInSelectionMode = true
        )
    }

    fun toggleSelection(item: T) {
        val currentItems = _selectionState.value.selectedItems
        val newItems = if (currentItems.contains(item)) {
            currentItems - item
        } else {
            currentItems + item
        }

        _selectionState.value = _selectionState.value.copy(
            selectedItems = newItems,
            isInSelectionMode = newItems.isNotEmpty()
        )
    }

    fun selectAll(items: List<T>) {
        _selectionState.value = _selectionState.value.copy(
            selectedItems = items.toSet(),
            isInSelectionMode = true
        )
    }

    fun clearSelection() {
        _selectionState.value = SimpleSelectionState()
    }

    fun removeFromSelection(items: Set<T>) {
        val newSelectedItems = _selectionState.value.selectedItems - items
        _selectionState.value = _selectionState.value.copy(
            selectedItems = newSelectedItems,
            isInSelectionMode = newSelectedItems.isNotEmpty()
        )
    }
}

/**
 * Composable helper
 */
@Composable
fun <T> rememberSimpleSelectionManager(): SimpleSelectionManager<T> {
    return remember { SimpleSelectionManager<T>() }
}

/**
 * Selection actions for TopBar
 */
sealed class SelectionAction {
    abstract val icon: ImageVector
    abstract val label: String
    abstract val isDestructive: Boolean

    // Common actions
    data object Edit : SelectionAction() {
        override val icon = Icons.Default.Edit
        override val label = "Edit"
        override val isDestructive = false
    }

    data object Delete : SelectionAction() {
        override val icon = Icons.Default.Delete
        override val label = "Delete"
        override val isDestructive = true
    }

    data object SelectAll : SelectionAction() {
        override val icon = Icons.Default.SelectAll
        override val label = "Select All"
        override val isDestructive = false
    }

    // Technical Intervention specific actions
    data object SetActive : SelectionAction() {
        override val icon = Icons.Default.PlayArrow
        override val label = "Set Active"
        override val isDestructive = false
    }

    data object SetInactive : SelectionAction() {
        override val icon = Icons.Default.Pause
        override val label = "Set Inactive"
        override val isDestructive = false
    }

    data object Export : SelectionAction() {
        override val icon = Icons.Default.Download
        override val label = "Export"
        override val isDestructive = false
    }

    data object Archive : SelectionAction() {
        override val icon = Icons.Default.Archive
        override val label = "Archive"
        override val isDestructive = false
    }

    data object Renew : SelectionAction() {
        override val icon = Icons.Default.NewLabel
        override val label = "Rinnova"
        override val isDestructive = false
    }

    data object MarkCompleted : SelectionAction() {
        override val icon = Icons.Default.CheckCircle
        override val label = "Mark Completed"
        override val isDestructive = false
    }

    // Custom action for feature-specific needs
    data class Custom(
        override val icon: ImageVector,
        override val label: String,
        override val isDestructive: Boolean = false,
        val actionId: String
    ) : SelectionAction()
}

/**
 * Selection action handler interface
 */
interface SimpleSelectionActionHandler<T> {
    fun onActionClick(action: SelectionAction, selectedItems: Set<T>)
    fun isActionEnabled(action: SelectionAction, selectedItems: Set<T>): Boolean = true
    fun getDeleteConfirmationMessage(selectedItems: Set<T>): String =
        "Delete ${selectedItems.size} items?"
}

/**
 * Extension functions for easy interaction
 */
fun <T> SimpleSelectionManager<T>.handleLongPress(item: T) {
    if (!currentState.isInSelectionMode) {
        enterSelectionMode(item)
    } else {
        toggleSelection(item)
    }
}

fun <T> SimpleSelectionManager<T>.handleClick(
    item: T,
    onNormalClick: (T) -> Unit
) {
    if (currentState.isInSelectionMode) {
        toggleSelection(item)
    } else {
        onNormalClick(item)
    }
}