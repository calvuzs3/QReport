package net.calvuz.qreport.client.client.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business

object ClientPkg {
    val title = "Clienti"
    val description = "Gestisci Clienti"
    val icon = Icons.Default.Business
    val selectedFilter: ClientFilter = ClientFilter.ACTIVE
    val selectedSortOrder: ClientSortOrder = ClientSortOrder.CREATED_RECENT
}
