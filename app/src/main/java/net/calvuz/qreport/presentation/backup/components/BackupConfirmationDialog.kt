package net.calvuz.qreport.presentation.backup.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.calvuz.qreport.domain.model.backup.BackupMode
import net.calvuz.qreport.presentation.components.DialogItemRow
import net.calvuz.qreport.util.SizeUtils.getFormattedSize

// ✅ AGGIUNGI: Data class per opzioni backup
data class BackupOptions(
    val includePhotos: Boolean,
    val includeThumbnails: Boolean,
    val backupMode: BackupMode,
    val description: String,
    val estimatedSize: Long
)

@Composable
fun BackupConfirmationDialog(
    backupOptions: BackupOptions,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ===== HEADER =====
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Conferma Backup",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider()

                // ===== BACKUP SUMMARY =====
                Text(
                    text = "Riepilogo Opzioni",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Opzioni foto
                DialogItemRow(
                    icon = Icons.Default.Photo,
                    label = "Foto",
                    value = if (backupOptions.includePhotos) "Incluse" else "Escluse",
                    enabled = backupOptions.includePhotos
                )

                // Opzioni thumbnails
                DialogItemRow(
                    icon = Icons.Default.PhotoSizeSelectLarge,
                    label = "Miniature",
                    value = if (backupOptions.includeThumbnails && backupOptions.includePhotos) "Incluse" else "Escluse",
                    enabled = backupOptions.includeThumbnails && backupOptions.includePhotos
                )

                // Modalità backup
                DialogItemRow(
                    icon = backupOptions.backupMode.icon,
                    label = "Modalità",
                    value = backupOptions.backupMode.displayName               ,
                    enabled = true
                )

                // Dimensione stimata
                DialogItemRow(
                    icon = Icons.Default.DataUsage,
                    label = "Stima Dimensione",
                    value = backupOptions.estimatedSize.getFormattedSize(),
                    enabled = true
                )

                // Descrizione se presente
                if (backupOptions.description.isNotEmpty()) {
                    DialogItemRow(
                        icon = Icons.Default.Description,
                        label = "Descrizione",
                        value = backupOptions.description,
                        enabled = true
                    )
                }

                HorizontalDivider()

                // ===== WARNING/INFO =====
//                if (backupOptions.includePhotos) {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Info,
//                            contentDescription = null,
//                            tint = MaterialTheme.colorScheme.primary,
//                            modifier = Modifier.size(16.dp)
//                        )
//                        Text(
//                            text = "Il backup includerà tutte le foto salvate",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
//                        )
//                    }
//                }

                // ===== BUTTONS =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annulla")
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Backup")
                    }
                }
            }
        }
    }
}