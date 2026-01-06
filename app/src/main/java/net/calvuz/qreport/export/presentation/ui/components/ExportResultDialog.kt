package net.calvuz.qreport.export.presentation.ui.components

import net.calvuz.qreport.R
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.calvuz.qreport.export.domain.model.ExportErrorCode
import net.calvuz.qreport.export.presentation.model.color
import net.calvuz.qreport.export.presentation.model.getDisplayName
import net.calvuz.qreport.app.util.SizeUtils.getFormattedSize
import net.calvuz.qreport.export.domain.reposirory.ExportFormat
import net.calvuz.qreport.export.domain.reposirory.ExportResult

/**
 * Dialog per mostrare il risultato dell'export
 *
 * Features:
 * - Mostra successo o errore
 * - Dettagli del file esportato
 * - Bottoni per aprire o condividere il file
 */
@Composable
fun ExportResultDialog(
    result: ExportResult?,
    onDismiss: () -> Unit,
    onOpenFile: () -> Unit,
    onShareFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (result == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
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
            when (result) {
                is ExportResult.Success -> {
                    SuccessContent(
                        result = result,
                        onDismiss = onDismiss,
                        onOpenFile = onOpenFile,
                        onShareFile = onShareFile
                    )
                }
                is ExportResult.Error -> {
                    ErrorContent(
                        result = result,
                        onDismiss = onDismiss
                    )
                }
                is ExportResult.Loading -> {
                    LoadingContent(onDismiss = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    result: ExportResult.Success,
    onDismiss: () -> Unit,
    onOpenFile: () -> Unit,
    onShareFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success icon
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(64.dp)
        )

        // Title
        Text(
            text = stringResource(R.string.export_dialog_result_success_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        // File details
        FileDetailsCard(
            fileName = result.fileName,
            filePath = result.filePath,
            fileSize = result.fileSize,
            format = result.format
        )

        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Primary actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Open file button
                Button(
                    onClick = onOpenFile,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(stringResource(R.string.action_open))
                    }
                }

                // Share button
                OutlinedButton(
                    onClick = onShareFile,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(stringResource(R.string.action_share))
                    }
                }
            }

            // Close button
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_close))
            }
        }
    }
}

@Composable
private fun ErrorContent(
    result: ExportResult.Error,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Error icon
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        // Title
        Text(
            text = stringResource(R.string.export_dialog_result_error_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        // Error details
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.export_dialog_result_error_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )

                Text(
                    text = result.exception.message ?: stringResource(R.string.export_dialog_result_error_unknown),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.export_dialog_result_error_code, result.errorCode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Error suggestions
        ErrorSuggestionsCard(errorCode = result.errorCode)

        // Close button
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(stringResource(R.string.action_close))
        }
    }
}

@Composable
private fun LoadingContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()

        Text(
            text =stringResource(R.string.export_dialog_result_loading_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_close))
        }
    }
}

@Composable
private fun FileDetailsCard(
    fileName: String,
    filePath: String,
    fileSize: Long,
    format: ExportFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.export_dialog_result_file_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                FormatBadge(format = format)
            }

            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.export_dialog_result_file_size, fileSize.getFormattedSize()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = filePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ErrorSuggestionsCard(
    errorCode: ExportErrorCode,
    modifier: Modifier = Modifier
) {
    val suggestions = when (errorCode) {
        ExportErrorCode.INSUFFICIENT_STORAGE -> listOf(
            stringResource(R.string.export_error_suggestion_storage_1),
            stringResource(R.string.export_error_suggestion_storage_2),
            stringResource(R.string.export_error_suggestion_storage_3)
        )
        ExportErrorCode.PERMISSION_DENIED -> listOf(
            stringResource(R.string.export_error_suggestion_permission_1),
            stringResource(R.string.export_error_suggestion_permission_2),
            stringResource(R.string.export_error_suggestion_permission_3)
        )
        ExportErrorCode.TEMPLATE_NOT_FOUND -> listOf(
            stringResource(R.string.export_error_suggestion_template_1),
            stringResource(R.string.export_error_suggestion_template_2),
            stringResource(R.string.export_error_suggestion_template_3)
        )
        ExportErrorCode.DOCUMENT_GENERATION_ERROR -> listOf(
            stringResource(R.string.export_error_suggestion_docgen_1),
            stringResource(R.string.export_error_suggestion_docgen_2),
            stringResource(R.string.export_error_suggestion_docgen_3)
        )
        ExportErrorCode.IMAGE_PROCESSING_ERROR -> listOf(
            stringResource(R.string.export_error_suggestion_image_1),
            stringResource(R.string.export_error_suggestion_image_2),
            stringResource(R.string.export_error_suggestion_image_3)
        )
        ExportErrorCode.PHOTO_FOLDER_ERROR -> listOf(
            stringResource(R.string.export_error_suggestion_photofolder_1),
            stringResource(R.string.export_error_suggestion_photofolder_2),
            stringResource(R.string.export_error_suggestion_photofolder_3)
        )
        ExportErrorCode.TEXT_GENERATION_ERROR -> listOf(
            stringResource(R.string.export_error_suggestion_textgen_1),
            stringResource(R.string.export_error_suggestion_textgen_2),
            stringResource(R.string.export_error_suggestion_textgen_3)
        )
        ExportErrorCode.INVALID_DATA -> listOf(
            stringResource(R.string.export_error_suggestion_invaliddata_1),
            stringResource(R.string.export_error_suggestion_invaliddata_2),
            stringResource(R.string.export_error_suggestion_invaliddata_3)
        )
        ExportErrorCode.PROCESSING_TIMEOUT -> listOf(
            stringResource(R.string.export_error_suggestion_timeout_1),
            stringResource(R.string.export_error_suggestion_timeout_2),
            stringResource(R.string.export_error_suggestion_timeout_3)
        )
        ExportErrorCode.NETWORK_ERROR -> listOf(
            stringResource(R.string.export_error_suggestion_network_1),
            stringResource(R.string.export_error_suggestion_network_2),
            stringResource(R.string.export_error_suggestion_network_3)
        )
        ExportErrorCode.SYSTEM_ERROR -> listOf(
            stringResource(R.string.export_error_suggestion_system_1),
            stringResource(R.string.export_error_suggestion_system_2),
            stringResource(R.string.export_error_suggestion_system_3)
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.export_dialog_result_suggestions_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            suggestions.forEach { suggestion ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatBadge(
    format: ExportFormat,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = { },
        label = { Text(format.getDisplayName()) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = format.color.copy(alpha = 0.1f),
            labelColor = format.color
        ),
        modifier = modifier
    )
}