package net.calvuz.qreport.client.client.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import net.calvuz.qreport.app.app.presentation.components.QrListStatItem
import net.calvuz.qreport.app.app.presentation.components.QReportConfirmDeleteDialog
import net.calvuz.qreport.app.app.presentation.components.QrCardFooter
import net.calvuz.qreport.app.app.presentation.components.QrCardFooterData
import net.calvuz.qreport.app.app.presentation.components.QrStatusIndicator
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.presentation.model.ClientStatistics
import net.calvuz.qreport.settings.domain.model.ListViewMode

/**
 * ClientCard
 *
 * Client main infos with stats
 */

@Suppress("ParamsComparedByRef", "HardCodedStringLiteral", "ASSIGNED_VALUE_IS_NEVER_READ")
@Composable
fun ClientCard(
    modifier: Modifier = Modifier,
    client: Client,
    stats: ClientStatistics? = null,
    onClick: () -> Unit,
    showActions: Boolean = true,
    onDelete: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
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
                client = client, stats = stats, showActions = showActions,
//                onDelete = { showDeleteDialog = false },
                onDelete = { showDeleteDialog = true }, onRestore = onRestore, onEdit = onEdit
            )

            ListViewMode.COMPACT -> CompactClientCard(
                client = client, onRestore = onRestore, stats = stats
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
            onDismiss = { showDeleteDialog = false })
    }
}

@Suppress("ParamsComparedByRef")
@Composable
private fun FullClientCard(
    client: Client,
    stats: ClientStatistics?,
    showActions: Boolean,
    onDelete: (() -> Unit)?,
    onRestore: (() -> Unit)? = null,
    onEdit: (() -> Unit)?
) {
    Column(
        modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            onClick = onEdit, modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.client_card_action_edit),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete, modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete),
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
                QrListStatItem(
                    icon = Icons.Default.Business,
                    value = stats.facilitiesCount.toString(),
                    label = stringResource(R.string.client_card_stat_facilities)
                )

                QrListStatItem(
                    icon = Icons.Default.PrecisionManufacturing,
                    value = stats.islandsCount.toString(),
                    label = stringResource(R.string.client_card_stat_islands)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QrListStatItem(
                    icon = Icons.Default.AssignmentTurnedIn,
                    value = stats.contractsCount.toString(),
                    label = stringResource(R.string.client_card_stat_contracts)
                )

                QrListStatItem(
                    icon = Icons.AutoMirrored.Default.Assignment,
                    value = stats.totalCheckUps.toString(),
                    label = stringResource(R.string.client_card_stat_checkups)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QrListStatItem(
                    icon = Icons.Default.People,
                    value = stats.contactsCount.toString(),
                    label = stringResource(R.string.client_card_stat_contacts)
                )
            }
        }

        // Last modified
        QrCardFooter(
            data = QrCardFooterData(
                isActive = client.isActive,
                date = client.updatedAt,
                onRestore = onRestore,
            )
        )
    }
}

@Suppress("ParamsComparedByRef")
@Composable
private fun CompactClientCard(
    client: Client, onRestore: (() -> Unit)? = null, stats: ClientStatistics?
) {
    Column(
        modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    QrListStatItem(
                        icon = Icons.Default.Business,
                        value = stats.facilitiesCount.toString(),
                        label = "",
                        compact = true
                    )
                    QrListStatItem(
                        icon = Icons.AutoMirrored.Default.Assignment,
                        value = stats.totalCheckUps.toString(),
                        label = "",
                        compact = true
                    )
                }
            }

            QrStatusIndicator(isActive = client.isActive, onRestore = onRestore)
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
private fun MinimalClientCard(client: Client) {

    Row(
        modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically
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

        QrStatusIndicator(isActive = client.isActive)
    }
}