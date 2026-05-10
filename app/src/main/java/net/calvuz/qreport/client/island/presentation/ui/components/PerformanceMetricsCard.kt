package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.client.island.domain.usecase.SingleIslandStatistics

@Composable
fun PerformanceMetricsCard(statistics: SingleIslandStatistics?) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )


            HorizontalDivider()

            // Health Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                statistics?.let {
                    Text(
                        text = "Salute Generale",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    LinearProgressIndicator(
                        progress = { statistics.healthScore / 100f },
                        modifier = Modifier
                            .weight(2f)
                            .height(8.dp),
                        color = when {
                            statistics.healthScore >= 80 -> Color(0xFF00B050)
                            statistics.healthScore >= 60 -> Color(0xFFFFC000)
                            else -> Color(0xFFFF0000)
                        }
                    )

                    Text(
                        text = "${statistics.healthScore}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )


                    // Summary text
                    Text(
                        text = statistics.summaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } ?: run {
                    // Summary text
                    Text(
                        text = "Nessuna informazione sulla performance disponibile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}