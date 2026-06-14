package net.calvuz.qreport.backup.presentation.ui.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.calvuz.qreport.R
import net.calvuz.qreport.backup.presentation.ui.model.BackupProgress
import net.calvuz.qreport.app.app.presentation.components.DialogItemRow
import net.calvuz.qreport.app.util.SizeUtils.getFormattedSize

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
                                text = stringResource(R.string.backup_progress_in_progress),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                        HorizontalDivider()

                        DialogItemRow(
                            icon = Icons.Default.Restore,
                            label = stringResource(R.string.backup_progress_in_progress),
                            value = progress.step.asString(),
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
                                label = stringResource(R.string.backup_restore_progress_table_label),
                                value = progress.currentTable.asString(),
                                enabled = true
                            )
                            DialogItemRow(
                                icon = Icons.Default.Tab,
                                label = stringResource(R.string.backup_restore_progress_record_label),
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
                                text = stringResource(R.string.backup_progress_completed_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                        HorizontalDivider()

                        // ===== BACKUP SUMMARY =====
                        Text(
                            text = stringResource(R.string.backup_restore_progress_summary_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // ===== BACKUP SUMMARY =====
                        DialogItemRow(
                            icon = Icons.Default.CheckCircle,
                            label = stringResource(R.string.backup_progress_tables_saved_label),
                            value = "${progress.tablesBackedUp}",
                            enabled = true,
                        )
                        if (progress.totalSize > 0) {
                            DialogItemRow(
                                icon = Icons.Default.CheckCircle,
                                label = stringResource(R.string.backup_progress_data_saved_label),
                                value = progress.totalSize.getFormattedSize(),
                                enabled = true
                            )
                        }
                        DialogItemRow(
                            icon = Icons.Default.CheckCircle,
                            label = stringResource(R.string.backup_progress_time_elapsed_label),
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
                                text = stringResource(R.string.backup_progress_error_title),
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
                            value = progress.message.asString(),
                            enabled = true
                        )
                    }


                    is BackupProgress.Idle -> {
                        // Stato idle - shouldn't be the case
                        Text(stringResource(R.string.backup_restore_progress_idle))
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
                            Text(stringResource(R.string.action_cancel))
                        }
                    }

                    is BackupProgress.Completed,
                    is BackupProgress.Error -> {
                        // press OK to close
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Text(stringResource(R.string.action_ok))
                        }
                    }

                    else -> {
                        // Idle state
                        Button(onClick = onDismiss) {
                            Text(stringResource(R.string.action_close))
                        }
                    }
                }
            }
        }
    }
}