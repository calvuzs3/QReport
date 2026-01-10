package net.calvuz.qreport.app.app.presentation.components.contextmenu

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import net.calvuz.qreport.R

/**
 * Generic sealed interface for context menu actions
 * Provides type-safe actions for all list items in the app
 */
sealed interface ListItemAction {
    val iconVector: ImageVector
    @get:StringRes val labelResId: Int
    val isDestructive: Boolean get() = false

    // Common actions available for most list items
    data object Edit : ListItemAction {
        override val iconVector = Icons.Default.Edit
        override val labelResId = R.string.context_menu_action_edit
    }

    data object Delete : ListItemAction {
        override val iconVector = Icons.Default.Delete
        override val labelResId = R.string.context_menu_action_delete
        override val isDestructive = true
    }

    data object ViewDetails : ListItemAction {
        override val iconVector = Icons.Default.Visibility
        override val labelResId = R.string.context_menu_action_view_details
    }

    data object Duplicate : ListItemAction {
        override val iconVector = Icons.Default.ContentCopy
        override val labelResId = R.string.context_menu_action_duplicate
    }

    // Context-specific actions
    sealed interface ClientAction : ListItemAction {
        data object ViewFacilities : ClientAction {
            override val iconVector = Icons.Default.Factory
            override val labelResId = R.string.context_menu_action_view_facilities
        }

        data object ViewContracts : ClientAction {
            override val iconVector = Icons.Default.Assignment
            override val labelResId = R.string.context_menu_action_view_contracts
        }
    }

    sealed interface FacilityAction : ListItemAction {
        data object ViewIslands : FacilityAction {
            override val iconVector = Icons.Default.PrecisionManufacturing
            override val labelResId = R.string.context_menu_action_view_islands
        }

        data object ViewCheckUps : FacilityAction {
            override val iconVector = Icons.Default.CheckCircle
            override val labelResId = R.string.context_menu_action_view_checkups
        }
    }

    sealed interface CheckUpAction : ListItemAction {
        data object Export : CheckUpAction {
            override val iconVector = Icons.Default.FileDownload
            override val labelResId = R.string.context_menu_action_export
        }

        data object Continue : CheckUpAction {
            override val iconVector = Icons.Default.PlayArrow
            override val labelResId = R.string.context_menu_action_continue
        }

        data object Finalize : CheckUpAction {
            override val iconVector = Icons.Default.Done
            override val labelResId = R.string.context_menu_action_finalize
        }
    }

    sealed interface ContractAction : ListItemAction {
        data object Renew : ContractAction {
            override val iconVector = Icons.Default.Refresh
            override val labelResId = R.string.context_menu_action_renew
        }

        data object ViewDetails : ContractAction {
            override val iconVector = Icons.Default.Description
            override val labelResId = R.string.context_menu_action_view_contract_details
        }
    }

    sealed interface IslandAction : ListItemAction {
        data object ViewMaintenance : IslandAction {
            override val iconVector = Icons.Default.Build
            override val labelResId = R.string.context_menu_action_view_maintenance
        }

        data object ScheduleCheckUp : IslandAction {
            override val iconVector = Icons.Default.Schedule
            override val labelResId = R.string.context_menu_action_schedule_checkup
        }
    }

    sealed interface ContactAction : ListItemAction {
        data object Call : ContactAction {
            override val iconVector = Icons.Default.Phone
            override val labelResId = R.string.context_menu_action_call
        }

        data object Email : ContactAction {
            override val iconVector = Icons.Default.Email
            override val labelResId = R.string.context_menu_action_email
        }
    }

    sealed interface SparePartAction : ListItemAction {
        data object ViewHistory : SparePartAction {
            override val iconVector = Icons.Default.History
            override val labelResId = R.string.context_menu_action_view_history
        }

        data object AddStock : SparePartAction {
            override val iconVector = Icons.Default.Add
            override val labelResId = R.string.context_menu_action_add_stock
        }
    }
}

/**
 * Configuration for delete confirmation dialog
 */
data class DeleteDialogConfig(
    @StringRes val titleResId: Int,
    @StringRes val messageResId: Int,
    val itemNameProvider: () -> String?
)

/**
 * Generic callback interface for handling context menu actions
 */
interface ListItemActionHandler {
    fun onActionSelected(action: ListItemAction, itemId: String)
    fun getDeleteDialogConfig(itemId: String): DeleteDialogConfig?
    fun isActionAvailable(action: ListItemAction, itemId: String): Boolean = true
}