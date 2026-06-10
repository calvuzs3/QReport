package net.calvuz.qreport.client.contract.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import net.calvuz.qreport.R

object ContractPkg {
    val titleResId: Int = R.string.contract_pkg_title
    val descriptionResId: Int = R.string.contract_pkg_description
    val icon = Icons.Default.AssignmentTurnedIn
    val iconUnselected = Icons.Outlined.AssignmentTurnedIn
    val selectedFilter: ContractFilter = ContractFilter.ACTIVE
    val selectedSortOrder: ContractSortOrder = ContractSortOrder.EXPIRE_RECENT
}