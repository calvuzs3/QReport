package net.calvuz.qreport.domain.model.island

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

/**
 * Tipi di isola robotizzata supportati - FAMIGLIA POLY
 */
@Serializable
enum class IslandType(
    val displayName: String,
    val code: String,
    val description: String,
    val icon: ImageVector
) {
    POLY_MOVE(
        displayName = "POLY Move",
        code = "MOVE",
        description = "Sistema robotizzato per movimentazione automatizzata",
        icon = Icons.Default.DirectionsCar
    ),
    POLY_CAST(
        displayName = "POLY Cast",
        code = "CAST",
        description = "Sistema robotizzato per casting e lavorazioni di precisione",
        icon = Icons.Default.Build
    ),
    POLY_EBT(
        displayName = "POLY EBT",
        code = "EBT",
        description = "Sistema robotizzato con tecnologia EBT e gestione lance",
        icon = Icons.Default.ElectricBolt
    ),
    POLY_TAG_BLE(
        displayName = "POLY Tag BLE",
        code = "TAG_BLE",
        description = "Sistema robotizzato per etichettatura con tecnologia BLE",
        icon = Icons.Default.Label
    ),
    POLY_TAG_FC(
        displayName = "POLY Tag FC",
        code = "TAG_FC",
        description = "Sistema robotizzato per etichettatura FC con QR Code",
        icon = Icons.Default.QrCode
    ),
    POLY_TAG_V(
        displayName = "POLY Tag V",
        code = "TAG_V",
        description = "Sistema robotizzato per etichettatura con visione artificiale",
        icon = Icons.Default.CameraAlt
    ),
    POLY_SAMPLE(
        displayName = "POLY Sample",
        code = "SAMPLE",
        description = "Sistema robotizzato per campionamento e analisi",
        icon = Icons.Default.Science
    )
}