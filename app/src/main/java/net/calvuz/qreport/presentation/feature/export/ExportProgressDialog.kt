package net.calvuz.qreport.presentation.feature.export

import net.calvuz.qreport.R
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.calvuz.qreport.presentation.core.model.UiText

/**
 * Export Progress Dialog
 *
 * Features:
 * - Animated progress with percentage
 * - Actual operation description
 * - Cancel operation button
 * - Remaining time estimation
 */
@Composable
fun ExportProgressDialog(
    progress: ExportProgress,
    onCancel: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = { /* Non permettere dismissal durante export */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.export_dialog_progress_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(onClick = { onCancel }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_cancel)
                        )
                    }
                }

                // Progress indicator
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(120.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress.percentage / 100f },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 8.dp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "${progress.percentage.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Info
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = progress.currentOperation.asString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    if (progress.estimatedTimeRemaining.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.export_dialog_progress_time_label,progress.estimatedTimeRemaining ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Linear progress
                    if (progress.currentStepProgress > 0f) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = progress.currentStepDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            LinearProgressIndicator(
                                progress = { progress.currentStepProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // Export Stats
                ExportStatsRow(
                    processedItems = progress.processedItems,
                    totalItems = progress.totalItems,
                    processedPhotos = progress.processedPhotos,
                    totalPhotos = progress.totalPhotos
                )

                // Cancel button
                OutlinedButton(
                    onClick = { onCancel },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text( stringResource(R.string.action_cancel))
                }
            }
        }
    }
}

@Composable
private fun ExportStatsRow(
    processedItems: Int,
    totalItems: Int,
    processedPhotos: Int,
    totalPhotos: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = stringResource(R.string.export_dialog_progress_stat_items),
                value = "$processedItems/$totalItems"
            )

            if (totalPhotos > 0) {
                StatItem(
                    label = stringResource(R.string.export_dialog_progress_stat_photos),
                    value = "$processedPhotos/$totalPhotos"
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Export progress data class
 */
data class ExportProgress(
    val percentage: Float = 0f,
    val currentOperation: UiText = UiText.StringResource(R.string.export_dialog_progress_state_init),
    val currentStepDescription: String = "",
    val currentStepProgress: Float = 0f,
    val estimatedTimeRemaining: String = "",
    val processedItems: Int = 0,
    val totalItems: Int = 0,
    val processedPhotos: Int = 0,
    val totalPhotos: Int = 0
) {
    companion object {
//        fun initial() = ExportProgress(
//            currentOperation = "Inizializzazione export..."
//        )

        fun initial(msg: UiText) = ExportProgress(currentOperation = msg)

        fun preparing(msg: UiText) = ExportProgress(
            percentage = 10f,
            currentOperation = msg
        )

        fun processing(
            percentage: Float,
            operation: UiText,
            processedItems: Int = 0,
            totalItems: Int = 0,
            processedPhotos: Int = 0,
            totalPhotos: Int = 0
        ) = ExportProgress(
            percentage = percentage,
            currentOperation = operation,
            processedItems = processedItems,
            totalItems = totalItems,
            processedPhotos = processedPhotos,
            totalPhotos = totalPhotos
        )
    }
}