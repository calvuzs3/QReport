package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.app.util.SizeUtils.getFormattedCycleCount
import net.calvuz.qreport.client.island.domain.usecase.SingleIslandStatistics

@Composable
fun OperationalStatsCard(statistics: SingleIslandStatistics) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Statistiche Operative",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OperationalStatItem(
                    icon = Icons.Default.Schedule,
                    label = "Ore Totali",
                    value = "${statistics.operationalStats.operatingHours}h",
                    subtitle = "Media: ${statistics.operationalStats.averageHoursPerDay}h/giorno"
                )

                OperationalStatItem(
                    icon = Icons.Default.Repeat,
                    label = "Cicli",
                    value = statistics.operationalStats.cycleCount.getFormattedCycleCount(),
                    subtitle = "Media: ${statistics.operationalStats.averageCyclesPerHour}/ora"
                )

                OperationalStatItem(
                    icon = Icons.AutoMirrored.Default.TrendingUp,
                    label = "Uptime",
                    value = "${statistics.operationalStats.uptime}%",
                    subtitle = if (statistics.operationalStats.ageInDays > 0) "${statistics.operationalStats.ageInDays} giorni attivi" else ""
                )
            }
        }
    }
}

@Composable
private fun OperationalStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    subtitle: String = ""
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
