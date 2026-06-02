package net.calvuz.qreport.client.client.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Group
import net.calvuz.qreport.R

object ClientPkg {
    val titleResId: Int = R.string.client_pkg_title
    val descriptionResId: Int = R.string.client_pkg_description
    val icon = Icons.Default.Group
    val iconUnselected = Icons.Outlined.Group
    val selectedFilter: ClientFilter = ClientFilter.ACTIVE
    val selectedSortOrder: ClientSortOrder = ClientSortOrder.CREATED_RECENT
}