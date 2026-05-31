package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.usecase.MaintenanceStatus
import net.calvuz.qreport.client.island.domain.usecase.SingleIslandStatistics

@Composable
fun MaintenanceTabContent(
    island: Island,
    statistics: SingleIslandStatistics?,
    onMaintenanceAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.island_detail_tab_maintenance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        MaintenanceStatusCard(island = island, statistics = statistics, onMaintenanceAction = onMaintenanceAction, modifier = Modifier.padding(horizontal = 16.dp))
    }
}

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
                maintenanceStatus == MaintenanceStatus.DUE_SOON -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.island_maintenance_status_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                if (needsMaintenance || maintenanceStatus == MaintenanceStatus.DUE_SOON) {
                    FilledTonalButton(
                        onClick = onMaintenanceAction,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (needsMaintenance) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (needsMaintenance) stringResource(R.string.island_maintenance_urgent)
                            else stringResource(R.string.island_maintenance_register)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Maintenance status label from enum
            maintenanceStatus?.let {
                Text(
                    text = stringResource(it.labelResId),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        needsMaintenance -> MaterialTheme.colorScheme.error
                        maintenanceStatus == MaintenanceStatus.DUE_SOON -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                island.lastMaintenanceDate?.let { lastDate ->
                    Column {
                        Text(text = stringResource(R.string.island_maintenance_last_date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = lastDate.toItalianDate(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                island.nextScheduledMaintenance?.let { nextDate ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = stringResource(R.string.island_maintenance_next_date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = nextDate.toItalianDate(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (needsMaintenance) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}