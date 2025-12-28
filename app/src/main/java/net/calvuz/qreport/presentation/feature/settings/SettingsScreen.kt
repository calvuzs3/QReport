package net.calvuz.qreport.presentation.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToBackup: () -> Unit = {},
    onNavigateToTechnicianSettings: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Impostazioni") },
        )


        // Technician info
        SettingsSection(title = "Profilo Utente") {
            SettingsItem(
                title = "Informazioni Tecnico",
                subtitle = "Dati per pre-compilazione CheckUp",
                icon = Icons.Default.Engineering,
                onClick = onNavigateToTechnicianSettings  // ✅ Connected to navigation
            )
        }

        // Backup
        SettingsSection(title = "Dati") {
            SettingsItem(
                title = "Backup Database",
                subtitle = "Backup e ripristino dati QReport",
                icon = Icons.Default.Backup,
                onClick = onNavigateToBackup  // ✅ Connected to navigation
            )
//                SettingsItem(
//                    title = "Pulizia Cache",
//                    subtitle = "Libera spazio foto temporanee",
//                    icon = Icons.Default.CleaningServices,
//                    onClick = { }
//                )
        }

//            SettingsSection(title = "Generale") {
//                SettingsItem(
//                    title = "Template Check-up",
//                    subtitle = "Personalizza checklist",
//                    icon = Icons.AutoMirrored.Default.Assignment,
//                    onClick = { }
//                )
//            }

//            SettingsSection(title = "App") {
//                SettingsItem(
//                    title = "Informazioni",
//                    subtitle = "QReport v1.0 - Calvuz",
//                    icon = Icons.Default.Info,
//                    onClick = { }
//                )
//                SettingsItem(
//                    title = "Supporto",
//                    subtitle = "Contatta il supporto tecnico",
//                    icon = Icons.Default.Support,
//                    onClick = { }
//                )
//            }
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