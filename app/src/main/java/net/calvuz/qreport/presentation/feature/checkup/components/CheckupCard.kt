package net.calvuz.qreport.presentation.feature.checkup.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.domain.model.checkup.CheckUp
import net.calvuz.qreport.domain.model.checkup.CheckUpSingleStatistics
import net.calvuz.qreport.domain.model.checkup.CheckUpStatus
import net.calvuz.qreport.presentation.core.components.ConfirmDeleteDialog
import net.calvuz.qreport.presentation.core.components.ListStatItem
import net.calvuz.qreport.presentation.core.components.StatusIndicator
import net.calvuz.qreport.util.DateTimeUtils.toItalianDateTime
import net.calvuz.qreport.util.DateTimeUtils.toItalianLastModified
import net.calvuz.qreport.util.NumberUtils.toItalianPercentage


@Composable
fun CheckupCard(
    modifier: Modifier = Modifier,

    checkup: CheckUp,
    stats: CheckUpSingleStatistics? = null,
    onClick: () -> Unit,
    showActions: Boolean = true,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    variant: CheckupCardVariant = CheckupCardVariant.FULL
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
            CheckupCardVariant.FULL -> FullCheckupCard(
                checkup = checkup,
                stats = stats,
                showActions = showActions,
                onDelete = { showDeleteDialog = false },
                onEdit = onEdit
            )

            CheckupCardVariant.COMPACT -> CompactCheckupCard(
                checkup = checkup,
                stats = stats
            )

            CheckupCardVariant.MINIMAL -> MinimalCheckupCard(
                checkup = checkup
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && onDelete != null) {
        ConfirmDeleteDialog(
            objectName = "Check-up",
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            //
            Text(
                text = checkup.header.checkUpDate.toItalianDateTime(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (showActions) {
                Row {
                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Modifica Check-up",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

//                    if (onDelete != null) {
//                        IconButton(
//                            onClick = onDelete,
//                            modifier = Modifier.size(24.dp)
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Delete,
//                                contentDescription = "Elimina cliente",
//                                tint = MaterialTheme.colorScheme.error,
//                                modifier = Modifier.size(20.dp)
//                            )
//                        }
//                    }
                }
            }
        }

        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //
            Text(
                text = checkup.header.clientInfo.companyName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // Island info
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PrecisionManufacturing,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Isola ${checkup.header.islandInfo.serialNumber}",
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
                        text = "Progresso",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stats.completionPercentage.toItalianPercentage(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LinearProgressIndicator(
                    progress = { stats.completionPercentage },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ListStatItem(
                    icon = Icons.Default.CheckCircle,
                    value = "${stats.okItems}/${stats.totalItems}",
                    label = "OK"
                )

                if (stats.nokItems > 0) {
                    ListStatItem(
                        icon = Icons.Default.Error,
                        value = stats.nokItems.toString(),
                        label = "NOK",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (stats.photosCount > 0) {
                    ListStatItem(
                        icon = Icons.Default.PhotoCamera,
                        value = stats.photosCount.toString(),
                        label = "Foto"
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = checkup.header.checkUpDate.toItalianDateTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
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

            if (true) {
                Text(
                    text = checkup.header.clientInfo.companyName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        if (stats != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ListStatItem(
                    icon = Icons.Default.Business,
                    value = stats.okItems.toString(),
                    label = "OK",
                    compact = true
                )
                ListStatItem(
                    icon = Icons.AutoMirrored.Default.Assignment,
                    value = stats.pendingItems.toString(),
                    label = "Pending",
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


@Composable
private fun CheckupStatusChip(
    status: CheckUpStatus,
    modifier: Modifier = Modifier
) {
    val (text, containerColor) = when (status) {
        CheckUpStatus.DRAFT -> "Bozza" to MaterialTheme.colorScheme.tertiaryContainer
        CheckUpStatus.IN_PROGRESS -> "In Corso" to MaterialTheme.colorScheme.primaryContainer
        CheckUpStatus.COMPLETED -> "Completato" to MaterialTheme.colorScheme.surfaceVariant
        CheckUpStatus.EXPORTED -> "Esportato" to MaterialTheme.colorScheme.secondaryContainer
        CheckUpStatus.ARCHIVED -> "Archiviato" to MaterialTheme.colorScheme.outline
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

/**
 * Varianti di visualizzazione per ClientCard
 */
enum class CheckupCardVariant {
    FULL,       // Card completa con tutte le informazioni
    COMPACT,    // Card compatta per liste dense
    MINIMAL     // Card minimalista per selezioni
}