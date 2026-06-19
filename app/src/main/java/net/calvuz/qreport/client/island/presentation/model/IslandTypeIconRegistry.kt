package net.calvuz.qreport.client.island.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Single source of icon mapping for island types, keyed by [code][net.calvuz.qreport.client.island.domain.model.IslandTypeMaster.code]
 * (same values as the legacy [net.calvuz.qreport.client.island.domain.model.IslandType.code]).
 *
 * Replaces the two duplicate enum-keyed icon files that existed before. Unknown
 * codes (custom types without a recognized code) fall back to [Icons.Default.Category].
 */
object IslandTypeIconRegistry {

    private val byCode: Map<String, ImageVector> = mapOf(
        "MOVE" to Icons.Default.DirectionsCar,
        "CAST" to Icons.Default.Build,
        "EBT" to Icons.Default.ElectricBolt,
        "TAG_BLE" to Icons.AutoMirrored.Default.Label,
        "TAG_FC" to Icons.Default.QrCode,
        "TAG_V" to Icons.Default.CameraAlt,
        "SAMPLE" to Icons.Default.Science,
        "WELD" to Icons.Default.Hardware,
        "PAINT" to Icons.Default.FormatPaint,
        "OTHER" to Icons.Default.Category
    )

    fun iconFor(code: String): ImageVector = byCode[code] ?: Icons.Default.Category
}
