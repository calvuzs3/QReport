package net.calvuz.qreport.presentation.feature.client.client.components

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
import kotlinx.datetime.Clock
import net.calvuz.qreport.domain.model.client.Client
import net.calvuz.qreport.domain.model.client.ClientSingleStatistics
import net.calvuz.qreport.presentation.core.components.ConfirmDeleteDialog
import net.calvuz.qreport.presentation.core.components.ListStatItem
import net.calvuz.qreport.presentation.core.components.StatusIndicator
import net.calvuz.qreport.util.DateTimeUtils.toItalianLastModified

/**
 * ClientCard riutilizzabile per QReport
 *
 * Mostra le informazioni principali del cliente con statistiche
 * Supporta diverse modalità di visualizzazione e azioni
 */

@Composable
fun ClientCard(
    modifier: Modifier = Modifier,
    client: Client,
    stats: ClientSingleStatistics? = null,
    onClick: () -> Unit,
    showActions: Boolean = true,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    variant: ClientCardVariant = ClientCardVariant.FULL
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        when (variant) {
            ClientCardVariant.FULL -> FullClientCard(
                client = client,
                stats = stats,
                showActions = showActions,
                onDelete = { showDeleteDialog = false },
                onEdit = onEdit
            )
            ClientCardVariant.COMPACT -> CompactClientCard(
                client = client,
                stats = stats
            )
            ClientCardVariant.MINIMAL -> MinimalClientCard(client = client)
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && onDelete != null) {
        ConfirmDeleteDialog(
            objectName = "Cliente",
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
    stats: ClientSingleStatistics?,
    showActions: Boolean,
    onDelete: (() -> Unit)?,
    onEdit: (() -> Unit)?
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header row - Solo nome azienda e azioni
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ FIXED: Solo nome azienda, rimossa P.IVA
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
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Modifica cliente",
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

        // Industry and headquarters
        if (!client.industry.isNullOrBlank()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = client.industry,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

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

        // ✅ FIXED: Statistics row - Solo statistiche, senza timestamp
        if (stats != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ListStatItem(
                    icon = Icons.Default.Business,
                    value = stats.facilitiesCount.toString(),
                    label = "Stabilimenti"
                )

                ListStatItem(
                    icon = Icons.Default.PrecisionManufacturing,
                    value = stats.islandsCount.toString(),
                    label = "Isole"
                )

                ListStatItem(
                    icon = Icons.Default.People,
                    value = stats.contactsCount.toString(),
                    label = "Contatti"
                )

                ListStatItem(
                    icon = Icons.AutoMirrored.Default.Assignment,
                    value = stats.totalCheckUps.toString(),
                    label = "Check-up"
                )
            }
        }

        // ✅ FIXED: Last modified su riga separata, allineato a destra
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
    stats: ClientSingleStatistics?
) {
    Row(
        modifier = Modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

/**
 * Varianti di visualizzazione per ClientCard
 */
enum class ClientCardVariant {
    FULL,       // Card completa con tutte le informazioni
    COMPACT,    // Card compatta per liste dense
    MINIMAL     // Card minimalista per selezioni
}