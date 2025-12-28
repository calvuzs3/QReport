package net.calvuz.qreport.presentation.feature.export

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Dialog per mostrare il progresso dell'export
 *
 * Features:
 * - Progresso animato con percentuale
 * - Descrizione dell'operazione in corso
 * - Bottone per annullare l'export
 * - Tempo stimato rimanente
 */
@Composable
fun ExportProgressDialog(
    progress: ExportProgress,
    onCancel: () -> Unit,
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
                // Header con titolo e bottone chiudi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Export in Corso",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Annulla export"
                        )
                    }
                }

                // Indicatore progresso circolare con percentuale
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(120.dp)
                ) {
                    CircularProgressIndicator(
                        progress = progress.percentage / 100f,
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

                // Informazioni dettagliate
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = progress.currentOperation,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    if (progress.estimatedTimeRemaining.isNotBlank()) {
                        Text(
                            text = "Tempo stimato: ${progress.estimatedTimeRemaining}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Progress lineare per step corrente
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
                                progress = progress.currentStepProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // Statistiche export
                ExportStatsRow(
                    processedItems = progress.processedItems,
                    totalItems = progress.totalItems,
                    processedPhotos = progress.processedPhotos,
                    totalPhotos = progress.totalPhotos
                )

                // Bottone annulla
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Annulla Export")
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
                label = "Items",
                value = "$processedItems/$totalItems"
            )

            if (totalPhotos > 0) {
                StatItem(
                    label = "Foto",
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
 * Data class per il progresso dell'export
 */
data class ExportProgress(
    val percentage: Float = 0f,
    val currentOperation: String = "",
    val currentStepDescription: String = "",
    val currentStepProgress: Float = 0f,
    val estimatedTimeRemaining: String = "",
    val processedItems: Int = 0,
    val totalItems: Int = 0,
    val processedPhotos: Int = 0,
    val totalPhotos: Int = 0
) {
    companion object {
        fun initial() = ExportProgress(
            currentOperation = "Inizializzazione export..."
        )

        fun preparing() = ExportProgress(
            percentage = 10f,
            currentOperation = "Preparazione dati..."
        )

        fun processing(
            percentage: Float,
            operation: String,
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