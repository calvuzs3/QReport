package net.calvuz.qreport.client.contract.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.client.contract.data.local.isValid
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.app.app.presentation.components.DeleteDialog
import net.calvuz.qreport.app.app.presentation.components.list.QrListItemCard.QrListItemCardVariant
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate

interface QrItemInterface

sealed interface QrListItemInterface<out D : QrItemInterface> {
    data class QrListItem<out D : QrItemInterface>(val item: D) : QrListItemInterface<D>
}

@Composable
fun ContractCard(
    modifier: Modifier = Modifier,
    contract: Contract,
    onClick: (() -> Unit)? = null,
    showActions: Boolean = true,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    variant: QrListItemCardVariant = QrListItemCardVariant.FULL
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
//        onClick = if (onClick != null) onClick else null ,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        when (variant) {
            QrListItemCardVariant.FULL -> FullContactCard(
                contract = contract,
                showActions = showActions,
                onDelete = onDelete,
                onEdit = onEdit,
            )

            QrListItemCardVariant.COMPACT -> CompactContactCard(
                contract = contract,
            )

            QrListItemCardVariant.MINIMAL -> MinimalContactCard(contract = contract)
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && onDelete != null) {
        DeleteDialog(
            title = stringResource(R.string.action_delete), //"Elimina Contatto",
            text = stringResource(
                R.string.contract_screen_delete_action_confirm,
                "${contract.startDate} - ${contract.endDate}"
            ),
            //"${contract.sta2rtDate} - ${contract.endDate}") //"Sei sicuro di voler eliminare il contatto '${contract.fullName}'? Questa operazione non può essere annullata.",
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun FullContactCard(
    contract: Contract,
    showActions: Boolean = true,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
) {
    val context = LocalContext.current

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
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = "${contract.startDate.toItalianDate()} - ${contract.endDate.toItalianDate()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (showActions) {
                Row {

                    // EDIT
                    if (onEdit != null) {
                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = onEdit
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.action_edit),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // DELETE
                    if (onDelete != null) {
                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = onDelete
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        if (!contract.isValid()) {
            Row {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            stringResource(R.string.label_assistchip_not_active),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    enabled = false
                )
            }
        }

        // MAINTENANCE
        if (contract.hasMaintenance) {
            ContractItem(
                icon = Icons.Default.Build,
                label = stringResource(R.string.label_maintenance),
                value = true.toString(),
                onClick = { },
                isValid = true
            )
        }

        // ASSISTANCE
        if (contract.hasRemoteAssistance) {
            ContractItem(
                icon = Icons.Default.Assistant,
                label = stringResource(R.string.label_assistance),
                value = true.toString(),
                onClick = {},
                isValid = true
            )
        }

        // MOBILE
        if (contract.hasPriority) {
            ContractItem(
                icon = Icons.Default.PriorityHigh,
                label = stringResource(R.string.label_priority),
                value = true.toString(),
                onClick = {},
                isValid = true
            )
        }
    }
}

@Composable
private fun CompactContactCard(
    contract: Contract,
    onCall: () -> Unit = { },
    onEmail: () -> Unit = { }
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
                    text = "${contract.startDate.toItalianDate()} - ${contract.endDate.toItalianDate()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // ROLE
            if (contract.hasMaintenance) {
                Row {
                    Text(
                        text = "${stringResource(R.string.label_maintenance)} ✔️",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MinimalContactCard(contract: Contract) {
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
                    text = "${contract.startDate.toItalianDate()} - ${contract.endDate.toItalianDate()}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (contract.hasMaintenance) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.label_maintenance),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun ContractItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    isValid: Boolean
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

//        IconButton(onClick = onClick) {
//            Icon(
//                imageVector = Icons.AutoMirrored.Default.Launch,
//                contentDescription = "Contatta",
//                modifier = Modifier.size(18.dp)
//            )
//        }
    }
}