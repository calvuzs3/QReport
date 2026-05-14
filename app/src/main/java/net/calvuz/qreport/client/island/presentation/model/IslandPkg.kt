package net.calvuz.qreport.client.island.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Workspaces

object IslandPkg {
    val title = "Isole"
    val description = "Gestisci Isole"
    val selectedFilter: IslandFilter = IslandFilter.ACTIVE
    val icon = Icons.Default.Workspaces
    val selectedSortOrder: IslandSortOrder = IslandSortOrder.CUSTOM_NAME
}