package net.calvuz.qreport.client.unit.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PrecisionManufacturing
import net.calvuz.qreport.R

object MechanicalUnitPkg {
    val titleResId: Int = R.string.unit_pkg_title
    val descriptionResId: Int = R.string.unit_pkg_description
    val icon = Icons.Default.PrecisionManufacturing
    val selectedFilter: MechanicalUnitFilter = MechanicalUnitFilter.ACTIVE
    val selectedSortOrder: MechanicalUnitSortOrder = MechanicalUnitSortOrder.NAME
}