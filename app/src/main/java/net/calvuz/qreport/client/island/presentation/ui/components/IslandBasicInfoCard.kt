package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate
import net.calvuz.qreport.client.island.domain.model.Island

@Composable
fun IslandBasicInfoCard(island: Island) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.island_detail_info_card_general),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IslandTypeIcon(islandType = island.islandType)
            }

            HorizontalDivider()

            InfoRow(
                label = stringResource(R.string.island_detail_info_field_serial),
                value = island.serialNumber,
                icon = Icons.Outlined.Tag
            )
            InfoRow(
                label = stringResource(R.string.island_detail_info_field_type),
                value = stringResource(island.islandType.labelResId),
                icon = Icons.Outlined.Category
            )
            island.customName?.let {
                InfoRow(
                    label = stringResource(R.string.island_detail_info_field_custom_name),
                    value = it,
                    icon = Icons.AutoMirrored.Outlined.Label
                )
            }
            island.modelNumber?.let {
                InfoRow(
                    label = stringResource(R.string.island_detail_info_field_model),
                    value = it,
                    icon = Icons.Outlined.Info
                )
            }
            island.location?.let {
                InfoRow(
                    label = stringResource(R.string.island_detail_info_field_location),
                    value = it,
                    icon = Icons.Outlined.LocationOn
                )
            }
            island.installationDate?.let {
                InfoRow(
                    label = stringResource(R.string.island_detail_info_field_installation),
                    value = it.toItalianDate(),
                    icon = Icons.Outlined.CalendarToday
                )
            }
            island.notes?.let {
                InfoRow(
                    label = stringResource(R.string.island_detail_info_field_notes),
                    value = it,
                    icon = Icons.AutoMirrored.Outlined.Notes
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, icon: ImageVector) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}