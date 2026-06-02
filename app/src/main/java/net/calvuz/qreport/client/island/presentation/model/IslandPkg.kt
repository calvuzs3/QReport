package net.calvuz.qreport.client.island.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Workspaces
import net.calvuz.qreport.R

object IslandPkg {
    val titleResId = R.string.island_pkg_title
    val descResId = R.string.island_pkg_description
    val selectedFilter: IslandFilter = IslandFilter.ACTIVE
    val icon = Icons.Default.Workspaces
    val selectedSortOrder: IslandSortOrder = IslandSortOrder.CUSTOM_NAME
}