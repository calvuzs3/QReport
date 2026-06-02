package net.calvuz.qreport.client.facility.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Factory
import net.calvuz.qreport.R

object FacilityPkg {
    val titleResId: Int = R.string.client_pkg_title
    val descriptionResId: Int = R.string.client_pkg_description
    val icon = Icons.Default.Factory
    val selectedFilter: FacilityFilter = FacilityFilter.ACTIVE
    val selectedSortOrder: FacilitySortOrder = FacilitySortOrder.CREATED_RECENT
}