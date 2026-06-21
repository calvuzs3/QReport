package net.calvuz.qreport.settings.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.SettingsItem
import net.calvuz.qreport.app.app.presentation.components.SettingsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToBackup: () -> Unit,
    onNavigateToTechnicianSettings: () -> Unit,
    onNavigateToSyncSettings: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_screen_main_title)) },
        )

        // Technician info
        SettingsSection(title = stringResource(R.string.settings_screen_main_section_profile)) {
            SettingsItem(
                title = stringResource(R.string.settings_screen_main_technician_title),
                subtitle = stringResource(R.string.settings_screen_main_technician_subtitle),
                icon = Icons.Default.Engineering,
                onClick = onNavigateToTechnicianSettings
            )
        }

        // Data
        SettingsSection(title = stringResource(R.string.settings_screen_main_section_data)) {
            SettingsItem(
                title = stringResource(R.string.settings_screen_main_backup_title),
                subtitle = stringResource(R.string.settings_screen_main_backup_subtitle),
                icon = Icons.Default.Backup,
                onClick = onNavigateToBackup
            )
        }

        // Sync
        SettingsSection(title = stringResource(R.string.settings_screen_main_section_sync)) {
            SettingsItem(
                title = stringResource(R.string.settings_screen_main_sync_title),
                subtitle = stringResource(R.string.settings_screen_main_sync_subtitle),
                icon = Icons.Default.CloudSync,
                onClick = onNavigateToSyncSettings
            )
        }
    }
}