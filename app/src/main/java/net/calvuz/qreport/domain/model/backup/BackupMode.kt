package net.calvuz.qreport.domain.model.backup

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsCell
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * BackupMode - Modalità di backup
 */
enum class BackupMode (
    val displayName: String,
    val description: String,
    val icon: ImageVector
){
    LOCAL("Locale", "Salva solo su dispositivo", Icons.Default.Storage),            // Locale
//    CLOUD("Cloud", "Carica su cloud", Icons.Default.Cloud),                         // Cloud (future)
//    BOTH("Entrambi", "Salva locale e carica su cloud", Icons.Default.CloudSync)     // Entrambi (future)
}