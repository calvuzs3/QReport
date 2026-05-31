package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.util.SizeUtils.getFormattedCycleCount
import net.calvuz.qreport.client.island.domain.usecase.FacilityOperationalSummary

@Composable
fun IslandStatisticsHeader(
    statistics: FacilityOperationalSummary,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(R.string.island_stats_header_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatisticItem(icon = Icons.Default.Analytics, label = stringResource(R.string.island_stats_total), value = statistics.totalIslands.toString(), color = MaterialTheme.colorScheme.primary)
                StatisticItem(icon = Icons.Default.CheckCircle, label = stringResource(R.string.island_stats_active), value = statistics.activeIslands.toString(), color = MaterialTheme.colorScheme.tertiary)
                StatisticItem(icon = Icons.Default.Warning, label = stringResource(R.string.island_stats_maintenance), value = statistics.islandsDueMaintenance.toString(), color = MaterialTheme.colorScheme.error)
                StatisticItem(icon = Icons.Default.Shield, label = stringResource(R.string.island_stats_warranty), value = statistics.islandsUnderWarranty.toString(), color = MaterialTheme.colorScheme.secondary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = stringResource(R.string.island_stats_total_hours, statistics.totalOperatingHours), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(text = stringResource(R.string.island_stats_avg_hours, statistics.averageOperatingHours), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = stringResource(R.string.island_stats_cycles, statistics.totalCycles.getFormattedCycleCount()), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(text = stringResource(R.string.island_stats_global_performance), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(icon: ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = color)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}