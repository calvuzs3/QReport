package net.calvuz.qreport.client.unit.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PrecisionManufacturing

object UnitPkg {
    const val title = "Unità meccanica"
    const val description = "Gestisci Unità Meccaniche"
    val icon = Icons.Default.PrecisionManufacturing
    val selectedFilter: UnitFilter = UnitFilter.ACTIVE
    val selectedSortOrder: UnitSortOrder = UnitSortOrder.NAME
}