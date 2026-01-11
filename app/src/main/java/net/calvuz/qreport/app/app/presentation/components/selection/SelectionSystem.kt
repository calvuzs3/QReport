package net.calvuz.qreport.app.app.presentation.components.selection

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.calvuz.qreport.app.error.presentation.UiText

/**
 * Generic selection state for multi-select functionality
 */
data class SelectionState<T>(
    val selectedItems: Set<T> = emptySet(),
    val isInSelectionMode: Boolean = false
) {
    val selectedCount: Int get() = selectedItems.size
    val hasSelection: Boolean get() = selectedItems.isNotEmpty()

    fun isSelected(item: T): Boolean = selectedItems.contains(item)

    fun toggleSelection(item: T): SelectionState<T> {
        val newSelectedItems = if (isSelected(item)) {
            selectedItems - item
        } else {
            selectedItems + item
        }

        return copy(
            selectedItems = newSelectedItems,
            isInSelectionMode = newSelectedItems.isNotEmpty()
        )
    }

    fun selectAll(items: List<T>): SelectionState<T> {
        return copy(
            selectedItems = items.toSet(),
            isInSelectionMode = true
        )
    }

    fun clearSelection(): SelectionState<T> {
        return copy(
            selectedItems = emptySet(),
            isInSelectionMode = false
        )
    }

    fun removeFromSelection(items: Set<T>): SelectionState<T> {
        val newSelectedItems = selectedItems - items
        return copy(
            selectedItems = newSelectedItems,
            isInSelectionMode = newSelectedItems.isNotEmpty()
        )
    }
}

/**
 * Generic selection manager for handling multi-select state
 */
class SelectionManager<T> {
    private val _selectionState = MutableStateFlow(SelectionState<T>())
    val selectionState: StateFlow<SelectionState<T>> = _selectionState.asStateFlow()

    val currentState: SelectionState<T> get() = _selectionState.value

    fun toggleSelection(item: T) {
        _selectionState.value = _selectionState.value.toggleSelection(item)
    }

    fun selectAll(items: List<T>) {
        _selectionState.value = _selectionState.value.selectAll(items)
    }

    fun clearSelection() {
        _selectionState.value = _selectionState.value.clearSelection()
    }

    fun removeFromSelection(items: Set<T>) {
        _selectionState.value = _selectionState.value.removeFromSelection(items)
    }

    fun isSelected(item: T): Boolean {
        return _selectionState.value.isSelected(item)
    }

    fun enterSelectionMode(initialItem: T) {
        _selectionState.value = _selectionState.value.copy(
            selectedItems = setOf(initialItem),
            isInSelectionMode = true
        )
    }
}

/**
 * Composable helper for managing selection state
 */
@Composable
fun <T> rememberSelectionManager(): SelectionManager<T> {
    return remember { SelectionManager<T>() }
}

/**
 * Batch action interface for multi-select operations
 */
sealed interface BatchAction {
    val iconResId: Int
    val labelResId: Int
    val isDestructive: Boolean get() = false

    data object SelectAll : BatchAction {
        override val iconResId = net.calvuz.qreport.R.drawable.select_all_24
        override val labelResId = net.calvuz.qreport.R.string.batch_action_select_all
    }

    data object Edit : BatchAction {
        override val iconResId = android.R.drawable.ic_menu_edit
        override val labelResId = net.calvuz.qreport.R.string.batch_action_edit
    }

    data object Delete : BatchAction {
        override val iconResId = android.R.drawable.ic_menu_delete
        override val labelResId = net.calvuz.qreport.R.string.batch_action_delete
        override val isDestructive = true
    }

    data object Export : BatchAction {
        override val iconResId = android.R.drawable.stat_sys_download
        override val labelResId = net.calvuz.qreport.R.string.batch_action_export
    }

    // Entity-specific batch actions
    sealed interface ContractBatchAction : BatchAction {
        data object Renew : ContractBatchAction {
            override val iconResId = android.R.drawable.ic_popup_sync
            override val labelResId = net.calvuz.qreport.R.string.batch_action_renew_contracts
        }

        data object BulkEdit : ContractBatchAction {
            override val iconResId = android.R.drawable.ic_menu_edit
            override val labelResId = net.calvuz.qreport.R.string.batch_action_bulk_edit_contracts
        }
    }

    // Contact specific batch actions
    sealed interface ContactBatchAction : BatchAction {
    }

    sealed interface ClientBatchAction : BatchAction {
        data object SendNotification : ClientBatchAction {
            override val iconResId = android.R.drawable.ic_dialog_email
            override val labelResId = net.calvuz.qreport.R.string.batch_action_send_notification
        }

        data object GenerateReport : ClientBatchAction {
            override val iconResId = android.R.drawable.ic_menu_report_image
            override val labelResId = net.calvuz.qreport.R.string.batch_action_generate_report
        }
    }

    sealed interface CheckUpBatchAction : BatchAction {
        data object BulkExport : CheckUpBatchAction {
            override val iconResId = android.R.drawable.stat_sys_download
            override val labelResId = net.calvuz.qreport.R.string.batch_action_bulk_export
        }

        data object BulkFinalize : CheckUpBatchAction {
            override val iconResId = android.R.drawable.ic_menu_save
            override val labelResId = net.calvuz.qreport.R.string.batch_action_bulk_finalize
        }
    }
}

/**
 * Batch action handler interface
 */
interface BatchActionHandler<T> {
    fun onBatchAction(action: BatchAction, selectedItems: Set<T>)
    fun isBatchActionAvailable(action: BatchAction, selectedItems: Set<T>): Boolean = true
    fun getBatchDeleteConfirmationMessage(selectedItems: Set<T>): UiText
}

/**
 * Predefined batch action sets for different entity types
 */
object BatchActionSets {
    val common = listOf(
        BatchAction.SelectAll,
        BatchAction.Delete
    )

    val contractActions = listOf(
        BatchAction.SelectAll,
        BatchAction.Edit,
        BatchAction.ContractBatchAction.Renew,
        BatchAction.Export,
        BatchAction.Delete
    )

    val contactActions = common

    val clientActions = listOf(
        BatchAction.SelectAll,
        BatchAction.Edit,
        BatchAction.ClientBatchAction.SendNotification,
        BatchAction.ClientBatchAction.GenerateReport,
        BatchAction.Delete
    )

    val checkUpActions = listOf(
        BatchAction.SelectAll,
        BatchAction.CheckUpBatchAction.BulkExport,
        BatchAction.CheckUpBatchAction.BulkFinalize,
        BatchAction.Delete
    )

    val facilityActions = listOf(
        BatchAction.SelectAll,
        BatchAction.Edit,
        BatchAction.Export,
        BatchAction.Delete
    )
}

/**
 * Extension functions for common operations
 */
fun <T> SelectionManager<T>.toggleItem(item: T) {
    if (!currentState.isInSelectionMode) {
        enterSelectionMode(item)
    } else {
        toggleSelection(item)
    }
}

fun <T> SelectionManager<T>.handleLongPress(item: T) {
    if (!currentState.isInSelectionMode) {
        enterSelectionMode(item)
    }
}

fun <T> SelectionManager<T>.handleNormalClick(item: T, onNavigate: (T) -> Unit) {
    if (currentState.isInSelectionMode) {
        toggleSelection(item)
    } else {
        onNavigate(item)
    }
}