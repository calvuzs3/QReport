package net.calvuz.qreport.presentation.feature.client.facility.components

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import net.calvuz.qreport.domain.model.client.Facility
import net.calvuz.qreport.domain.model.client.FacilityType
import net.calvuz.qreport.presentation.core.components.PrimaryBadge
import net.calvuz.qreport.presentation.feature.client.facility.FacilityStatistics

/**
 * FacilityCard riutilizzabile per QReport
 *
 * Mostra le informazioni principali dello stabilimento con statistiche
 * Supporta diverse modalità di visualizzazione e azioni
 */

@Composable
fun FacilityCard(
    modifier: Modifier = Modifier,
    facility: Facility,
    stats: FacilityStatistics? = null,
    onClick: () -> Unit,
    showActions: Boolean = true,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    variant: FacilityCardVariant = FacilityCardVariant.FULL
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        when (variant) {
            FacilityCardVariant.FULL -> FullFacilityCard(
                facility = facility,
                stats = stats,
                showActions = showActions,
                onDelete = if(onDelete != null){ {showDeleteDialog = true}} else null,
                onEdit = onEdit
            )
            FacilityCardVariant.COMPACT -> CompactFacilityCard(
                facility = facility,
                stats = stats
            )
            FacilityCardVariant.MINIMAL -> MinimalFacilityCard(facility = facility)
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && onDelete != null) {
        FacilityDeleteDialog(
            facilityName = facility.displayName,
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun FullFacilityCard(
    facility: Facility,
    stats: FacilityStatistics?,
    showActions: Boolean,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header row - Nome stabilimento e azioni
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        text = facility.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

//                    if (facility.isPrimary) {
//                        Spacer(modifier = Modifier.height(4.dp))
//                        Icon(
//                            Icons.Default.Star,
//                            contentDescription = "Contatto primario",
//                            tint = MaterialTheme.colorScheme.primary,
//                            modifier = Modifier.size(18.dp)
//                        )
//                    }
                }

                if (facility.isPrimary) {
                    Spacer(modifier = Modifier.height(4.dp))
                    PrimaryBadge()
                }
            }

            if (showActions) {
                Row {
                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Modifica stabilimento",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Elimina stabilimento",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Type and location info
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getFacilityTypeIcon(facility.facilityType),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = facility.facilityType.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Address
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = facility.addressDisplay,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Statistics row
        if (stats != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FacilityStatItem(
                    icon = Icons.Default.PrecisionManufacturing,
                    value = stats.islandsCount.toString(),
                    label = "Isole"
                )

                FacilityStatItem(
                    icon = Icons.Default.CheckCircle,
                    value = stats.activeIslandsCount.toString(),
                    label = "Attive",
                    color = if (stats.activeIslandsCount > 0)
                        MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (stats.maintenanceDueCount > 0) {
                    FacilityStatItem(
                        icon = Icons.Default.Warning,
                        value = stats.maintenanceDueCount.toString(),
                        label = "Manutenzione",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    FacilityStatItem(
                        icon = Icons.Default.Build,
                        value = "0",
                        label = "Manutenzioni"
                    )
                }
            }
        }

        // Status and last modified
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status chip a sinistra
            FacilityStatusChip(isActive = facility.isActive)

            // Timestamp a destra
            Text(
                text = formatLastModified(facility),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompactFacilityCard(
    facility: Facility,
    stats: FacilityStatistics?
) {
    Row(
        modifier = Modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = facility.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (facility.isPrimary) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Stabilimento primario",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "${facility.facilityType.displayName} • ${facility.address.city ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        if (stats != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FacilityStatItem(
                    icon = Icons.Default.PrecisionManufacturing,
                    value = stats.islandsCount.toString(),
                    label = "",
                    compact = true
                )
                if (stats.maintenanceDueCount > 0) {
                    FacilityStatItem(
                        icon = Icons.Default.Warning,
                        value = stats.maintenanceDueCount.toString(),
                        label = "",
                        color = MaterialTheme.colorScheme.error,
                        compact = true
                    )
                }
            }
        }

        FacilityStatusIndicator(isActive = facility.isActive)
    }
}

@Composable
private fun MinimalFacilityCard(facility: Facility) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = facility.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (facility.isPrimary) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Primario",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        FacilityStatusIndicator(isActive = facility.isActive)
    }
}

@Composable
private fun FacilityStatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(if (compact) 14.dp else 16.dp),
            tint = color
        )
        Text(
            text = value,
            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
        if (label.isNotEmpty() && !compact) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Composable
private fun FacilityStatusChip(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val (text, containerColor) = if (isActive) {
        "Attivo" to MaterialTheme.colorScheme.tertiaryContainer
    } else {
        "Inattivo" to MaterialTheme.colorScheme.outline
    }

    AssistChip(
        onClick = { },
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor
        ),
        modifier = modifier
    )
}

@Composable
private fun FacilityStatusIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isActive) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Icon(
        imageVector = if (isActive) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
        contentDescription = if (isActive) "Attivo" else "Inattivo",
        tint = color,
        modifier = modifier.size(16.dp)
    )
}

@Composable
private fun FacilityDeleteDialog(
    facilityName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elimina Stabilimento") },
        text = {
            Text("Sei sicuro di voler eliminare lo stabilimento '$facilityName'? Questa operazione eliminerà anche tutte le isole associate e non può essere annullata.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Elimina")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

private fun formatLastModified(facility: Facility): String {
    val now = Clock.System.now()
    val updated = facility.updatedAt
    val diffMillis = (now - updated).inWholeMilliseconds

    return when {
        diffMillis < 60000 -> "Aggiornato ora"
        diffMillis < 3600000 -> "Aggiornato ${diffMillis / 60000} min fa"
        diffMillis < 86400000 -> "Aggiornato ${diffMillis / 3600000}h fa"
        else -> "Aggiornato ${diffMillis / 86400000} giorni fa"
    }
}

private fun getFacilityTypeIcon(facilityType: FacilityType): ImageVector {
    return when (facilityType) {
        FacilityType.PRODUCTION -> Icons.Default.Factory
        FacilityType.WAREHOUSE -> Icons.Default.Warehouse
        FacilityType.ASSEMBLY -> Icons.Default.Build
        FacilityType.TESTING -> Icons.Default.Science
        FacilityType.LOGISTICS -> Icons.Default.LocalShipping
        FacilityType.OFFICE -> Icons.Default.Business
        FacilityType.MAINTENANCE -> Icons.Default.Build
        FacilityType.R_AND_D -> Icons.Default.Biotech
        FacilityType.OTHER -> Icons.Default.Business
    }
}

/**
 * Varianti di visualizzazione per FacilityCard
 */
enum class FacilityCardVariant {
    FULL,       // Card completa con tutte le informazioni
    COMPACT,    // Card compatta per liste dense
    MINIMAL     // Card minimalista per selezioni
}