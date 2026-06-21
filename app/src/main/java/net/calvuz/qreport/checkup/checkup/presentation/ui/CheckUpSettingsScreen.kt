package net.calvuz.qreport.checkup.checkup.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.SettingsItem
import net.calvuz.qreport.app.app.presentation.components.SettingsSection

/**
 * Settings hub for the Checkup feature's normalized master data — reachable from
 * [net.calvuz.qreport.checkup.checkup.presentation.CheckUpListScreen], mirrors
 * [net.calvuz.qreport.settings.presentation.ui.SettingsScreen]'s former "master
 * data" section (moved here, out of general Settings).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckUpSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToIslandTypes: () -> Unit,
    onNavigateToModuleTypes: () -> Unit,
    onNavigateToCriticalityLevels: () -> Unit,
    onNavigateToCheckItemTemplates: () -> Unit,
    onNavigateToModuleIslandAssociation: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.checkup_settings_screen_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_close))
                }
            }
        )

        SettingsSection(title = stringResource(R.string.settings_screen_main_section_master_data)) {
            SettingsItem(
                title = stringResource(R.string.settings_screen_main_island_types_title),
                subtitle = stringResource(R.string.settings_screen_main_island_types_subtitle),
                icon = Icons.Default.Category,
                onClick = onNavigateToIslandTypes
            )
            SettingsItem(
                title = stringResource(R.string.settings_screen_main_module_types_title),
                subtitle = stringResource(R.string.settings_screen_main_module_types_subtitle),
                icon = Icons.Default.ViewModule,
                onClick = onNavigateToModuleTypes
            )
            SettingsItem(
                title = stringResource(R.string.settings_screen_main_criticality_levels_title),
                subtitle = stringResource(R.string.settings_screen_main_criticality_levels_subtitle),
                icon = Icons.Default.PriorityHigh,
                onClick = onNavigateToCriticalityLevels
            )
            SettingsItem(
                title = stringResource(R.string.settings_screen_main_check_item_templates_title),
                subtitle = stringResource(R.string.settings_screen_main_check_item_templates_subtitle),
                icon = Icons.AutoMirrored.Filled.ListAlt,
                onClick = onNavigateToCheckItemTemplates
            )
            SettingsItem(
                title = stringResource(R.string.checkup_settings_module_island_association_title),
                subtitle = stringResource(R.string.checkup_settings_module_island_association_subtitle),
                icon = Icons.AutoMirrored.Filled.CompareArrows,
                onClick = onNavigateToModuleIslandAssociation
            )
        }
    }
}
