package net.calvuz.qreport.client.island.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import net.calvuz.qreport.client.island.domain.model.IslandType

/**
 * Maps [IslandType] to a Material icon for use in the presentation layer.
 *
 * Kept in the presentation layer so the domain model stays free
 * of Compose dependencies.
 */
fun IslandType.icon(): ImageVector = when (this) {
    IslandType.POLY_MOVE -> Icons.Default.DirectionsCar
    IslandType.POLY_CAST -> Icons.Default.Build
    IslandType.POLY_EBT -> Icons.Default.ElectricBolt
    IslandType.POLY_TAG_BLE -> Icons.AutoMirrored.Default.Label
    IslandType.POLY_TAG_FC -> Icons.Default.QrCode
    IslandType.POLY_TAG_V -> Icons.Default.CameraAlt
    IslandType.POLY_SAMPLE -> Icons.Default.Science
}

