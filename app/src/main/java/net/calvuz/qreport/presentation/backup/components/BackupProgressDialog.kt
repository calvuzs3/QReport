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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.calvuz.qreport.domain.model.backup.BackupProgress
import net.calvuz.qreport.presentation.components.DialogItemRow
import net.calvuz.qreport.util.SizeUtils.getFormattedSize

/**
 * Backup progress Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupProgressDialog(
    modifier: Modifier = Modifier,
    progress: BackupProgress,
    onCancel: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    val isInProgress = progress is BackupProgress.InProgress

    Dialog(
        onDismissRequest = if (isInProgress) {
            {}
        } else onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = !isInProgress,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (progress) {
                    is BackupProgress.InProgress -> {
                        // ===== HEADER =====
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Progress in corso
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "Backup in corso...",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                        HorizontalDivider()

                        // TODO verificare se --> .message
                        DialogItemRow(
                            icon = Icons.Default.Restore,
                            label = "Backup in corso...",
                            value = progress.step,
                            enabled = true
                        )

                        LinearProgressIndicator(
                            progress = { progress.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round,
                        )

                        if (progress.currentTable != null) {
                            DialogItemRow(
                                icon = Icons.Default.Tab,
                                label = "Tabella",
                                value = progress.currentTable,
                                enabled = true
                            )
                            DialogItemRow(
                                icon = Icons.Default.Tab,
                                label = "Record",
                                value ="${progress.processedRecords}/${progress.totalRecords}",
                                enabled = true
                            )
                        }
                    }

                    is BackupProgress.Completed -> {
                        // ===== HEADER =====
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Backup completato
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "Backup Completato",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                        HorizontalDivider()

                        // ===== BACKUP SUMMARY =====
                        Text(
                            text = "Riepilogo azioni",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // ===== BACKUP SUMMARY =====
                        DialogItemRow(
                            icon = Icons.Default.CheckCircle,
                            label = "Tabelle salvate",
                            value = "${progress.tablesBackedUp}",
                            enabled = true,
                        )
                        if (progress.totalSize > 0) {
                            DialogItemRow(
                                icon = Icons.Default.CheckCircle,
                                label = "Dati salvati",
                                value = progress.totalSize.getFormattedSize(),
                                enabled = true
                            )
                        }
                        DialogItemRow(
                            icon = Icons.Default.CheckCircle,
                            label = "Tempo impiegato",
                            value = "${(progress.duration / 1000)}",
                            enabled = true,
                        )

                    }

                    is BackupProgress.Error -> {
                        // ===== HEADER =====
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )

                            Text(
                                text = "Errore Backup",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        HorizontalDivider()

                        DialogItemRow(
                            icon = Icons.Default.Error,
                            label = null,
                            value = progress.message,
                            enabled = true
                        )
                    }


                    is BackupProgress.Idle -> {
                        // Stato idle - shouldn't be the case
                        Text("Nessuna operazione in corso")
                    }
                }


            }

            // ===== BUTTONS =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (progress) {
                    is BackupProgress.InProgress -> {
                        // press CANCEL during process
                        TextButton(onClick = onCancel) {
                            Text("Annulla")
                        }
                    }

                    is BackupProgress.Completed,
                    is BackupProgress.Error -> {
                        // press OK to close
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Text("OK")
                        }
                    }

                    else -> {
                        // Idle state
                        Button(onClick = onDismiss) {
                            Text("Chiudi")
                        }
                    }
                }
            }
        }
    }
}