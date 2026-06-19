package net.calvuz.qreport.client.island.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Single source of icon mapping for island types, keyed by [label][net.calvuz.qreport.client.island.domain.model.IslandTypeMaster.label]
 * rather than by code — this decouples the mapping from the legacy
 * IslandType enum's codes, so that
 * enum can be removed once the migration to [net.calvuz.qreport.client.island.domain.model.IslandTypeMaster]
 * is complete.
 *
 * Replaces the two duplicate enum-keyed icon files that existed before. Unknown
 * labels (custom types without a recognized label) fall back to [Icons.Default.Category].
 */
object IslandTypeIconRegistry {

    private val byLabel: Map<String, ImageVector> = mapOf(
        "POLY Move" to Icons.Default.DirectionsCar,
        "POLY Cast" to Icons.Default.Build,
        "POLY EBT" to Icons.Default.ElectricBolt,
        "POLY Tag BLE" to Icons.AutoMirrored.Default.Label,
        "POLY Tag FC" to Icons.Default.QrCode,
        "POLY Tag V" to Icons.Default.CameraAlt,
        "POLY Sample" to Icons.Default.Science,
        "POLY Weld" to Icons.Default.Hardware,
        "POLY Paint" to Icons.Default.FormatPaint,
        "Altro" to Icons.Default.Category
    )

    fun iconFor(label: String): ImageVector = byLabel[label] ?: Icons.Default.Category
}
