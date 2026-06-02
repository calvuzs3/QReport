package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.ui.theme.onSuccessContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.onWarningContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.successContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.warningContainer
import net.calvuz.qreport.client.island.domain.usecase.SingleIslandStatistics

@Composable
fun PerformanceMetricsCard(statistics: SingleIslandStatistics?) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(R.string.island_performance_card_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()

            if (statistics == null) {
                Text(
                    text = stringResource(R.string.island_performance_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Health score — progress bar with theme-token colors
                val (progressColor, trackColor) = when {
                    statistics.healthScore >= 80 ->
                        MaterialTheme.colorScheme.successContainer to MaterialTheme.colorScheme.onSuccessContainer
                    statistics.healthScore >= 60 ->
                        MaterialTheme.colorScheme.warningContainer to MaterialTheme.colorScheme.onWarningContainer
                    else ->
                        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.island_performance_health_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    LinearProgressIndicator(
                        progress = { statistics.healthScore / 100f },
                        modifier = Modifier.weight(2f).height(8.dp),
                        color = progressColor,
                        trackColor = trackColor.copy(alpha = 0.3f)
                    )
                    Text(
                        text = stringResource(R.string.island_performance_health_value, statistics.healthScore),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}