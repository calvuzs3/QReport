package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.app.app.presentation.ui.theme.onWarningContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.warningContainer
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.usecase.MaintenanceStatus
import net.calvuz.qreport.client.island.domain.usecase.SingleIslandStatistics
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceLog
import net.calvuz.qreport.client.island.maintenance.presentation.ui.components.MaintenanceLogCard
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit

/**
 * Extended MaintenanceTabContent.
 *
 * Structure:
 *  1. [MaintenanceStatusCard]   — existing scheduled maintenance widget (unchanged)
 *  2. Log section header        — "Storico interventi" title + action buttons
 *  3. [MaintenanceLogCard] list — reactive list from MaintenanceLog history
 *
 * Parameters added vs original:
 *  - [logs]                  reactive list of [MaintenanceLog] for this island
 *  - [isLoadingLogs]         loading indicator for the log list
 *  - [availableUnits]        used to resolve mechanicalUnitId → unit name in cards
 *  - [onAddLog]              navigates to MaintenanceLogFormScreen
 *  - [onNavigateToHealth]    navigates to IslandHealthScreen (M5)
 *
 * Original parameters (island, statistics, onMaintenanceAction) are unchanged.
 */
@Composable
fun MaintenanceTabContent(
    island: Island,
    statistics: SingleIslandStatistics?,
    onMaintenanceAction: () -> Unit,
    modifier: Modifier = Modifier,
    // ── New parameters for log list ──────────────────────────────────────────
    logs: List<MaintenanceLog> = emptyList(),
    isLoadingLogs: Boolean = false,
    availableUnits: List<MechanicalUnit> = emptyList(),
    onAddLog: () -> Unit = {},
    onNavigateToHealth: () -> Unit = {}
) {
    // Pre-build unit name lookup map to avoid O(n²) in the list
    val unitNameById: Map<String, String> = availableUnits.associate { it.id to it.name }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),     // room for FAB
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── 1. Scheduled maintenance card (existing, unchanged) ───────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.maint_section_scheduled),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                MaintenanceStatusCard(
                    island = island,
                    statistics = statistics,
                    onMaintenanceAction = onMaintenanceAction
                )
            }
        }

        // ── 2. Log section header ────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.maint_section_history),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Health analysis button (M5 — always shown, disabled until implemented)
                        FilledTonalIconButton(
                            onClick = onNavigateToHealth,
                            enabled = logs.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = stringResource(R.string.maint_btn_health_analysis)
                            )
                        }
                        // Add log button
                        FilledTonalIconButton(onClick = onAddLog) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.maint_fab_add)
                            )
                        }
                    }
                }
            }
        }

        // ── 3. Log list ──────────────────────────────────────────────────────
        when {
            isLoadingLogs -> item {
                LoadingState(modifier = Modifier.height(120.dp))
            }

            logs.isEmpty() -> item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = stringResource(R.string.maint_no_logs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.maint_no_logs_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> items(
                items = logs,
                key = { it.id }
            ) { log ->
                MaintenanceLogCard(
                    log = log,
                    unitName = log.mechanicalUnitId?.let { unitNameById[it] },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// =============================================================================
// MAINTENANCE STATUS CARD — kept from original MaintenanceTabContent.kt
// =============================================================================

@Composable
fun MaintenanceStatusCard(
    island: Island,
    statistics: SingleIslandStatistics?,
    onMaintenanceAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maintenanceStatus = statistics?.maintenanceStats?.status
    val needsMaintenance = island.needsMaintenance()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                needsMaintenance -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                maintenanceStatus == MaintenanceStatus.DUE_SOON ->
                    MaterialTheme.colorScheme.warningContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.island_maintenance_status_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (needsMaintenance || maintenanceStatus == MaintenanceStatus.DUE_SOON) {
                    FilledTonalButton(
                        onClick = onMaintenanceAction,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (needsMaintenance)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.warningContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (needsMaintenance)
                                stringResource(R.string.island_maintenance_urgent)
                            else
                                stringResource(R.string.island_maintenance_register)
                        )
                    }
                }
            }

            HorizontalDivider()

            maintenanceStatus?.let {
                Text(
                    text = stringResource(it.labelResId),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        needsMaintenance -> MaterialTheme.colorScheme.error
                        maintenanceStatus == MaintenanceStatus.DUE_SOON ->
                            MaterialTheme.colorScheme.onWarningContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                island.lastMaintenanceDate?.let { lastDate ->
                    Column {
                        Text(
                            text = stringResource(R.string.island_maintenance_last_date),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = lastDate.toItalianDate(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                island.nextScheduledMaintenance?.let { nextDate ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.island_maintenance_next_date),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = nextDate.toItalianDate(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (needsMaintenance)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}