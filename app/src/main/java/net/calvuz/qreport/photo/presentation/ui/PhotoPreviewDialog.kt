package net.calvuz.qreport.photo.presentation.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.calvuz.qreport.photo.domain.model.Photo
import net.calvuz.qreport.photo.presentation.model.PhotoPreviewUiState
import net.calvuz.qreport.photo.presentation.model.PhotoDisplayModel

/**
 * Dialog fullscreen per la preview delle foto con capacità di navigazione, editing e gestione.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoPreviewDialog(
    photos: List<Photo>,
    initialPhoto: Photo,
    previewState: PhotoPreviewUiState,
    onDismiss: () -> Unit,
    onEditCaption: (Photo) -> Unit,
    onSaveCaption: (String, String) -> Unit,
    onCancelEdit: () -> Unit,
    onUpdateTempCaption: (String) -> Unit,
    onDeletePhoto: (String) -> Unit,
    onShowDeleteConfirmation: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialIndex = photos.indexOfFirst { it.id == initialPhoto.id }.coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { photos.size }
    )

    val currentPhoto = photos.getOrNull(pagerState.currentPage) ?: initialPhoto

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Photo Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val photo = photos[page]
                PhotoPreviewItem(
                    photo = photo,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Controls
            PhotoPreviewTopControls(
                currentIndex = pagerState.currentPage + 1,
                totalPhotos = photos.size,
                onDismiss = onDismiss,
                onDeletePhoto = { onShowDeleteConfirmation(true) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            )

            // Bottom Controls
            PhotoPreviewBottomControls(
                photo = currentPhoto,
                previewState = previewState,
                onEditCaption = onEditCaption,
                onSaveCaption = onSaveCaption,
                onCancelEdit = onCancelEdit,
                onUpdateTempCaption = onUpdateTempCaption,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )

            // Page Indicator
            if (photos.size > 1) {
                PhotoPageIndicator(
                    currentPage = pagerState.currentPage,
                    totalPages = photos.size,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp)
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (previewState.showDeleteConfirmation) {
        PhotoDeleteConfirmationDialog(
            onConfirm = {
                onDeletePhoto(currentPhoto.id)
                if (photos.size == 1) {
                    onDismiss() // Chiudi se era l'ultima foto
                }
            },
            onDismiss = { onShowDeleteConfirmation(false) }
        )
    }
}

/**
 * Singola foto nel pager.
 */
@Composable
private fun PhotoPreviewItem(
    photo: Photo,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.filePath)
                .crossfade(true)
                .build(),
            contentDescription = photo.caption.ifBlank { "Foto check item" },
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Controlli superiori della preview (chiudi, elimina).
 */
@Composable
private fun PhotoPreviewTopControls(
    currentIndex: Int,
    totalPhotos: Int,
    onDismiss: () -> Unit,
    onDeletePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.5f)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Chiudi",
                tint = Color.White
            )
        }

        Text(
            text = "$currentIndex di $totalPhotos",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onDeletePhoto) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Elimina",
                tint = Color.White
            )
        }
    }
}

/**
 * Controlli inferiori con info foto e editing caption.
 */
@Composable
private fun PhotoPreviewBottomControls(
    photo: Photo,
    previewState: PhotoPreviewUiState,
    onEditCaption: (Photo) -> Unit,
    onSaveCaption: (String, String) -> Unit,
    onCancelEdit: () -> Unit,
    onUpdateTempCaption: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayModel = PhotoDisplayModel.fromPhoto(photo)

    Card(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Photo Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = displayModel.formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Text(
                    text = displayModel.formattedSize,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Caption Section
            if (previewState.isEditingCaption) {
                CaptionEditingSection(
                    tempCaption = previewState.tempCaption,
                    isUpdating = previewState.isUpdating,
                    onCaptionChanged = onUpdateTempCaption,
                    onSave = { onSaveCaption(photo.id, previewState.tempCaption) },
                    onCancel = onCancelEdit
                )
            } else {
                CaptionDisplaySection(
                    caption = photo.caption,
                    onEditClick = { onEditCaption(photo) }
                )
            }
        }
    }
}

/**
 * Sezione per mostrare la caption con pulsante edit.
 */
@Composable
private fun CaptionDisplaySection(
    caption: String,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Descrizione",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Text(
                text = caption.ifBlank { "Nessuna descrizione" },
                style = MaterialTheme.typography.bodyLarge,
                color = if (caption.isBlank()) Color.White.copy(alpha = 0.5f) else Color.White
            )
        }

        IconButton(onClick = onEditClick) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Modifica descrizione",
                tint = Color.White
            )
        }
    }
}

/**
 * Sezione per editing della caption.
 */
@Composable
private fun CaptionEditingSection(
    tempCaption: String,
    isUpdating: Boolean,
    onCaptionChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Modifica descrizione",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.8f)
        )

        OutlinedTextField(
            value = tempCaption,
            onValueChange = onCaptionChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Inserisci una descrizione...",
                    color = Color.White.copy(alpha = 0.5f)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            maxLines = 3
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !isUpdating,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text("Annulla")
            }

            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !isUpdating
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Salva")
                }
            }
        }
    }
}

/**
 * Indicatore di pagina per navigazione tra foto.
 */
@Composable
private fun PhotoPageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(totalPages) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (index == currentPage) Color.White else Color.White.copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

/**
 * Dialog di conferma eliminazione foto.
 */
@Composable
private fun PhotoDeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Elimina foto")
        },
        text = {
            Text("Sei sicuro di voler eliminare questa foto? L'operazione non può essere annullata.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Elimina")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}