@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.export.presentation.ui

import net.calvuz.qreport.R
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.error.presentation.asUiText
import net.calvuz.qreport.export.domain.reposirory.ExportFormat
import net.calvuz.qreport.export.domain.reposirory.PhotoQuality
import net.calvuz.qreport.export.presentation.model.PhotoQualityExt.getDisplayName
import net.calvuz.qreport.export.presentation.model.getDescription
import net.calvuz.qreport.export.presentation.model.getDisplayName
import net.calvuz.qreport.export.presentation.model.icon
import net.calvuz.qreport.export.presentation.model.supportsPhotos
import net.calvuz.qreport.export.presentation.ui.components.ExportProgressDialog
import net.calvuz.qreport.export.presentation.ui.components.ExportResultDialog

/**
 * Export Options Screen
 *
 * Choose:
 * - Export format (Word, Text, Foto, Package Completo)
 * - Photo quality (Bassa/Media/Alta)
 * - Notes and photo inclusion
 * - Destination Dir
 */
@Suppress("ParamsComparedByRef")
@Composable
fun ExportOptionsScreen(
    checkUpId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExportOptionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Initialize with checkUpId
    LaunchedEffect(checkUpId) {
        viewModel.initialize(checkUpId)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(stringResource(R.string.export_screen_title))
            },
            
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            }
        )

        val currentError = uiState.error
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            (currentError != null) -> {
                ErrorContent(
                    error = currentError.asUiText().asString(),
                    onRetry ={ viewModel.initialize(checkUpId) },
                    modifier = Modifier
                )
//                ErrorDialog(
//                    title = currentError.asUiText().asString(),
//                    message = "",
//                    onDismiss = { viewModel.initialize(checkUpId) }
//                )
            }

            else -> {
                ExportOptionsContent(
                    uiState = uiState,
                    onFormatChange = viewModel::setExportFormat,
                    onPhotoQualityChange = viewModel::setPhotoQuality,
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
            onCancel = { viewModel.cancelExport(UiText.StringResources(R.string.export_dialog_progress_state_init)) }
        )
    }

    // Show result dialog on completion
    if (uiState.showResultDialog) {
        ExportResultDialog(
            result = uiState.exportResult,
            onDismiss = {
                viewModel.dismissResultDialog()
                onNavigateBack()
            },
            onOpenFile = viewModel::openFile,
            onShareFile = viewModel::shareFile
        )
    }
}

@Composable
private fun ExportOptionsContent(
    uiState: ExportOptionsUiState,
    onFormatChange: (ExportFormat) -> Unit,
    onPhotoQualityChange: (PhotoQuality) -> Unit,
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
                photoQuality = uiState.photoQuality,
                onIncludePhotosChange = onIncludePhotosChange,
                onPhotoQualityChange = onPhotoQualityChange
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
                    text = stringResource(R.string.export_screen_action_start),
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
                text = stringResource(R.string.export_screen_preview_title),
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
                    icon = Icons.AutoMirrored.Default.Assignment,
                    text = stringResource(R.string.export_screen_preview_items, itemCount)
                )
                InfoChip(
                    icon = Icons.Default.Photo,
                    text = stringResource(R.string.export_screen_preview_photos, photoCount)
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
            text = stringResource(R.string.export_screen_format_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExportFormat.entries.forEach { format ->
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
                brush = SolidColor(MaterialTheme.colorScheme.primary)
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
                    text = format.getDisplayName(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )

                Text(
                    text = format.getDescription(),
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
    photoQuality: PhotoQuality,
    onIncludePhotosChange: (Boolean) -> Unit,
    onPhotoQualityChange: (PhotoQuality) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.export_screen_photo_options_title),
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
                        text = stringResource(R.string.export_screen_photo_include_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.export_screen_photo_include_desc),
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
                selectedPhotoQuality = photoQuality,
                onPhotoQualityChange = onPhotoQualityChange
            )
        }
    }
}

@Composable
private fun CompressionLevelSection(
    selectedPhotoQuality: PhotoQuality,
    onPhotoQualityChange: PhotoQuality.() -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.export_screen_photo_quality_label),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PhotoQuality.entries.forEach { level ->
                CompressionChip(
                    photoQuality = level,
                    isSelected = selectedPhotoQuality == level,
                    onClick = { onPhotoQualityChange(level) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CompressionChip(
    photoQuality: PhotoQuality,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        onClick = onClick,
        label = {
            Text(
                text = photoQuality.getDisplayName().asString(),
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
            text = stringResource(R.string.export_screen_additional_title) ,
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
                        text = stringResource(R.string.export_screen_notes_include_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.export_screen_notes_include_desc),
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
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}