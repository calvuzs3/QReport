package net.calvuz.qreport.checkup.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText

object CheckupPkg {
    //val title = UiText.StringResource(R.string.route_checkups_title)
    val title = "Check-up"
    val description = UiText.StringResource(R.string.route_checkups_subtitle)
    val icon = Icons.AutoMirrored.Filled.Assignment
    val icon_unselected = Icons.AutoMirrored.Outlined.Assignment
    val selectedFilter = null
    val selectedSortOrder = null
}