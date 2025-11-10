@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.presentation.screen.export

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.domain.model.export.CompressionLevel
import net.calvuz.qreport.domain.model.export.ExportFormat

/**
 * Screen per la configurazione delle opzioni di export
 *
 * Permette di scegliere:
 * - Formato di export (Word, Text, Foto, Package Completo)
 * - Qualità delle foto (Bassa/Media/Alta)
 * - Inclusione di foto e note
 * - Directory di destinazione
 */
@Composable
fun ExportOptionsScreen(
    checkUpId: String,
    onNavigateBack: () -> Unit,
    onExportStarted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExportOptionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Initialize with checkUpId
    LaunchedEffect(checkUpId) {
        viewModel.initialize(checkUpId)
    }

    // Navigate back on export completion
    LaunchedEffect(uiState.exportCompleted) {
        if (uiState.exportCompleted) {
            onExportStarted()
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text("Opzioni Export")
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Indietro"
                    )
                }
            }
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                ErrorContent(
                    error = uiState.error!!,
                    onRetry = { viewModel.initialize(checkUpId) }
                )
            }

            else -> {
                ExportOptionsContent(
                    uiState = uiState,
                    onFormatChange = viewModel::setExportFormat,
                    onCompressionChange = viewModel::setCompressionLevel,
                    onIncludePhotosChange = viewModel::setIncludePhotos,
                    onIncludeNotesChange = viewModel::setIncludeNotes,
                    onExportStart = viewModel::startExport,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Show progress dialog during export
    if (uiState.isExporting) {
        ExportProgressDialog(
            progress = uiState.exportProgress,
            onCancel = viewModel::cancelExport
        )
    }

    // Show result dialog on completion
    if (uiState.showResultDialog) {
        ExportResultDialog(
            result = uiState.exportResult,
            onDismiss = viewModel::dismissResult,
            onOpenFile = viewModel::openExportedFile,
            onShareFile = viewModel::shareExportedFile
        )
    }
}

@Composable
private fun ExportOptionsContent(
    uiState: ExportOptionsUiState,
    onFormatChange: (ExportFormat) -> Unit,
    onCompressionChange: (CompressionLevel) -> Unit,
    onIncludePhotosChange: (Boolean) -> Unit,
    onIncludeNotesChange: (Boolean) -> Unit,
    onExportStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Preview info
        CheckUpPreviewCard(
            checkUpName = uiState.checkUpName,
            itemCount = uiState.totalItems,
            photoCount = uiState.totalPhotos,
            estimatedSize = uiState.estimatedSize
        )

        // Export Format Selection
        ExportFormatSection(
            selectedFormat = uiState.exportFormat,
            onFormatChange = onFormatChange
        )

        // Photo Options (only if format supports photos)
        AnimatedVisibility(
            visible = uiState.exportFormat.supportsPhotos,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            PhotoOptionsSection(
                includePhotos = uiState.includePhotos,
                compressionLevel = uiState.compressionLevel,
                onIncludePhotosChange = onIncludePhotosChange,
                onCompressionChange = onCompressionChange
            )
        }

        // Additional Options
        AdditionalOptionsSection(
            includeNotes = uiState.includeNotes,
            onIncludeNotesChange = onIncludeNotesChange
        )

        // Export Button
        Button(
            onClick = onExportStart,
            enabled = uiState.canExport && !uiState.isExporting,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null
                )
                Text(
                    text = "Avvia Export",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun CheckUpPreviewCard(
    checkUpName: String,
    itemCount: Int,
    photoCount: Int,
    estimatedSize: String,
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
            Text(
                text = "Anteprima Export",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = checkUpName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(
                    icon = Icons.Default.Assignment,
                    text = "$itemCount items"
                )
                InfoChip(
                    icon = Icons.Default.Photo,
                    text = "$photoCount foto"
                )
                InfoChip(
                    icon = Icons.Default.Storage,
                    text = estimatedSize
                )
            }
        }
    }
}

@Composable
private fun ExportFormatSection(
    selectedFormat: ExportFormat,
    onFormatChange: (ExportFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Formato Export",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExportFormat.values().forEach { format ->
                FormatOptionCard(
                    format = format,
                    isSelected = selectedFormat == format,
                    onClick = { onFormatChange(format) }
                )
            }
        }
    }
}

@Composable
private fun FormatOptionCard(
    format: ExportFormat,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            )
        } else {
            CardDefaults.outlinedCardBorder()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = format.icon,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = format.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )

                Text(
                    text = format.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = null // gestito dal selectable della Card
            )
        }
    }
}

@Composable
private fun PhotoOptionsSection(
    includePhotos: Boolean,
    compressionLevel: CompressionLevel,
    onIncludePhotosChange: (Boolean) -> Unit,
    onCompressionChange: (CompressionLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Opzioni Foto",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        // Include photos toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Includi Foto",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Inserisci le foto nel documento",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = includePhotos,
                    onCheckedChange = onIncludePhotosChange
                )
            }
        }

        // Compression level (only if photos included)
        AnimatedVisibility(
            visible = includePhotos,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            CompressionLevelSection(
                selectedLevel = compressionLevel,
                onLevelChange = onCompressionChange
            )
        }
    }
}

@Composable
private fun CompressionLevelSection(
    selectedLevel: CompressionLevel,
    onLevelChange: (CompressionLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Qualità Foto",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompressionLevel.values().forEach { level ->
                CompressionChip(
                    level = level,
                    isSelected = selectedLevel == level,
                    onClick = { onLevelChange(level) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CompressionChip(
    level: CompressionLevel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        onClick = onClick,
        label = {
            Text(
                text = level.displayName,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        selected = isSelected,
        modifier = modifier
    )
}

@Composable
private fun AdditionalOptionsSection(
    includeNotes: Boolean,
    onIncludeNotesChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Opzioni Aggiuntive",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Includi Note",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Inserisci note e commenti tecnici",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = includeNotes,
                    onCheckedChange = onIncludeNotesChange
                )
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = { },
        label = { Text(text) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        modifier = modifier
    )
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )

            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            Button(onClick = onRetry) {
                Text("Riprova")
            }
        }
    }
}

// Extension properties for ExportFormat
private val ExportFormat.icon: ImageVector
    get() = when (this) {
        ExportFormat.WORD -> Icons.Default.Description
        ExportFormat.TEXT -> Icons.Default.TextSnippet
        ExportFormat.PHOTO_FOLDER -> Icons.Default.PhotoLibrary
        ExportFormat.COMBINED_PACKAGE -> Icons.Default.Archive
    }

private val ExportFormat.displayName: String
    get() = when (this) {
        ExportFormat.WORD -> "Documento Word"
        ExportFormat.TEXT -> "Report Testuale"
        ExportFormat.PHOTO_FOLDER -> "Cartella Foto"
        ExportFormat.COMBINED_PACKAGE -> "Package Completo"
    }

private val ExportFormat.description: String
    get() = when (this) {
        ExportFormat.WORD -> "Documento professionale con foto integrate"
        ExportFormat.TEXT -> "Report testuale universalmente leggibile"
        ExportFormat.PHOTO_FOLDER -> "Foto organizzate per sezione"
        ExportFormat.COMBINED_PACKAGE -> "Tutti i formati insieme"
    }

private val ExportFormat.supportsPhotos: Boolean
    get() = when (this) {
        ExportFormat.WORD, ExportFormat.PHOTO_FOLDER, ExportFormat.COMBINED_PACKAGE -> true
        ExportFormat.TEXT -> false
    }

private val CompressionLevel.displayName: String
    get() = when (this) {
        CompressionLevel.LOW -> "Alta"
        CompressionLevel.MEDIUM -> "Media"
        CompressionLevel.HIGH -> "Bassa"
    }