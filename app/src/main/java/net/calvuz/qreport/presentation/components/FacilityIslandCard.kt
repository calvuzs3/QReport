package net.calvuz.qreport.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.model.client.OperationalStatus
import net.calvuz.qreport.domain.model.island.IslandType
import net.calvuz.qreport.util.DateTimeUtils.toItalianDate

/**
 * Card per visualizzazione isola robotizzata
 *
 * Features:
 * - Indicatori stato operativo (colori e icone)
 * - Info manutenzione con countdown/warning
 * - Ore operative e cicli
 * - Tipo isola con icona specifica
 * - Serial number e nome custom
 * - Azioni rapide (dettaglio, eliminazione)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilityIslandCard(
    island: FacilityIsland,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    variant: IslandCardVariant = IslandCardVariant.FULL,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (island.operationalStatus) {
                OperationalStatus.MAINTENANCE_DUE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                OperationalStatus.INACTIVE -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header con tipo isola e stato
            IslandHeader(
                island = island,
                onDelete = if (onDelete != null) { { showDeleteDialog = true } } else null
            )

            // Informazioni principali
            IslandMainInfo(island = island, variant = variant)

            // Stato manutenzione
            if (variant == IslandCardVariant.FULL || island.needsMaintenance()) {
                IslandMaintenanceStatus(island = island)
            }

            // Statistiche operative (solo nella versione completa)
            if (variant == IslandCardVariant.FULL) {
                IslandOperationalStats(island = island)
            }
        }
    }

    // Dialog di conferma eliminazione
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Elimina Isola") },
            text = {
                Text("Sei sicuro di voler eliminare l'isola '${island.displayName}'? Questa azione non può essere annullata.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete?.invoke()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
private fun IslandHeader(
    island: FacilityIsland,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icona tipo isola
            IslandTypeIcon(
                islandType = island.islandType,
                modifier = Modifier.size(24.dp)
            )

            // Nome display e serial number
            Column {
                Text(
                    text = island.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (island.customName != null) {
                    Text(
                        text = island.serialNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Indicatore stato operativo
            OperationalStatusBadge(status = island.operationalStatus)

            // Menu azioni
            if (onDelete != null) {
                var showMenu by remember { mutableStateOf(false) }

                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Elimina") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IslandMainInfo(
    island: FacilityIsland,
    variant: IslandCardVariant
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Modello e tipo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            island.model?.let { model ->
                Text(
                    text = "Modello: $model",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (variant == IslandCardVariant.FULL) {
                Text(
                    text = island.islandType.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Ubicazione (se disponibile)
        island.location?.let { location ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun IslandMaintenanceStatus(island: FacilityIsland) {
    val maintenanceText = island.maintenanceStatusText
    val isDue = island.needsMaintenance()
    val daysToMaintenance = island.daysToNextMaintenance()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDue -> MaterialTheme.colorScheme.errorContainer
                daysToMaintenance != null && daysToMaintenance <= 7 -> MaterialTheme.colorScheme.warningContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = when {
                    isDue -> Icons.Default.Error
                    daysToMaintenance != null && daysToMaintenance <= 7 -> Icons.Default.Warning
                    else -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = when {
                    isDue -> MaterialTheme.colorScheme.onErrorContainer
                    daysToMaintenance != null && daysToMaintenance <= 7 -> MaterialTheme.colorScheme.onWarningContainer
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }
            )

            Text(
                text = maintenanceText,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isDue -> MaterialTheme.colorScheme.onErrorContainer
                    daysToMaintenance != null && daysToMaintenance <= 7 -> MaterialTheme.colorScheme.onWarningContainer
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun IslandOperationalStats(island: FacilityIsland) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Ore operative
            StatItem(
                icon = Icons.Default.Schedule,
                label = "Ore",
                value = "${island.operatingHours}h"
            )

            // Conteggio cicli
            StatItem(
                icon = Icons.Default.Repeat,
                label = "Cicli",
                value = formatCycleCount(island.cycleCount)
            )

            // Data installazione
            island.installationDate?.let { installDate ->
                StatItem(
                    icon = Icons.Default.CalendarToday,
                    label = "Installata",
                    value = installDate.toItalianDate()
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun OperationalStatusBadge(status: OperationalStatus) {
    val (color, icon) = when (status) {
        OperationalStatus.OPERATIONAL -> Color(0xFF00B050) to Icons.Default.CheckCircle
        OperationalStatus.MAINTENANCE_DUE -> Color(0xFFFFC000) to Icons.Default.Warning
        OperationalStatus.INACTIVE -> Color(0xFFFF0000) to Icons.Default.Error
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )

            Text(
                text = status.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun IslandTypeIcon(
    islandType: IslandType,
    modifier: Modifier = Modifier
) {
    val icon = when (islandType) {
        IslandType.POLY_MOVE -> Icons.Default.OpenWith
        IslandType.POLY_CAST -> Icons.Default.Opacity
        IslandType.POLY_EBT -> Icons.Default.ElectricalServices
        IslandType.POLY_TAG_BLE -> Icons.Default.Bluetooth
        IslandType.POLY_TAG_FC -> Icons.Default.CenterFocusWeak
        IslandType.POLY_TAG_V -> Icons.Default.Visibility
        IslandType.POLY_SAMPLE -> Icons.Default.Science
    }

    Icon(
        imageVector = icon,
        contentDescription = islandType.displayName,
        modifier = modifier,
        tint = MaterialTheme.colorScheme.primary
    )
}

// Helper per formattare il conteggio cicli in modo leggibile
private fun formatCycleCount(cycleCount: Long): String {
    return when {
        cycleCount >= 1_000_000 -> "${(cycleCount / 1_000_000).toInt()}M"
        cycleCount >= 1_000 -> "${(cycleCount / 1_000).toInt()}K"
        else -> cycleCount.toString()
    }
}

/**
 * Varianti di visualizzazione card
 */
enum class IslandCardVariant {
    COMPACT,    // Solo info essenziali
    FULL        // Tutte le informazioni
}

// Extension per Color custom warning
@get:ReadOnlyComposable
private val ColorScheme.warningContainer: Color
    get() = Color(0xFFFFF3C0)

@get:ReadOnlyComposable
private val ColorScheme.onWarningContainer: Color
    get() = Color(0xFF8B5A00)