package net.calvuz.qreport.presentation.feature.export

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.calvuz.qreport.domain.model.export.ExportResult
import net.calvuz.qreport.domain.model.export.ExportFormat
import net.calvuz.qreport.domain.model.export.ExportErrorCode

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
            text = "Export Completato!",
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
                        Text("Apri")
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
                        Text("Condividi")
                    }
                }
            }

            // Close button
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Chiudi")
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
            text = "Export Fallito",
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
                    text = "Errore:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )

                Text(
                    text = result.exception.message ?: "Errore sconosciuto",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Codice: ${result.errorCode}",
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
            Text("Chiudi")
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
            text = "Export in corso...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Chiudi")
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
                    text = "File Esportato",
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
                    text = "Dimensione: ${formatFileSize(fileSize)}",
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
        ExportErrorCode.INSUFFICIENT_STORAGE ->
            listOf(
                "Liberare spazio di archiviazione",
                "Ridurre la qualità delle foto",
                "Escludere le foto dall'export"
            )
        ExportErrorCode.PERMISSION_DENIED ->
            listOf(
                "Verificare i permessi di scrittura",
                "Riavviare l'app",
                "Cambiare directory di destinazione"
            )
        ExportErrorCode.TEMPLATE_NOT_FOUND ->
            listOf(
                "Verificare i template disponibili",
                "Usare il template predefinito",
                "Reinstallare l'app"
            )
        ExportErrorCode.DOCUMENT_GENERATION_ERROR ->
            listOf(
                "Riprovare l'operazione",
                "Verificare i dati del checkup",
                "Riavviare l'app"
            )
        ExportErrorCode.IMAGE_PROCESSING_ERROR ->
            listOf(
                "Verificare che le foto siano valide",
                "Ridurre la qualità delle foto",
                "Escludere le foto problematiche"
            )
        ExportErrorCode.PHOTO_FOLDER_ERROR ->
            listOf(
                "Verificare i permessi di scrittura",
                "Liberare spazio di archiviazione",
                "Cambiare directory di destinazione"
            )
        ExportErrorCode.TEXT_GENERATION_ERROR ->
            listOf(
                "Verificare i dati del checkup",
                "Riprovare l'operazione",
                "Contattare il supporto"
            )
        ExportErrorCode.INVALID_DATA ->
            listOf(
                "Completare tutti i campi richiesti",
                "Verificare i dati inseriti",
                "Riavviare l'app"
            )
        ExportErrorCode.PROCESSING_TIMEOUT ->
            listOf(
                "Ridurre il numero di foto",
                "Riprovare più tardi",
                "Riavviare l'app"
            )
        ExportErrorCode.NETWORK_ERROR ->
            listOf(
                "Verificare la connessione internet",
                "Riprovare più tardi",
                "Usare export offline"
            )
        ExportErrorCode.SYSTEM_ERROR ->
            listOf(
                "Riavviare l'app",
                "Riavviare il dispositivo",
                "Contattare il supporto"
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
                text = "Suggerimenti:",
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
    val (text, containerColor) = when (format) {
        ExportFormat.WORD -> "WORD" to Color(0xFF2E7D32)
        ExportFormat.TEXT -> "TEXT" to Color(0xFF1976D2)
        ExportFormat.PHOTO_FOLDER -> "FOTO" to Color(0xFFE65100)
        ExportFormat.COMBINED_PACKAGE -> "PACK" to Color(0xFF6A1B9A)
    }

    AssistChip(
        onClick = { },
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor.copy(alpha = 0.1f),
            labelColor = containerColor
        ),
        modifier = modifier
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${String.format("%.1f", bytes / 1024.0)}KB"
        bytes < 1024 * 1024 * 1024 -> "${String.format("%.1f", bytes / (1024.0 * 1024.0))}MB"
        else -> "${String.format("%.1f", bytes / (1024.0 * 1024.0 * 1024.0))}GB"
    }
}