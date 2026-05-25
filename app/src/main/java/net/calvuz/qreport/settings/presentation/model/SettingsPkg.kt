package net.calvuz.qreport.settings.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText

object SettingsPkg {
    val title = UiText.StringResource(R.string.route_settings_title)
    val description = UiText.StringResource(R.string.route_settings_subtitle)
    val icon = Icons.Filled.Settings
    val icon_unselected = Icons.Outlined.Settings
    val selectedFilter = null
    val selectedSortOrder = null
}