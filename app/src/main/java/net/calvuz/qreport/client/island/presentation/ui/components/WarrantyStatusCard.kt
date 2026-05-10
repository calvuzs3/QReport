package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.usecase.SingleIslandStatistics
import net.calvuz.qreport.client.island.domain.usecase.WarrantyStatus

@Composable
fun WarrantyStatusCard(
    island: Island,
    statistics: SingleIslandStatistics?
) {
    val warrantyStatus = statistics?.warrantyStats?.status
    val isUnderWarranty = island.isUnderWarranty()

    Card {
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
                    text = "Stato Garanzia",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Status indicator
                Surface(
                    color = when (warrantyStatus) {
                        WarrantyStatus.ACTIVE -> Color(0xFF00B050)
                        WarrantyStatus.EXPIRING_SOON -> Color(0xFFFFC000)
                        WarrantyStatus.EXPIRED -> Color(0xFFFF0000)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = warrantyStatus?.displayName ?: "Non specificata",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = when (warrantyStatus) {
                            WarrantyStatus.ACTIVE -> Color(0xFF00B050)
                            WarrantyStatus.EXPIRING_SOON -> Color(0xFFFFC000)
                            WarrantyStatus.EXPIRED -> Color(0xFFFF0000)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            HorizontalDivider()

            island.warrantyExpiration?.let { expiryDate ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Scadenza Garanzia",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = expiryDate.toItalianDate(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                statistics?.warrantyStats?.daysRemaining?.let { daysRemaining ->
                    if (daysRemaining > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Giorni Rimanenti",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "$daysRemaining giorni",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    daysRemaining <= 30 -> Color(0xFFFF0000)
                                    daysRemaining <= 90 -> Color(0xFFFFC000)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            } ?: run {
                Text(
                    text = "Nessuna informazione sulla garanzia disponibile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}