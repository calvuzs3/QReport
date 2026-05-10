package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        // ✅ Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ Title
            Text(
                text = "Manutenzioni",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Empty
                Button(onClick = {}) {
//                        Icon(
//                            imageVector = Icons.Default.Add,
//                            contentDescription = null,
//                            modifier = Modifier.size(18.dp)
//                        )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(" ")
                }
            }
        }

        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MaintenanceStatusCard(
                    island = island,
                    statistics = statistics,
                    onMaintenanceAction = onMaintenanceAction
                )
            }
        }
    }
}

@Composable
fun MaintenanceStatusCard(
    island: Island,
    statistics: SingleIslandStatistics?,
    onMaintenanceAction: () -> Unit
) {
    val maintenanceStatus = statistics?.maintenanceStats?.status
    val needsMaintenance = island.needsMaintenance()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                needsMaintenance -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                maintenanceStatus == MaintenanceStatus.DUE_SOON -> MaterialTheme.colorScheme.tertiaryContainer.copy(
                    alpha = 0.3f
                )

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
                    text = "Stato Manutenzione",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (needsMaintenance || maintenanceStatus == MaintenanceStatus.DUE_SOON) {
                    FilledTonalButton(
                        onClick = onMaintenanceAction,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (needsMaintenance) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (needsMaintenance) "Urgente" else "Registra")
                    }
                }
            }

            HorizontalDivider()

            // Stato manutenzione
            Text(
                text = island.maintenanceStatusText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = when {
                    needsMaintenance -> MaterialTheme.colorScheme.error
                    maintenanceStatus == MaintenanceStatus.DUE_SOON -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            // Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                island.lastMaintenanceDate?.let { lastDate ->
                    Column {
                        Text(
                            text = "Ultima Manutenzione",
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
                            text = "Prossima Programmata",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = nextDate.toItalianDate(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (needsMaintenance) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
