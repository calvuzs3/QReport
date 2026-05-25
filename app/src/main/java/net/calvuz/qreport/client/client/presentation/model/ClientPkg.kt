package net.calvuz.qreport.client.client.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Group

object ClientPkg {
    val title = "Clienti"
    val description = "Gestisci Clienti"
    val icon = Icons.Default.Group
    val icon_unselected = Icons.Outlined.Group
    val selectedFilter: ClientFilter = ClientFilter.ACTIVE
    val selectedSortOrder: ClientSortOrder = ClientSortOrder.CREATED_RECENT
}
