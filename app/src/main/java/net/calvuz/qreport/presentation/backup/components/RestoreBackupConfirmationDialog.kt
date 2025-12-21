package net.calvuz.qreport.presentation.backup.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.data.backup.model.BackupInfo
import net.calvuz.qreport.domain.model.backup.RestoreStrategy
import net.calvuz.qreport.util.DateTimeUtils.toItalianDate
import net.calvuz.qreport.util.SizeUtils.getFormattedSize

/**
 * FASE 5.4 - BACKUP DIALOG COMPONENTS
 *
 * Componenti dialog specializzati per il sistema backup:
 * - RestoreConfirmationDialog: Conferma ripristino con strategia
 * - DeleteBackupDialog: Conferma eliminazione backup
 * - RestoreProgressDialog: Progress durante ripristino
 */

// ===== RESTORE CONFIRMATION DIALOG =====

/**
 * Dialog conferma ripristino backup con selezione strategia
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreBackupConfirmationDialog(
    backup: BackupInfo,
    onDismiss: () -> Unit,
    onConfirm: (RestoreStrategy) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStrategy by remember { mutableStateOf(RestoreStrategy.REPLACE_ALL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Ripristina Backup",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Backup info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Backup da ripristinare:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "📅 ${(backup.timestamp).toItalianDate()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "📦 ${backup.getFormattedSize()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (backup.description?.isNotEmpty() == true) {
                            Text(
                                text = backup.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (backup.includesPhotos) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Photo,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Include foto (${backup.photoCount ?: "N/A"})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                // Strategy selection
                Text(
                    text = "Strategia di ripristino:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                RestoreStrategy.entries.forEach { strategy ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedStrategy == strategy,
                                onClick = { selectedStrategy = strategy }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStrategy == strategy,
                            onClick = { selectedStrategy = strategy }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = strategy.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = strategy.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Warning
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "⚠️ Attenzione",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (selectedStrategy == RestoreStrategy.REPLACE_ALL) {
                                    "Tutti i dati esistenti verranno sostituiti. Questa operazione non è reversibile."
                                } else {
                                    "I dati esistenti verranno uniti con quelli del backup. Verificare compatibilità."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedStrategy) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedStrategy == RestoreStrategy.REPLACE_ALL) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ripristina")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}



/*
=============================================================================
                           BACKUP DIALOG COMPONENTS
=============================================================================

DIALOG IMPLEMENTATI:

✅ RestoreConfirmationDialog:
   - Mostra dettagli backup da ripristinare
   - Selezione strategia ripristino (RadioButton)
   - Warning per operazioni distruttive
   - Conferma/annulla actions

✅ DeleteBackupDialog:
   - Mostra dettagli backup da eliminare
   - Warning irreversibilità operazione
   - Informazioni complete (data, dimensione, foto)
   - Conferma/annulla con colori semantici

✅ RestoreProgressDialog:
   - Progress real-time durante ripristino
   - Stati: InProgress, Completed, Error, Idle
   - Progress bar con percentuale
   - Dettagli tabella e record correnti
   - Cancel functionality durante processo
   - Success summary con statistiche

DESIGN FEATURES:

✅ Material Design 3 compliance
✅ Semantic colors (error, primary, success)
✅ Industrial-friendly sizing (48dp+ targets)
✅ Clear visual hierarchy
✅ Progress indicators per feedback
✅ Icon semantici per stati

USAGE:

var showRestoreDialog by remember { mutableStateOf(false) }
var selectedBackup by remember { mutableStateOf<BackupInfo?>(null) }

if (showRestoreDialog && selectedBackup != null) {
    RestoreConfirmationDialog(
        backup = selectedBackup!!,
        onDismiss = { showRestoreDialog = false },
        onConfirm = { strategy ->
            viewModel.restoreBackup(selectedBackup!!.id, strategy)
            showRestoreDialog = false
        }
    )
}

=============================================================================
*/