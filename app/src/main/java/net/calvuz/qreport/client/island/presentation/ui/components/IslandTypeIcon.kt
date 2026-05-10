package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.client.island.domain.model.IslandType

@Composable
fun IslandTypeIcon(islandType: IslandType) {
    val icon = when (islandType) {
        IslandType.POLY_MOVE -> Icons.Default.OpenWith
        IslandType.POLY_CAST -> Icons.Default.Opacity
        IslandType.POLY_EBT -> Icons.Default.ElectricalServices
        IslandType.POLY_TAG_BLE -> Icons.Default.Bluetooth
        IslandType.POLY_TAG_FC -> Icons.Default.TextFields
        IslandType.POLY_TAG_V -> Icons.Default.Visibility
        IslandType.POLY_SAMPLE -> Icons.Default.Science
    }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Icon(
            imageVector = icon,
            contentDescription = islandType.displayName,
            modifier = Modifier
                .padding(8.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}