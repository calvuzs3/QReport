
// ===============================
// Settings Screen
// ===============================

package net.calvuz.qreport.presentation.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Impostazioni",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            SettingsSection(title = "Generale") {
                SettingsItem(
                    title = "Profilo Tecnico",
                    subtitle = "Gestisci informazioni personali",
                    icon = Icons.Default.Person,
                    onClick = { }
                )
                SettingsItem(
                    title = "Template Check-up",
                    subtitle = "Personalizza checklist",
                    icon = Icons.Default.Assignment,
                    onClick = { }
                )
            }
        }

        item {
            SettingsSection(title = "Dati") {
                SettingsItem(
                    title = "Backup Locale",
                    subtitle = "Esporta tutti i dati",
                    icon = Icons.Default.Backup,
                    onClick = { }
                )
                SettingsItem(
                    title = "Pulizia Cache",
                    subtitle = "Libera spazio foto temporanee",
                    icon = Icons.Default.CleaningServices,
                    onClick = { }
                )
            }
        }

        item {
            SettingsSection(title = "App") {
                SettingsItem(
                    title = "Informazioni",
                    subtitle = "QReport v1.0 - Calvuz",
                    icon = Icons.Default.Info,
                    onClick = { }
                )
                SettingsItem(
                    title = "Supporto",
                    subtitle = "Contatta il supporto tecnico",
                    icon = Icons.Default.Support,
                    onClick = { }
                )
            }
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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