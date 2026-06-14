package net.calvuz.qreport.settings.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R

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

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}