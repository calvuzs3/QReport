package net.calvuz.qreport.backup.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.backup.domain.model.BackupInfo
import net.calvuz.qreport.backup.domain.model.enum.RestoreStrategy
import net.calvuz.qreport.backup.presentation.model.RestoreStrategyExt.getDescription
import net.calvuz.qreport.backup.presentation.model.RestoreStrategyExt.getDisplayName
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate
import net.calvuz.qreport.app.util.SizeUtils.getFormattedSize

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
                    text = stringResource(R.string.backup_restore_confirm_title),
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
                            text = stringResource(R.string.backup_restore_confirm_info_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.backup_restore_confirm_date_format, (backup.createdAt).toItalianDate()),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = stringResource(R.string.backup_restore_confirm_size_format, backup.totalSize.getFormattedSize()),
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
                                    text = stringResource(
                                        R.string.backup_restore_confirm_includes_photos,
                                        backup.photoCount?.toString() ?: stringResource(R.string.backup_restore_confirm_photo_count_na)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                // Strategy selection
                Text(
                    text = stringResource(R.string.backup_restore_confirm_strategy_label),
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
                                text = strategy.getDisplayName().asString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = strategy.getDescription().asString(),
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
                                text = stringResource(R.string.backup_restore_confirm_warning_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (selectedStrategy == RestoreStrategy.REPLACE_ALL) {
                                    stringResource(R.string.backup_restore_confirm_warning_replace_all)
                                } else {
                                    stringResource(R.string.backup_restore_confirm_warning_merge)
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
                Text(stringResource(R.string.backup_restore_confirm_action))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
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