package net.calvuz.qreport.client.client.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import net.calvuz.qreport.client.client.presentation.ui.ClientFilter
import net.calvuz.qreport.client.client.presentation.ui.ClientSortOrder

object ClientPkg {
    val title = "Clienti"
    val description = "Gestisci Clienti"
    val icon = Icons.Default.Business
    val selectedFilter: ClientFilter = ClientFilter.ACTIVE
    val selectedSortOrder: ClientSortOrder = ClientSortOrder.CREATED_RECENT
}
