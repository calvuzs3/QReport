package net.calvuz.qreport.client.contract.presentation.ui

import androidx.compose.runtime.*
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.contextmenu.*
import net.calvuz.qreport.client.contract.domain.model.Contract
import timber.log.Timber
import kotlin.time.Duration.Companion.days

/**
 * Contract-specific configuration for the generic context menu system
 * This shows how to customize the generic system for specific entity needs
 */
object ContractContextMenuConfig {

    /**
     * Defines the available actions for contracts
     * Uses the predefined contract action set with customizations
     */
    val contractActions = listOf(
        ListItemAction.ViewDetails,
        ListItemAction.Edit,
        ListItemAction.ContractAction.Renew,
        ListItemAction.Delete
    )

    /**
     * Alternative action set for different contract states
     */
    val contractActionsForActiveContracts = listOf(
        ListItemAction.ViewDetails,
        ListItemAction.Edit,
        //ListItemAction.ContractAction.ViewDetails,
        ListItemAction.Delete
    )

    val contractActionsForExpiredContracts = listOf(
        ListItemAction.ViewDetails,
        ListItemAction.Edit,
        ListItemAction.ContractAction.Renew,
        ListItemAction.Delete
    )

    /**
     * Creates a specialized action handler for contracts
     */
    fun createContractActionHandler(
        contractsWithStats: List<ContractWithStats>,
        onEdit: (String) -> Unit,
        onDelete: (String) -> Unit,
        onViewDetails: (String) -> Unit,
        onRenewContract: (String) -> Unit
    ): ListItemActionHandler {
        return object : ListItemActionHandler {
            override fun onActionSelected(action: ListItemAction, itemId: String) {
                Timber.d("Contract action selected: $action for contract: $itemId")

                when (action) {
                    ListItemAction.Edit -> {
                        Timber.d("Navigating to edit contract: $itemId")
                        onEdit(itemId)
                    }
                    ListItemAction.Delete -> {
                        Timber.d("Deleting contract: $itemId")
                        onDelete(itemId)
                    }
                    ListItemAction.ViewDetails -> {
                        Timber.d("Viewing contract details: $itemId")
                        onViewDetails(itemId)
                    }
                    ListItemAction.ContractAction.ViewDetails -> {
                        Timber.d("Viewing contract details (contract-specific): $itemId")
                        onViewDetails(itemId)
                    }
                    ListItemAction.ContractAction.Renew -> {
                        Timber.d("Renewing contract: $itemId")
                        onRenewContract(itemId)
                    }
                    else -> {
                        Timber.w("Unsupported action for contract: $action")
                    }
                }
            }

            override fun getDeleteDialogConfig(itemId: String): DeleteDialogConfig {
                val contract = contractsWithStats.find { it.contract.id == itemId }?.contract

                return DeleteDialogConfig(
                    titleResId = R.string.delete_dialog_contract_title,
                    messageResId = R.string.delete_dialog_contract_message,
                    itemNameProvider = {
                        contract?.name ?: "Contratto senza nome"
                    }
                )
            }

            override fun isActionAvailable(action: ListItemAction, itemId: String): Boolean {
                val contractWithStats = contractsWithStats.find { it.contract.id == itemId }
                val contract = contractWithStats?.contract
                val stats = contractWithStats?.stats

                return when (action) {
                    ListItemAction.ContractAction.Renew -> {
                        // Only show renew for expired or soon-to-expire contracts
                        val isExpired = stats?.isExpired == true
                        val expiresWithin30Days = contract?.let {
                            isContractExpiringSoon(it)
                        } == true

                        isExpired || expiresWithin30Days
                    }
                    ListItemAction.Edit -> {
                        // Allow editing for all contracts
                        true
                    }
                    ListItemAction.Delete -> {
                        // Allow deletion for all contracts
                        // Could add logic to prevent deletion of active contracts if needed
                        true
                    }
                    ListItemAction.ViewDetails,
                    ListItemAction.ContractAction.ViewDetails -> {
                        // Always allow viewing details
                        true
                    }
                    else -> false
                }
            }
        }
    }

    /**
     * Determines if a contract is expiring within the next 30 days
     */
    private fun isContractExpiringSoon(contract: Contract): Boolean {
        val now = kotlinx.datetime.Clock.System.now()
        val thirtyDaysFromNow = now.plus(30.days)
        val contractEndInstant = contract.endDate

        return contractEndInstant <= thirtyDaysFromNow
    }

    /**
     * Gets the appropriate action set based on contract state
     */
    fun getActionsForContract(contract: Contract, stats: ContractsStatistics): List<ListItemAction> {
        return when {
            stats.isExpired -> contractActionsForExpiredContracts
            isContractExpiringSoon(contract) -> contractActions // Include renew option
            else -> contractActionsForActiveContracts
        }
    }
}

/**
 * Composable helper for creating contract action handler with Remember
 */
@Composable
fun rememberContractActionHandler(
    contractsWithStats: List<ContractWithStats>,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onViewDetails: (String) -> Unit,
    onRenewContract: (String) -> Unit
): ListItemActionHandler {
    return remember(contractsWithStats) {
        ContractContextMenuConfig.createContractActionHandler(
            contractsWithStats = contractsWithStats,
            onEdit = onEdit,
            onDelete = onDelete,
            onViewDetails = onViewDetails,
            onRenewContract = onRenewContract
        )
    }
}

/**
 * Extension function to get available actions for a specific contract
 */
fun ContractWithStats.getAvailableActions(): List<ListItemAction> {
    return ContractContextMenuConfig.getActionsForContract(this.contract, this.stats)
}

/**
 * Example usage showing different approaches to configure the context menu
 */
@Composable
fun ExampleContractContextMenuUsage() {
    // Approach 1: Use predefined action sets
    val basicActions = CommonActionSets.contractActions

    // Approach 2: Use contract-specific configuration
    val contractActions = ContractContextMenuConfig.contractActions

    // Approach 3: Create custom action set
    val customActions = listOf(
        ListItemAction.ViewDetails,
        ListItemAction.Edit,
        ListItemAction.ContractAction.Renew,
        ListItemAction.Duplicate,
        ListItemAction.Delete
    )

    // Approach 4: Dynamic actions based on contract state
    val dynamicActionHandler = remember {
        object : ListItemActionHandler {
            override fun onActionSelected(action: ListItemAction, itemId: String) {
                // Handle actions
            }

            override fun getDeleteDialogConfig(itemId: String): DeleteDialogConfig? {
                // Return delete configuration
                return null
            }

            override fun isActionAvailable(action: ListItemAction, itemId: String): Boolean {
                // Dynamic availability based on contract state
                return true
            }
        }
    }
}

/**
 * Contract-specific utility functions for context menu integration
 */
object ContractMenuUtils {

    /**
     * Creates a contract action handler with default error handling
     */
    fun createRobustActionHandler(
        viewModel: ContractListViewModel,
        navigationCallbacks: ContractNavigationCallbacks
    ): ListItemActionHandler {
        return object : ListItemActionHandler {
            override fun onActionSelected(action: ListItemAction, itemId: String) {
                try {
                    when (action) {
                        ListItemAction.Edit -> navigationCallbacks.onEditContract(itemId)
                        ListItemAction.Delete -> viewModel.delete(itemId)
                        ListItemAction.ViewDetails -> navigationCallbacks.onViewDetails(itemId)
                        ListItemAction.ContractAction.Renew -> navigationCallbacks.onRenewContract(itemId)
                        else -> Timber.w("Unhandled action: $action")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error handling contract action: $action")
                    // Could show an error snackbar here
                }
            }

            override fun getDeleteDialogConfig(itemId: String): DeleteDialogConfig? {
                val contract = viewModel.getContract(itemId)
                return if (contract != null) {
                    DeleteDialogConfig(
                        titleResId = R.string.delete_dialog_contract_title,
                        messageResId = R.string.delete_dialog_contract_message,
                        itemNameProvider = { contract.name }
                    )
                } else null
            }

            override fun isActionAvailable(action: ListItemAction, itemId: String): Boolean {
                return when (action) {
                    ListItemAction.ContractAction.Renew -> {
                        viewModel.isContractEligibleForRenewal(itemId)
                    }
                    else -> true
                }
            }
        }
    }
}

/**
 * Navigation callbacks interface for contract actions
 */
interface ContractNavigationCallbacks {
    fun onEditContract(contractId: String)
    fun onViewDetails(contractId: String)
    fun onRenewContract(contractId: String)
}