package net.calvuz.qreport.client.facility.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Factory

object FacilityPkg {
    val title = "Stabilimenti"
    val description = "Gestisci Stabilimenti"
    val icon = Icons.Default.Factory
    val selectedFilter: FacilityFilter = FacilityFilter.ACTIVE
    val selectedSortOrder: FacilitySortOrder = FacilitySortOrder.CREATED_RECENT
}