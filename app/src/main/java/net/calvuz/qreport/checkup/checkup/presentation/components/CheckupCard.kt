@file:Suppress("HardCodedStringLiteral", "ASSIGNED_VALUE_IS_NEVER_READ", "unused")
package net.calvuz.qreport.checkup.checkup.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUp
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUpSingleStatistics
import net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster
import net.calvuz.qreport.app.app.presentation.components.QReportConfirmDeleteDialog
import net.calvuz.qreport.app.app.presentation.components.QrListStatItem
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDateTime
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianLastModified
import net.calvuz.qreport.app.util.NumberUtils.toItalianPercentage
import net.calvuz.qreport.settings.domain.model.ListViewMode


@Suppress("ParamsComparedByRef")
@Composable
fun CheckupCard(
    modifier: Modifier = Modifier,
    checkup: CheckUp,
    stats: CheckUpSingleStatistics? = null,
    statusMaster: CheckUpStatusMaster? = null,
    onClick: () -> Unit,
    showActions: Boolean = true,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    variant: ListViewMode = ListViewMode.FULL
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        when (variant) {
            ListViewMode.FULL -> FullCheckupCard(
                checkup = checkup,
                stats = stats,
                statusMaster = statusMaster,
                showActions = showActions,
                onDelete = if (onDelete != null) ({ showDeleteDialog = true }) else null,
                onEdit = onEdit
            )
            ListViewMode.COMPACT -> CompactCheckupCard(
                checkup = checkup,
                stats = stats,
                statusMaster = statusMaster
            )
            ListViewMode.MINIMAL -> MinimalCheckupCard(
                checkup = checkup,
                statusMaster = statusMaster
            )
        }
    }

    if (showDeleteDialog && onDelete != null) {
        QReportConfirmDeleteDialog(
            objectName = stringResource(R.string.checkup_component_card_delete_object_name),
            objectDesc = checkup.header.checkUpDate.toItalianDateTime(),
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}


@Suppress("ParamsComparedByRef")
@Composable
private fun FullCheckupCard(
    checkup: CheckUp,
    stats: CheckUpSingleStatistics?,
    statusMaster: CheckUpStatusMaster?,
    showActions: Boolean,
    onDelete: (() -> Unit)?,
    onEdit: (() -> Unit)?
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: date + company / edit button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = checkup.header.checkUpDate.toItalianDateTime(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = checkup.header.clientInfo.companyName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (showActions && onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.checkup_component_card_action_edit),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Island row
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PrecisionManufacturing,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.checkup_component_card_island_prefix,
                    checkup.header.islandInfo.serialNumber
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (checkup.header.clientInfo.site.isNotBlank()) {
                Text(
                    text = "• ${checkup.header.clientInfo.site}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Progress bar + stats
        if (stats != null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.checkup_component_card_progress),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stats.completionPercentage.toItalianPercentage(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val progress =
                    if (stats.completionPercentage > 1) stats.completionPercentage / 100
                    else stats.completionPercentage
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    strokeCap = StrokeCap.Round
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    QrListStatItem(
                        icon = Icons.Default.CheckCircle,
                        value = "${stats.okItems}/${stats.totalItems}",
                        label = stringResource(R.string.checkup_component_card_stat_ok)
                    )
                    if (stats.nokItems > 0) {
                        QrListStatItem(
                            icon = Icons.Default.Error,
                            value = stats.nokItems.toString(),
                            label = stringResource(R.string.checkup_component_card_stat_nok),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (stats.photosCount > 0) {
                        QrListStatItem(
                            icon = Icons.Default.PhotoCamera,
                            value = stats.photosCount.toString(),
                            label = stringResource(R.string.checkup_component_card_stat_photos)
                        )
                    }
                }
            }
        }

        // Footer: status chip + last modified
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckupStatusChip(statusMaster = statusMaster)
            Text(
                text = checkup.updatedAt.toItalianLastModified().asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Suppress("ParamsComparedByRef")
@Composable
private fun CompactCheckupCard(
    checkup: CheckUp,
    stats: CheckUpSingleStatistics?,
    statusMaster: CheckUpStatusMaster?
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: date + company + island
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = checkup.header.checkUpDate.toItalianDateTime(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(checkup.header.clientInfo.companyName)
                    val sn = checkup.header.islandInfo.serialNumber
                    if (sn.isNotBlank()) append(" · $sn")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right: stats + status chip
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (stats != null) {
                QrListStatItem(
                    icon = Icons.Default.CheckCircle,
                    value = "${stats.okItems}/${stats.totalItems}",
                    label = "",
                    compact = true
                )
                if (stats.nokItems > 0) {
                    QrListStatItem(
                        icon = Icons.Default.Error,
                        value = stats.nokItems.toString(),
                        label = "",
                        color = MaterialTheme.colorScheme.error,
                        compact = true
                    )
                }
            }
            CheckupStatusDot(statusMaster = statusMaster)
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
private fun MinimalCheckupCard(
    checkup: CheckUp,
    statusMaster: CheckUpStatusMaster?
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = checkup.header.checkUpDate.toItalianDateTime(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = checkup.header.clientInfo.companyName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        CheckupStatusDot(statusMaster = statusMaster)
    }
}
