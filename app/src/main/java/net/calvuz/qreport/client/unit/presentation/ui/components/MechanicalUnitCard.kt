package net.calvuz.qreport.client.unit.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.QReportConfirmDeleteDialog
import net.calvuz.qreport.app.app.presentation.components.QReportConfirmRestoreDialog
import net.calvuz.qreport.app.app.presentation.components.QrCardFooter
import net.calvuz.qreport.app.app.presentation.components.QrCardFooterData
import net.calvuz.qreport.app.app.presentation.components.QrStatusIndicator
import net.calvuz.qreport.app.app.presentation.ui.theme.success
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.settings.domain.model.ListViewMode

@Suppress("ParamsComparedByRef", "HardCodedStringLiteral", "ASSIGNED_VALUE_IS_NEVER_READ")
@Composable
fun MechanicalUnitCard(
    modifier: Modifier = Modifier,
    unit: MechanicalUnit,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    showActions: Boolean = true,
    variant: ListViewMode = ListViewMode.FULL
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        when (variant) {
            ListViewMode.FULL -> FullMechanicalUnitCard(
                unit = unit,
                showActions = showActions,
                onEdit = onEdit,
                onDelete = if (onDelete != null) { { showDeleteDialog = true } } else null,
                onRestore = if (onRestore != null) { { showRestoreDialog = true } } else null
            )
            ListViewMode.COMPACT -> CompactMechanicalUnitCard(
                unit = unit,
                showActions = showActions,
                onEdit = onEdit,
                onDelete = if (onDelete != null) { { showDeleteDialog = true } } else null,
                onRestore = if (onRestore != null) { { showRestoreDialog = true } } else null
            )
            ListViewMode.MINIMAL -> MinimalMechanicalUnitCard(unit)
        }
    }

    if (showDeleteDialog && onDelete != null) {
        QReportConfirmDeleteDialog(
            objectName = stringResource(R.string.unit_card_object_name),
            objectDesc = unit.name,
            onConfirm = { onDelete(); showDeleteDialog = false },
            onDismiss = { showDeleteDialog = false }
        )
    }
    
    if (showRestoreDialog && onRestore != null) {
        QReportConfirmRestoreDialog(
            objectName = stringResource(R.string.unit_card_object_name),
            objectDesc = unit.name,
            onConfirm = { onRestore(); showRestoreDialog = false },
            onDismiss = { showRestoreDialog = false }
        )
    }
}

// =============================================================================
// FULL — tutti i dettagli, bottoni grandi per uso con guanti
// =============================================================================

@Suppress("ParamsComparedByRef")@Composable
private fun FullMechanicalUnitCard(
    unit: MechanicalUnit,
    showActions: Boolean,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onRestore: (() -> Unit)?
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header: tipo badge + azioni ──────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = stringResource(unit.unitType.labelResId),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )

            if (showActions) {
                UnitActionButtons(onEdit = onEdit, onDelete = onDelete, large = true)
            }
        }

        // ── Nome ──────────────────────────────────────────────────────────────
        Text(
            text = unit.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // ── Dettagli tecnici ─────────────────────────────────────────────────
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            unit.serialNumber?.let {
                DetailRow(
                    label = stringResource(R.string.unit_card_serial_label),
                    value = it
                )
            }
            unit.model?.let {
                DetailRow(
                    label = stringResource(R.string.unit_card_model_label),
                    value = it
                )
            }
            unit.notes?.takeIf { it.isNotBlank() }?.let {
                DetailRow(
                    label = stringResource(R.string.unit_card_notes_label),
                    value = it,
                    maxLines = 2
                )
            }
        }

        // ── Footer: timestamp a sinistra, stato a destra ─────────────────────
        QrCardFooter(
            QrCardFooterData(
                date = unit.updatedAt,
                isActive = unit.isActive,
                onRestore = onRestore
            )
        )
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text(
//                text = stringResource(
//                    R.string.unit_card_last_modified,
//                    unit.updatedAt.toItalianLastModified()
//                ),
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//            UnitStatusBadge(isActive = unit.isActive)
//        }
    }
}

// =============================================================================
// COMPACT — una riga header + subtitle, bottoni standard
// =============================================================================

@Suppress("ParamsComparedByRef")@Composable
private fun CompactMechanicalUnitCard(
    unit: MechanicalUnit,
    showActions: Boolean,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onRestore: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Colonna principale
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Tipo piccolo sopra il nome
            Text(
                text = stringResource(unit.unitType.labelResId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = unit.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Seriale · Modello su una riga
            val subtitle = listOfNotNull(unit.serialNumber, unit.model).joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Stato + azioni
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            QrStatusIndicator(
                isActive = unit.isActive,
                onRestore = onRestore
            )
            
            if (showActions) {
                UnitActionButtons(onEdit = onEdit, onDelete = onDelete, large = false)
            }
        }
    }
}

// =============================================================================
// MINIMAL — solo nome e stato
// =============================================================================

@Suppress("ParamsComparedByRef")@Composable
private fun MinimalMechanicalUnitCard(unit: MechanicalUnit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = unit.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        UnitStatusIcon(isActive = unit.isActive)
    }
}

// =============================================================================
// SHARED COMPOSABLES
// =============================================================================

/**
 * Label + valore su una riga, per la scheda full.
 */
@Composable
private fun DetailRow(label: String, value: String, maxLines: Int = 1) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

///**
// * Chip testuale per la scheda full — "Attiva" / "Non attiva".
// */
//@Composable
//private fun UnitStatusBadge(isActive: Boolean) {
//    val (text, colors) = if (isActive) {
//        stringResource(R.string.unit_card_status_active) to
//                SuggestionChipDefaults.suggestionChipColors(
//                    containerColor = MaterialTheme.colorScheme.successContainer,
//                    labelColor = MaterialTheme.colorScheme.onSuccessContainer
//                )
//    } else {
//        stringResource(R.string.unit_card_status_inactive) to
//                SuggestionChipDefaults.suggestionChipColors(
//                    containerColor = MaterialTheme.colorScheme.errorContainer,
//                    labelColor = MaterialTheme.colorScheme.onErrorContainer
//                )
//    }
//    SuggestionChip(
//        onClick = {},
//        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
//        colors = colors
//    )
//}

/**
 * Icona stato per COMPACT e MINIMAL.
 */
@Composable
private fun UnitStatusIcon(isActive: Boolean) {
    Icon(
        imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
        contentDescription = if (isActive)
            stringResource(R.string.unit_card_status_active)
        else
            stringResource(R.string.unit_card_status_inactive),
        tint = if (isActive) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.error,
        modifier = Modifier.size(18.dp)
    )
}

/**
 * Bottoni edit/delete.
 * [large] = true per la scheda FULL (48dp touch target, uso con guanti).
 * [large] = false per COMPACT (32dp).
 */
@Composable
private fun UnitActionButtons(
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    large: Boolean
) {
    val buttonSize = if (large) 48.dp else 36.dp
    val iconSize = if (large) 24.dp else 20.dp

    Row {
        if (onEdit != null) {
            IconButton(onClick = onEdit, modifier = Modifier.size(buttonSize)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.action_edit),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(buttonSize)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}