package net.calvuz.qreport.client.facility.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.PrimaryBadge
import net.calvuz.qreport.app.app.presentation.components.QReportConfirmDeleteDialog
import net.calvuz.qreport.app.app.presentation.components.QReportConfirmRestoreDialog
import net.calvuz.qreport.app.app.presentation.components.QrListStatItem
import net.calvuz.qreport.app.app.presentation.components.QrStatusChip
import net.calvuz.qreport.app.app.presentation.components.QrStatusIndicator
import net.calvuz.qreport.app.app.presentation.ui.theme.success
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianLastModified
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.presentation.model.icon
import net.calvuz.qreport.client.facility.presentation.ui.FacilityStatistics
import net.calvuz.qreport.settings.domain.model.ListViewMode

@Suppress("ParamsComparedByRef")
@Composable
fun FacilityCard(
    modifier: Modifier = Modifier,
    facility: Facility,
    stats: FacilityStatistics? = null,
    showActions: Boolean = true,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onOpenMaps: (() -> Unit)? = null,
    variant: ListViewMode,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

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
                onRestore = if (onRestore != null) { { showRestoreDialog = true } } else null,
                onEdit = onEdit,
                onOpenMaps = onOpenMaps
            )
            ListViewMode.COMPACT -> CompactFacilityCard(
                facility = facility,
                stats = stats,
                onOpenMaps = onOpenMaps,
                onRestore = if (onRestore != null) { { showRestoreDialog = true } } else null
            )
            ListViewMode.MINIMAL -> MinimalFacilityCard(facility = facility)
        }
    }

    if (showDeleteDialog && onDelete != null) {
        QReportConfirmDeleteDialog(
            objectName = stringResource(R.string.facility_card_action_delete),
            objectDesc = facility.displayName,
            onConfirm = { onDelete(); showDeleteDialog = false },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showRestoreDialog && onRestore != null) {
        QReportConfirmRestoreDialog(
            objectName = stringResource(R.string.facility_card_object_name),
            objectDesc = facility.displayName,
            onConfirm = { onRestore(); showRestoreDialog = false },
            onDismiss = { showRestoreDialog = false }
        )
    }
}

@Suppress("ParamsComparedByRef")
@Composable
private fun FullFacilityCard(
    facility: Facility,
    stats: FacilityStatistics?,
    showActions: Boolean,
    onDelete: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onOpenMaps: (() -> Unit)? = null
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // ── Header: tipo chip + azioni ────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SuggestionChip(
                onClick = {}, label = {
                    Icon(
                        imageVector = facility.facilityType.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(facility.facilityType.labelResId),
                        style = MaterialTheme.typography.labelMedium
                    )
                }, colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )

            if (showActions) {
                Row {
                    if (onOpenMaps != null && facility.address?.isComplete() == true) {
                        IconButton(onClick = onOpenMaps, modifier = Modifier.size(48.dp)) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = stringResource(R.string.facility_card_action_open_maps),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (onEdit != null) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.facility_card_action_edit),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.facility_card_action_delete),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Nome + primary badge ───────────────────────────────────────────────
        Text(
            text = facility.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (facility.isPrimary) {
            PrimaryBadge()
        }

        // Address row
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                Icon(
                    imageVector = Icons.AutoMirrored.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Statistics row
        if (stats != null) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()
            ) {
                QrListStatItem(
                    icon = Icons.Default.PrecisionManufacturing,
                    value = stats.islandsCount.toString(),
                    label = stringResource(R.string.facility_card_stat_islands)
                )
                QrListStatItem(
                    icon = Icons.Default.CheckCircle,
                    value = stats.activeIslandsCount.toString(),
                    label = stringResource(R.string.facility_card_stat_active),
                    color = if (stats.activeIslandsCount > 0) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.onSurfaceVariant
                )
                QrListStatItem(
                    icon = if (stats.maintenanceDueCount > 0) Icons.Default.Warning else Icons.Default.Build,
                    value = stats.maintenanceDueCount.toString(),
                    label = stringResource(R.string.facility_card_stat_maintenance),
                    color = if (stats.maintenanceDueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Status + timestamp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QrStatusChip(
                isActive = facility.isActive,
                onRestore = onRestore,
                activeString = UiText.StringResource(R.string.facility_card_status_active),
                inactiveString = UiText.StringResource(R.string.facility_card_status_inactive)
            )
            Text(
                text = facility.updatedAt.toItalianLastModified().asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
private fun CompactFacilityCard(
    facility: Facility,
    stats: FacilityStatistics?,
    onRestore: (() -> Unit)? = null,
    onOpenMaps: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icona tipo — identica a IslandCard COMPACT
        Icon(
            imageVector = facility.facilityType.icon(),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(facility.facilityType.labelResId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = facility.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (facility.isPrimary) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = stringResource(R.string.facility_card_primary_badge),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = facility.address?.city ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = if (onOpenMaps != null && facility.address?.isComplete() == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = if (onOpenMaps != null && facility.address?.isComplete() == true) Modifier.clickable { onOpenMaps() } else Modifier)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (stats != null) {
                QrListStatItem(
                    icon = Icons.Default.PrecisionManufacturing,
                    value = stats.islandsCount.toString(),
                    label = "",
                    compact = true
                )
                if (stats.maintenanceDueCount > 0) {
                    QrListStatItem(
                        icon = Icons.Default.Warning,
                        value = stats.maintenanceDueCount.toString(),
                        label = "",
                        color = MaterialTheme.colorScheme.error,
                        compact = true
                    )
                }
            }
            QrStatusIndicator(isActive = facility.isActive, onRestore = onRestore)
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
private fun MinimalFacilityCard(facility: Facility) {
    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        contentDescription = stringResource(R.string.facility_card_primary_badge),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        QrStatusIndicator(isActive = facility.isActive)
    }
}