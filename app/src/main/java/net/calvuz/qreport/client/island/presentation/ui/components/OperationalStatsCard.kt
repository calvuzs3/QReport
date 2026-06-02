package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.util.SizeUtils.getFormattedCycleCount
import net.calvuz.qreport.client.island.domain.usecase.SingleIslandStatistics

@Composable
fun OperationalStatsCard(statistics: SingleIslandStatistics) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(R.string.island_stats_card_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                OperationalStatItem(
                    icon = Icons.Default.Schedule,
                    label = stringResource(R.string.island_stats_hours_label),
                    value = stringResource(R.string.island_stats_hours_value, statistics.operationalStats.operatingHours),
                    subtitle = stringResource(R.string.island_stats_hours_avg, statistics.operationalStats.averageHoursPerDay)
                )
                OperationalStatItem(
                    icon = Icons.Default.Repeat,
                    label = stringResource(R.string.island_stats_cycles_label),
                    value = statistics.operationalStats.cycleCount.getFormattedCycleCount(),
                    subtitle = stringResource(R.string.island_stats_cycles_avg, statistics.operationalStats.averageCyclesPerHour)
                )
                OperationalStatItem(
                    icon = Icons.AutoMirrored.Default.TrendingUp,
                    label = stringResource(R.string.island_stats_uptime_label),
                    value = stringResource(R.string.island_stats_uptime_value, statistics.operationalStats.uptime),
                    subtitle = if (statistics.operationalStats.ageInDays > 0)
                        stringResource(R.string.island_stats_age_days, statistics.operationalStats.ageInDays)
                    else ""
                )
            }
        }
    }
}

@Composable
private fun OperationalStatItem(icon: ImageVector, label: String, value: String, subtitle: String = "") {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        if (subtitle.isNotBlank()) {
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}