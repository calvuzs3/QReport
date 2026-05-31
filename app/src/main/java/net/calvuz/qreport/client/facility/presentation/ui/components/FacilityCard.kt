package net.calvuz.qreport.client.facility.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.PrimaryBadge
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianLastModified
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.model.FacilityType
import net.calvuz.qreport.client.facility.presentation.ui.FacilityStatistics
import net.calvuz.qreport.settings.domain.model.ListViewMode

@Composable
fun FacilityCard(
    modifier: Modifier = Modifier,
    facility: Facility,
    stats: FacilityStatistics? = null,
    showActions: Boolean = true,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onOpenMaps: (() -> Unit)? = null,
    variant: ListViewMode,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        when (variant) {
            ListViewMode.FULL -> FullFacilityCard(
                facility = facility,
                stats = stats,
                showActions = showActions,
                onDelete = if (onDelete != null) { { showDeleteDialog = true } } else null,
                onEdit = onEdit,
                onOpenMaps = onOpenMaps
            )
            ListViewMode.COMPACT -> CompactFacilityCard(facility = facility, stats = stats, onOpenMaps = onOpenMaps)
            ListViewMode.MINIMAL -> MinimalFacilityCard(facility = facility)
        }
    }

    if (showDeleteDialog && onDelete != null) {
        FacilityDeleteDialog(
            facilityName = facility.displayName,
            onConfirm = { onDelete(); showDeleteDialog = false },
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
    onEdit: (() -> Unit)? = null,
    onOpenMaps: (() -> Unit)? = null
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(facility.facilityType.labelResId),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    Text(
                        text = facility.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (facility.isPrimary) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Icon(Icons.Default.Star, contentDescription = stringResource(R.string.facility_card_primary_badge), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
                if (facility.isPrimary) {
                    Spacer(modifier = Modifier.height(4.dp))
                    PrimaryBadge()
                }
            }

            if (showActions) {
                Row {
                    if (onOpenMaps != null && facility.address?.isComplete() == true) {
                        IconButton(onClick = onOpenMaps, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = stringResource(R.string.facility_card_action_open_maps), modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (onEdit != null) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.facility_card_action_edit), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.facility_card_action_delete), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // Type row
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = getFacilityTypeIcon(facility.facilityType), contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = stringResource(facility.facilityType.labelResId), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // Address row
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            val mapsClickable = onOpenMaps != null && facility.address?.isComplete() == true
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = if (mapsClickable) stringResource(R.string.facility_card_action_open_maps) else null,
                modifier = Modifier.size(16.dp),
                tint = if (mapsClickable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = facility.addressDisplay ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = if (mapsClickable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (mapsClickable) {
                Icon(imageVector = Icons.AutoMirrored.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Statistics row
        if (stats != null) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                FacilityStatItem(
                    icon = Icons.Default.PrecisionManufacturing,
                    value = stats.islandsCount.toString(),
                    label = stringResource(R.string.facility_card_stat_islands)
                )
                FacilityStatItem(
                    icon = Icons.Default.CheckCircle,
                    value = stats.activeIslandsCount.toString(),
                    label = stringResource(R.string.facility_card_stat_active),
                    color = if (stats.activeIslandsCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                FacilityStatItem(
                    icon = if (stats.maintenanceDueCount > 0) Icons.Default.Warning else Icons.Default.Build,
                    value = stats.maintenanceDueCount.toString(),
                    label = stringResource(R.string.facility_card_stat_maintenance),
                    color = if (stats.maintenanceDueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Status + timestamp
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            FacilityStatusChip(isActive = facility.isActive)
            Text(text = facility.updatedAt.toItalianLastModified(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CompactFacilityCard(
    facility: Facility,
    stats: FacilityStatistics?,
    onOpenMaps: (() -> Unit)? = null
) {
    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(facility.facilityType.labelResId), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = facility.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (facility.isPrimary) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(imageVector = Icons.Default.Star, contentDescription = stringResource(R.string.facility_card_primary_badge), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                text = facility.address?.city ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = if (onOpenMaps != null && facility.address?.isComplete() == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = if (onOpenMaps != null && facility.address?.isComplete() == true) Modifier.clickable { onOpenMaps() } else Modifier
            )
        }
        if (stats != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FacilityStatItem(icon = Icons.Default.PrecisionManufacturing, value = stats.islandsCount.toString(), label = "", compact = true)
                if (stats.maintenanceDueCount > 0) {
                    FacilityStatItem(icon = Icons.Default.Warning, value = stats.maintenanceDueCount.toString(), label = "", color = MaterialTheme.colorScheme.error, compact = true)
                }
            }
        }
        FacilityStatusIndicator(isActive = facility.isActive)
    }
}

@Composable
private fun MinimalFacilityCard(facility: Facility) {
    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = facility.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (facility.isPrimary) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = stringResource(R.string.facility_card_primary_badge), modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        FacilityStatusIndicator(isActive = facility.isActive)
    }
}

// =============================================================================
// SHARED COMPOSABLE
// =============================================================================

@Composable
private fun FacilityStatItem(icon: ImageVector, value: String, label: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant, modifier: Modifier = Modifier, compact: Boolean = false) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(if (compact) 14.dp else 16.dp), tint = color)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.Medium)
        if (label.isNotEmpty() && !compact) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}

@Composable
private fun FacilityStatusChip(isActive: Boolean, modifier: Modifier = Modifier) {
    val (text, containerColor) = if (isActive) {
        stringResource(R.string.facility_card_status_active) to MaterialTheme.colorScheme.tertiaryContainer
    } else {
        stringResource(R.string.facility_card_status_inactive) to MaterialTheme.colorScheme.outline
    }
    AssistChip(onClick = { }, label = { Text(text) }, colors = AssistChipDefaults.assistChipColors(containerColor = containerColor), modifier = modifier)
}

@Composable
private fun FacilityStatusIndicator(isActive: Boolean, modifier: Modifier = Modifier) {
    Icon(
        imageVector = if (isActive) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
        contentDescription = if (isActive) stringResource(R.string.facility_card_status_active) else stringResource(R.string.facility_card_status_inactive),
        tint = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
        modifier = modifier.size(16.dp)
    )
}

@Composable
private fun FacilityDeleteDialog(facilityName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.facility_card_delete_dialog_title)) },
        text = { Text(stringResource(R.string.facility_card_delete_dialog_message, facilityName)) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.facility_card_delete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.facility_card_delete_dialog_cancel)) }
        }
    )
}

// =============================================================================
// HELPERS
// =============================================================================

private fun getFacilityTypeIcon(facilityType: FacilityType): ImageVector = when (facilityType) {
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