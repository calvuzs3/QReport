package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.ui.theme.onSuccessContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.onWarningContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.success
import net.calvuz.qreport.app.app.presentation.ui.theme.successContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.warningContainer
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandOperationalStatus
import net.calvuz.qreport.client.island.presentation.model.icon
import net.calvuz.qreport.settings.domain.model.ListViewMode

/**
 * Island Card — three variants (FULL / COMPACT / MINIMAL).
 *
 * Consistent with [MechanicalUnitCard]:
 * - FULL  : type chip + status badge in header, all details, stats, maintenance row, footer
 * - COMPACT: type label + name + serial + status icon + actions
 * - MINIMAL: name + status icon only
 *
 * Delete confirmation is handled internally — no local dialog state needed by callers.
 */
@Composable
fun IslandCard(
    modifier: Modifier = Modifier,
    island: Island,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    showActions: Boolean = true,
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
            ListViewMode.FULL -> FullIslandCard(
                island = island,
                showActions = showActions,
                onEdit = onEdit,
                onDelete = if (onDelete != null) { { showDeleteDialog = true } } else null
            )
            ListViewMode.COMPACT -> CompactIslandCard(
                island = island,
                showActions = showActions,
                onEdit = onEdit,
                onDelete = if (onDelete != null) { { showDeleteDialog = true } } else null
            )
            ListViewMode.MINIMAL -> MinimalIslandCard(island)
        }
    }

    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.island_card_delete_dialog_title)) },
            text = { Text(stringResource(R.string.island_card_delete_dialog_message, island.displayName)) },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.island_card_delete_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

// =============================================================================
// FULL
// =============================================================================

@Composable
private fun FullIslandCard(
    island: Island,
    showActions: Boolean,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SuggestionChip(
                onClick = {},
                label = {
                    Icon(
                        imageVector = island.islandType.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(island.islandType.labelResId),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )

            if (showActions) {
                IslandActionButtons(onEdit = onEdit, onDelete = onDelete, large = true)
            }
        }

        // ── Nome + serial ─────────────────────────────────────────────────────
        Text(
            text = island.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (island.customName != null) {
            Text(
                text = island.serialNumber,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // ── Dettagli ──────────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            island.location?.let {
                IslandDetailRow(
                    icon = Icons.Outlined.LocationOn,
                    label = stringResource(R.string.island_card_field_location),
                    value = it
                )
            }
            island.modelNumber?.let {
                IslandDetailRow(
                    icon = Icons.Outlined.Info,
                    label = stringResource(R.string.island_card_field_model),
                    value = it
                )
            }
            island.installationDate?.let {
                IslandDetailRow(
                    icon = Icons.Outlined.CalendarToday,
                    label = stringResource(R.string.island_card_field_installation),
                    value = it.toItalianDate()
                )
            }
        }

        // ── Stats operative ───────────────────────────────────────────────────
        IslandOperationalStats(island = island)

        // ── Manutenzione ──────────────────────────────────────────────────────
        IslandMaintenanceRow(island = island)

        // ── Footer: timestamp sx + stato operativo dx ─────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string.island_card_commissioning_number,
                    island.commissioningNumber ?: "—"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IslandStatusBadge(status = island.islandOperationalStatus)
        }
    }
}

// =============================================================================
// COMPACT
// =============================================================================

@Composable
private fun CompactIslandCard(
    island: Island,
    showActions: Boolean,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icona tipo
        Icon(
            imageVector = island.islandType.icon(),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(island.islandType.labelResId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = island.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = island.serialNumber,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IslandStatusIcon(status = island.islandOperationalStatus)
            if (showActions) {
                IslandActionButtons(onEdit = onEdit, onDelete = onDelete, large = false)
            }
        }
    }
}

// =============================================================================
// MINIMAL
// =============================================================================

@Composable
private fun MinimalIslandCard(island: Island) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = island.displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IslandStatusIcon(status = island.islandOperationalStatus)
    }
}

// =============================================================================
// SHARED COMPOSABLES
// =============================================================================

@Composable
private fun IslandDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun IslandOperationalStats(island: Island) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IslandStatItem(icon = Icons.Default.Schedule, label = stringResource(R.string.island_card_stat_hours), value = "${island.operatingHours}h")
            IslandStatItem(icon = Icons.Default.Repeat, label = stringResource(R.string.island_card_stat_cycles), value = formatCycleCount(island.cycleCount))
            if (island.installationDate != null) {
                IslandStatItem(icon = Icons.Default.CalendarToday, label = stringResource(R.string.island_card_stat_installed), value = island.installationDate.toItalianDate())
            }
        }
    }
}

@Composable
private fun IslandMaintenanceRow(island: Island) {
    val isDue = island.needsMaintenance()
    val days = island.daysToNextMaintenance()
    if (!isDue && days == null) return

    val (containerColor, contentColor, icon, text) = when {
        isDue -> Quadruple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Error,
            stringResource(R.string.island_maintenance_today)
        )
        days != null && days <= 7 -> Quadruple(
            MaterialTheme.colorScheme.warningContainer,
            MaterialTheme.colorScheme.onWarningContainer,
            Icons.Default.Warning,
            stringResource(R.string.island_maintenance_days_soon, days)
        )
        else -> Quadruple(
            MaterialTheme.colorScheme.successContainer,
            MaterialTheme.colorScheme.onSuccessContainer,
            Icons.Default.CheckCircle,
            stringResource(R.string.island_maintenance_days_ahead, days ?: 0)
        )
    }

    Card(colors = CardDefaults.cardColors(containerColor = containerColor), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = contentColor)
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = contentColor, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Chip testuale per la FULL — mostra label dello stato con colore tema.
 * Segue lo stesso pattern di UnitStatusBadge in MechanicalUnitCard.
 */
@Composable
private fun IslandStatusBadge(status: IslandOperationalStatus) {
    val (containerColor, labelColor) = when (status) {
        IslandOperationalStatus.OPERATIONAL ->
            MaterialTheme.colorScheme.successContainer to MaterialTheme.colorScheme.onSuccessContainer
        IslandOperationalStatus.MAINTENANCE_DUE ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        IslandOperationalStatus.INACTIVE ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    SuggestionChip(
        onClick = {},
        label = { Text(stringResource(status.labelResId), style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = containerColor, labelColor = labelColor)
    )
}

/**
 * Icona stato per COMPACT e MINIMAL.
 */
@Composable
private fun IslandStatusIcon(status: IslandOperationalStatus) {
    val (icon, tint) = when (status) {
        IslandOperationalStatus.OPERATIONAL ->
            Icons.Default.CheckCircle to MaterialTheme.colorScheme.success
        IslandOperationalStatus.MAINTENANCE_DUE ->
            Icons.Default.Warning to MaterialTheme.colorScheme.error
        IslandOperationalStatus.INACTIVE ->
            Icons.Default.Cancel to MaterialTheme.colorScheme.outline
    }
    Icon(imageVector = icon, contentDescription = stringResource(status.labelResId), tint = tint, modifier = Modifier.size(18.dp))
}

/**
 * Bottoni edit/delete — [large] per FULL (48dp), standard per COMPACT (36dp).
 */
@Composable
private fun IslandActionButtons(onEdit: (() -> Unit)?, onDelete: (() -> Unit)?, large: Boolean) {
    val buttonSize = if (large) 48.dp else 36.dp
    val iconSize = if (large) 24.dp else 20.dp
    Row {
        if (onEdit != null) {
            IconButton(onClick = onEdit, modifier = Modifier.size(buttonSize)) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(iconSize))
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(buttonSize)) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(iconSize))
            }
        }
    }
}

@Composable
private fun IslandStatItem(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

// =============================================================================
// HELPERS
// =============================================================================

private fun formatCycleCount(cycleCount: Long): String = when {
    cycleCount >= 1_000_000 -> "${cycleCount / 1_000_000}M"
    cycleCount >= 1_000 -> "${cycleCount / 1_000}K"
    else -> cycleCount.toString()
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)