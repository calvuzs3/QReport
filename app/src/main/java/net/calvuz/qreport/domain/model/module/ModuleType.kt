package net.calvuz.qreport.domain.model.module

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

/**
 * Tipi di modulo check-up - CON ICONE E DESCRIZIONI
 */
@Serializable
enum class ModuleType(
    val displayName: String,
    val description: String,
    val icon: ImageVector
) {
    // Moduli base comuni
    SAFETY(
        displayName = "Sicurezza",
        description = "Controlli di sicurezza e protezioni",
        icon = Icons.Default.Security
    ),
    MECHANICAL(
        displayName = "Meccanico",
        description = "Componenti meccanici e strutturali",
        icon = Icons.Default.Settings
    ),
    ELECTRICAL(
        displayName = "Elettrico",
        description = "Sistemi elettrici e cablaggio",
        icon = Icons.Default.ElectricBolt
    ),
    PNEUMATIC(
        displayName = "Pneumatico",
        description = "Sistemi pneumatici e aria compressa",
        icon = Icons.Default.Air
    ),
    SOFTWARE(
        displayName = "Software",
        description = "Sistemi software e programmazione",
        icon = Icons.Default.Code
    ),

    // Moduli specifici robot
    ROBOT_TOOL(
        displayName = "Tool/Pinza Robot",
        description = "Utensili e pinze robotiche",
        icon = Icons.Default.Build
    ),
    ROBOT(
        displayName = "Controllo Robot",
        description = "Sistema robotico principale",
        icon = Icons.Default.SmartToy
    ),
    PLANT_SYSTEMS(
        displayName = "Impianti Isola",
        description = "Sistemi ausiliari dell'isola",
        icon = Icons.Default.Factory
    ),
    FUNCTIONAL_TESTS(
        displayName = "Prove Funzionamento",
        description = "Test funzionali e operativi",
        icon = Icons.Default.PlayArrow
    ),

    // Moduli trasporto
    CONVEYOR_SYSTEMS(
        displayName = "Sistemi Rulliere",
        description = "Sistemi di trasporto e movimentazione",
        icon = Icons.Default.DirectionsCar
    ),

    // Moduli visione
    VISION_SYSTEM(
        displayName = "Sistema Visione",
        description = "Sistemi di visione artificiale",
        icon = Icons.Default.CameraAlt
    ),

    // Moduli storage
    LANCE_STORAGE(
        displayName = "Lancia e Magazzini",
        description = "Sistemi di storage e lance",
        icon = Icons.Default.Science
    ),
    CARTRIDGE_SYSTEMS(
        displayName = "Sistemi Cartucce",
        description = "Sistemi di gestione cartucce",
        icon = Icons.Default.Inventory
    ),

    // Moduli etichettatura
    LABELING_MACHINE(
        displayName = "Macchina Etichettatura",
        description = "Sistemi di etichettatura automatica",
        icon = Icons.AutoMirrored.Default.Label
    ),

    // Moduli vibratori
    VIBRATORS(
        displayName = "Vibratori",
        description = "Sistemi vibratori e alimentazione",
        icon = Icons.Default.Vibration
    ),

    // Moduli robot duali
    DUAL_ROBOT(
        displayName = "Robot Duali",
        description = "Sistemi con robot multipli",
        icon = Icons.Default.Group
    )
}