package net.calvuz.qreport.client.client.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.presentation.model.ClientStatistics
import net.calvuz.qreport.app.app.presentation.components.QReportConfirmDeleteDialog
import net.calvuz.qreport.app.app.presentation.components.ListStatItem
import net.calvuz.qreport.app.app.presentation.components.StatusIndicator
import androidx.compose.ui.res.stringResource
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.ui.theme.onSuccessContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.successContainer
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianLastModified
import net.calvuz.qreport.settings.domain.model.ListViewMode

/**
 * ClientCard
 *
 * Client main infos with stats
 */

@Composable
fun ClientCard(
    modifier: Modifier = Modifier,
    client: Client,
    stats: ClientStatistics? = null,
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
            ListViewMode.FULL -> FullClientCard(
                client = client,
                stats = stats,
                showActions = showActions,
                onDelete = { showDeleteDialog = false },
                onEdit = onEdit
            )

            ListViewMode.COMPACT -> CompactClientCard(
                client = client,
                stats = stats
            )

            ListViewMode.MINIMAL -> MinimalClientCard(client = client)
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && onDelete != null) {
        QReportConfirmDeleteDialog(
            objectName = stringResource(R.string.client_card_delete_object_name),
            objectDesc = client.companyName,
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun FullClientCard(
    client: Client,
    stats: ClientStatistics?,
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
                text = client.companyName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (showActions) {
                Row {
                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.client_card_action_edit),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Industry and headquarters
        if (client.headquarters != null) {
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
                    text = client.headquarters.toDisplayString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Statistics row
        if (stats != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ListStatItem(
                    icon = Icons.Default.Business,
                    value = stats.facilitiesCount.toString(),
                    label = stringResource(R.string.client_card_stat_facilities)
                )

                ListStatItem(
                    icon = Icons.Default.PrecisionManufacturing,
                    value = stats.islandsCount.toString(),
                    label = stringResource(R.string.client_card_stat_islands)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ListStatItem(
                    icon = Icons.Default.AssignmentTurnedIn,
                    value = stats.contractsCount.toString(),
                    label = stringResource(R.string.client_card_stat_contracts)
                )

                ListStatItem(
                    icon = Icons.AutoMirrored.Default.Assignment,
                    value = stats.totalCheckUps.toString(),
                    label = stringResource(R.string.client_card_stat_checkups)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ListStatItem(
                    icon = Icons.Default.People,
                    value = stats.contactsCount.toString(),
                    label = stringResource(R.string.client_card_stat_contacts)
                )
            }
        }

        // Last modified
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left
            ClientStatusChip(isActive = client.isActive)

            // Right
            Text(
                text = client.updatedAt.toItalianLastModified(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompactClientCard(
    client: Client,
    stats: ClientStatistics?
) {
    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
//            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.companyName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (client.headquarters?.city != null) {
                    Text(
                        text = client.headquarters.city,
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
                        value = stats.facilitiesCount.toString(),
                        label = "",
                        compact = true
                    )
                    ListStatItem(
                        icon = Icons.AutoMirrored.Default.Assignment,
                        value = stats.totalCheckUps.toString(),
                        label = "",
                        compact = true
                    )
                }
            }

            StatusIndicator(isActive = client.isActive)
        }
    }
}

@Composable
private fun MinimalClientCard(client: Client) {

    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = client.companyName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        StatusIndicator(isActive = client.isActive)
    }
}

@Composable
private fun ClientStatusChip(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val (text, containerColor, labelColor) = if (isActive) {
        Triple(
            stringResource(R.string.client_card_status_active),
            MaterialTheme.colorScheme.successContainer,
            MaterialTheme.colorScheme.onSuccessContainer
        )
    } else {
        Triple(
            stringResource(R.string.client_card_status_inactive),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }

    AssistChip(
        onClick = {},
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = labelColor
        ),
        modifier = modifier
    )
}