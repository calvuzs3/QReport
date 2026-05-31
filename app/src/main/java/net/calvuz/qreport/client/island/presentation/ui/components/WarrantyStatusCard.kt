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
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.usecase.SingleIslandStatistics
import net.calvuz.qreport.client.island.domain.usecase.WarrantyStatus

@Composable
fun WarrantyStatusCard(island: Island, statistics: SingleIslandStatistics?) {
    val warrantyStatus = statistics?.warrantyStats?.status

    // Map status to theme color token — no hex codes in presentation layer
    val statusColor = when (warrantyStatus) {
        WarrantyStatus.ACTIVE -> MaterialTheme.colorScheme.tertiary
        WarrantyStatus.EXPIRING_THIS_QUARTER -> MaterialTheme.colorScheme.secondary
        WarrantyStatus.EXPIRING_SOON -> MaterialTheme.colorScheme.error
        WarrantyStatus.EXPIRED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.island_warranty_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = warrantyStatus?.let { stringResource(it.labelResId) }
                            ?: stringResource(R.string.island_warranty_no_info),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider()

            island.warrantyExpiration?.let { expiryDate ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(R.string.island_warranty_expiry_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = expiryDate.toItalianDate(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }

                statistics?.warrantyStats?.daysRemaining?.let { daysRemaining ->
                    if (daysRemaining > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = stringResource(R.string.island_warranty_days_remaining_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = stringResource(R.string.island_warranty_days_remaining_value, daysRemaining),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    daysRemaining <= 30 -> MaterialTheme.colorScheme.error
                                    daysRemaining <= 90 -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            } ?: run {
                Text(text = stringResource(R.string.island_warranty_not_available), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}