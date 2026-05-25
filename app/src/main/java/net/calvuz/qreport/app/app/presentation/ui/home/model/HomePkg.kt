package net.calvuz.qreport.app.app.presentation.ui.home.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText

object HomePkg {
    val title = UiText.StringResource(R.string.route_home_title)
    val description = UiText.StringResource(R.string.route_home_subtitle)
    val icon = Icons.Filled.Home
    val icon_unselected = Icons.Outlined.Home
    val selectedFilter = null
    val selectedSortOrder = null
}