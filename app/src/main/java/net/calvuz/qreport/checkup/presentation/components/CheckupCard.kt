package net.calvuz.qreport.checkup.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Business
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
import net.calvuz.qreport.checkup.domain.model.CheckUp
import net.calvuz.qreport.checkup.domain.model.CheckUpSingleStatistics
import net.calvuz.qreport.checkup.domain.model.CheckUpStatus
import net.calvuz.qreport.app.app.presentation.components.ConfirmDeleteDialog
import net.calvuz.qreport.app.app.presentation.components.ListStatItem
import net.calvuz.qreport.app.app.presentation.components.StatusIndicator
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDateTime
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianLastModified
import net.calvuz.qreport.app.util.NumberUtils.toItalianPercentage
import net.calvuz.qreport.settings.domain.model.ListViewMode


@Composable
fun CheckupCard(
    modifier: Modifier = Modifier,

    checkup: CheckUp,
    stats: CheckUpSingleStatistics? = null,
    onClick: () -> Unit,
    showActions: Boolean = true,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    variant: ListViewMode = ListViewMode.FULL
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Content
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
                showActions = showActions,
                onDelete = { showDeleteDialog = false },
                onEdit = onEdit
            )

            ListViewMode.COMPACT -> CompactCheckupCard(
                checkup = checkup,
                stats = stats
            )

            ListViewMode.MINIMAL -> MinimalCheckupCard(
                checkup = checkup
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && onDelete != null) {
        ConfirmDeleteDialog(
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


@Composable
private fun FullCheckupCard(
    checkup: CheckUp,
    stats: CheckUpSingleStatistics?,
    showActions: Boolean,
    onDelete: (() -> Unit)?,
    onEdit: (() -> Unit)?
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
        ) {
            //
            Column (
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ){
                // Name (date)
                Text(
                    text = checkup.header.checkUpDate.toItalianDateTime(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
//                // Header row
//                Row(
//                    modifier = Modifier.weight(1f),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
                    //
                    Text(
                        text = checkup.header.clientInfo.companyName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
//                }
            }

            if (showActions) {
                Column(
                ) {
                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(24.dp)

                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.checkup_component_card_action_edit),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Island info
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PrecisionManufacturing,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.checkup_component_card_island_prefix,
                    checkup.header.islandInfo.serialNumber
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (checkup.header.clientInfo.site.isNotBlank()) {
                Text(
                    text = "• ${checkup.header.clientInfo.site}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Progress bar
        if (stats != null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    if (stats.completionPercentage > 1) stats.completionPercentage / 100 else stats.completionPercentage
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSecondary,
                    trackColor = MaterialTheme.colorScheme.secondary,
                    strokeCap = StrokeCap.Butt
                )
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                ListStatItem(
                    icon = Icons.Default.CheckCircle,
                    value = "${stats.okItems}/${stats.totalItems}",
                    label = stringResource(R.string.checkup_component_card_stat_ok)
                )

                if (stats.nokItems > 0) {
                    ListStatItem(
                        icon = Icons.Default.Error,
                        value = stats.nokItems.toString(),
                        label = stringResource(R.string.checkup_component_card_stat_nok),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (stats.photosCount > 0) {
                    ListStatItem(
                        icon = Icons.Default.PhotoCamera,
                        value = stats.photosCount.toString(),
                        label = stringResource(R.string.checkup_component_card_stat_photos)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left
            CheckupStatusChip(status = checkup.status)

            // Right
            Text(
                text = checkup.updatedAt.toItalianLastModified(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun CompactCheckupCard(
    checkup: CheckUp,
    stats: CheckUpSingleStatistics?
) {
    Row(
        modifier = Modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = checkup.header.checkUpDate.toItalianDateTime(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = checkup.header.clientInfo.companyName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        if (stats != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ListStatItem(
                    icon = Icons.Default.Business,
                    value = stats.okItems.toString(),
                    label = stringResource(R.string.checkup_component_card_stat_ok),
                    compact = true
                )
                ListStatItem(
                    icon = Icons.AutoMirrored.Default.Assignment,
                    value = stats.pendingItems.toString(),
                    label = stringResource(R.string.checkup_component_card_stat_pending),
                    compact = true
                )
            }
        }

        StatusIndicator(isActive = checkup.status == CheckUpStatus.COMPLETED)
    }
}

@Composable
private fun MinimalCheckupCard(checkup: CheckUp) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = checkup.header.checkUpDate.toItalianDateTime(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        StatusIndicator(isActive = checkup.status == CheckUpStatus.COMPLETED)
    }
}

/**
 * Varianti di visualizzazione per ClientCard
 */
enum class CheckupCardVariant {
    FULL,       // Card completa con tutte le informazioni
    COMPACT,    // Card compatta per liste dense
    MINIMAL     // Card minimalista per selezioni
}