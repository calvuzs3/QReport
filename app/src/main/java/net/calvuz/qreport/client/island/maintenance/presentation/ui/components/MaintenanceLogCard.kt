package net.calvuz.qreport.client.island.maintenance.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate
import net.calvuz.qreport.app.app.presentation.ui.theme.onSuccessContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.onWarningContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.successContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.warningContainer
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceLog
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceOutcome

/**
 * Card displaying a single [MaintenanceLog] entry.
 *
 * Variants:
 *  - COMPACT (default): date, operation type, outcome badge, component if any.
 *    Tapping expands to FULL inline.
 *  - FULL: all COMPACT fields + description, technician, duration, machine state, notes.
 *
 * Expansion state is managed internally — no ViewModel needed for the card itself.
 */
@Composable
fun MaintenanceLogCard(
    log: MaintenanceLog,
    modifier: Modifier = Modifier,
    unitName: String? = null,           // resolved by caller from available units list
    initiallyExpanded: Boolean = false
) {
    var expanded by remember(log.id) { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Always visible: header row ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date
                Text(
                    text = log.performedAt.toItalianDate(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Outcome badge
                OutcomeBadge(outcome = log.outcome)
            }

            // Operation type
            Text(
                text = log.effectiveOperationLabel(stringResource(log.operationType.labelResId)),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            // Component target — shown if present
            val component = log.effectiveComponentLabel(unitName)
            if (!component.isNullOrBlank()) {
                Text(
                    text = component,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Expanded content ──────────────────────────────────────────────
            if (expanded) {
                HorizontalDivider()

                // Description
                Text(
                    text = log.description,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Technician row
                DetailRow(
                    label = stringResource(R.string.maint_label_technician),
                    value = listOfNotNull(log.technicianName, log.technicianCompany)
                        .joinToString(" · ")
                )

                // Duration
                log.durationMinutes?.let {
                    DetailRow(
                        label = stringResource(R.string.maint_label_duration),
                        value = stringResource(R.string.maint_health_avg_duration_value, it.toDouble())
                    )
                }

                // Machine state snapshot
                val machineState = buildString {
                    log.operatingHoursAtEvent?.let { append(stringResource(R.string.maint_label_operating_hours) + ": $it h") }
                    if (log.operatingHoursAtEvent != null && log.cycleCountAtEvent != null) append("  |  ")
                    log.cycleCountAtEvent?.let { append(stringResource(R.string.maint_label_cycle_count) + ": $it") }
                }
                if (machineState.isNotBlank()) {
                    DetailRow(
                        label = stringResource(R.string.island_form_section_technical),
                        value = machineState
                    )
                }

                // Notes
                if (!log.notes.isNullOrBlank()) {
                    DetailRow(
                        label = stringResource(R.string.maint_label_notes),
                        value = log.notes
                    )
                }
            }
        }
    }
}

// =============================================================================
// OUTCOME BADGE
// =============================================================================

@Composable
private fun OutcomeBadge(outcome: MaintenanceOutcome) {
    val (containerColor, contentColor, icon) = when (outcome) {
        MaintenanceOutcome.COMPLETED ->
            Triple(
                MaterialTheme.colorScheme.successContainer,
                MaterialTheme.colorScheme.onSuccessContainer,
                Icons.Default.CheckCircle
            )
        MaintenanceOutcome.PARTIAL ->
            Triple(
                MaterialTheme.colorScheme.warningContainer,
                MaterialTheme.colorScheme.onWarningContainer,
                Icons.Default.Build
            )
        MaintenanceOutcome.DEFERRED ->
            Triple(
                MaterialTheme.colorScheme.warningContainer,
                MaterialTheme.colorScheme.onWarningContainer,
                Icons.Default.HourglassEmpty
            )
        MaintenanceOutcome.REQUIRES_PARTS ->
            Triple(
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer,
                Icons.Default.Inventory
            )
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = contentColor
            )
            Text(
                text = stringResource(outcome.labelResId),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// =============================================================================
// DETAIL ROW
// =============================================================================

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}