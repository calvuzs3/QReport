package net.calvuz.qreport.presentation.feature.photo.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.calvuz.qreport.domain.model.photo.Photo
import timber.log.Timber
import java.io.File

/**
 * ✅ COMPONENTE AGGIORNATO: CheckItemPhotoPreview SENZA PhotoViewModel
 *
 * CAMBIAMENTI PRINCIPALI:
 * - ❌ Rimosso: PhotoViewModel e hiltViewModel()
 * - ✅ Aggiunto: Parametri photos e photoCount dal parent ViewModel
 * - ✅ Aggiunto: Nessun LaunchedEffect o collect - i dati arrivano direttamente
 * - ✅ Aggiunto: Stato di loading opzionale per feedback UX
 *
 * VANTAGGI:
 * - Single Source of Truth: CheckUpDetailViewModel gestisce tutte le foto
 * - Performance: Nessun ViewModel aggiuntivo per ogni istanza
 * - Consistency: Le foto sono sempre sincronizzate in tutta la UI
 */
@Composable
fun CheckItemPhotoPreview(
    modifier: Modifier = Modifier,
    checkItemId: String,
    photos: List<Photo>,                    // ✅ NUOVO: Riceve foto come parametro
    photoCount: Int,                        // ✅ NUOVO: Riceve count come parametro
    isLoadingPhotos: Boolean = false,       // ✅ NUOVO: Stato loading opzionale
    onNavigateToCamera: () -> Unit,
    onNavigateToGallery: () -> Unit
) {
    // ✅ DEBUG: Log dei dati ricevuti (rimuovere in produzione)
    LaunchedEffect(photos, photoCount) {
        Timber.d("CheckItemPhotoPreview: Ricevute $photoCount foto per checkItemId: $checkItemId")
        photos.take(3).forEach { photo ->
            Timber.d("  - ${photo.fileName}: ${if (File(photo.filePath).exists()) "EXISTS" else "MISSING"}")
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Photo Count Badge
        PhotoCountBadge(
            count = photoCount,
            isLoading = isLoadingPhotos,
            onClick = if (photoCount > 0) onNavigateToGallery else onNavigateToCamera
        )

        // Photo Thumbnails or Empty State
        if (isLoadingPhotos) {
            // Loading state
            PhotoLoadingState(modifier = Modifier.weight(1f))
        } else if (photos.isNotEmpty()) {
            // Photos available
            PhotoThumbnails(
                photos = photos.take(3),  // Solo prime 3 per il preview
                totalCount = photoCount,
                onClick = onNavigateToGallery,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Empty state
            EmptyPhotoState(
                onClick = onNavigateToCamera,
                modifier = Modifier.weight(1f)
            )
        }

        // Add Photo Button
        AddPhotoButton(
            onClick = onNavigateToCamera,
            enabled = !isLoadingPhotos  // ✅ Disabilita durante loading
        )
    }
}

/**
 * ✅ AGGIORNATO: Badge con stato di loading
 */
@Composable
private fun PhotoCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Badge(
        modifier = modifier
            .clickable(enabled = !isLoading) { onClick() },
        containerColor = when {
            isLoading -> MaterialTheme.colorScheme.surfaceVariant
            count > 0 -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
            }

            Text(
                text = if (isLoading) "..." else count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * ✅ NUOVO: Stato di loading per le foto
 */
@Composable
private fun PhotoLoadingState(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(6.dp)
                    )
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.Center),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * ✅ INVARIATO: Thumbnails delle foto
 */
@Composable
private fun PhotoThumbnails(
    photos: List<Photo>,
    totalCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable { onClick() },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnails (max 3)
        photos.take(3).forEach { photo ->
            PhotoThumbnail(
                photo = photo,
                size = 32.dp
            )
        }

        // Show remaining count if more than 3
        if (totalCount > 3) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${totalCount - 3}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * ✅ INVARIATO: Thumbnail singola foto con gestione path migliorata
 */
@Composable
private fun PhotoThumbnail(
    photo: Photo,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val imagePath = getImagePath(photo)

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imagePath)
            .crossfade(true)
            .build(),
        contentDescription = photo.caption.ifBlank { "Foto check item" },
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp)),
        contentScale = ContentScale.Crop,
        onError = { error ->
            Timber.e("Errore caricamento thumbnail: ${error.result.throwable.message}")
            Timber.d("Path tentativo: $imagePath")
        },
        onSuccess = {
            Timber.d("Thumbnail caricata: ${photo.fileName}")
        }
    )
}

/**
 * ✅ INVARIATO: Helper per determinare il path migliore
 */
private fun getImagePath(photo: Photo): String {
    return when {
        // Prima prova thumbnail se esiste ed è accessibile
        !photo.thumbnailPath.isNullOrBlank() && File(photo.thumbnailPath).exists() -> {
            Timber.d("Usando thumbnail: ${photo.thumbnailPath}")
            photo.thumbnailPath
        }
        // Poi prova file originale
        File(photo.filePath).exists() -> {
            Timber.d("Usando file originale: ${photo.filePath}")
            photo.filePath
        }
        // Fallback su thumbnail path anche se non esiste (per debugging)
        !photo.thumbnailPath.isNullOrBlank() -> {
            Timber.w("Thumbnail non trovata ma provo comunque: ${photo.thumbnailPath}")
            photo.thumbnailPath
        }
        // Ultimo fallback su file path originale
        else -> {
            Timber.w("Nessun file trovato, provo file path originale: ${photo.filePath}")
            photo.filePath
        }
    }
}

/**
 * ✅ INVARIATO: Stato vuoto
 */
@Composable
private fun EmptyPhotoState(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Nessuna foto",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * ✅ AGGIORNATO: Pulsante con stato enabled
 */
@Composable
private fun AddPhotoButton(
    modifier: Modifier = Modifier ,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(32.dp)
            .background(
                if (enabled)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Aggiungi foto",
            modifier = Modifier.size(16.dp),
            tint = if (enabled)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * ✅ NUOVO: Versione semplificata per uso nelle liste (solo counter e pulsante)
 * Ora riceve i dati come parametri invece di usare PhotoViewModel
 */
@Composable
fun CheckItemPhotoCounter(
    modifier: Modifier = Modifier,
    photoCount: Int,                        // ✅ NUOVO: Parametro
    isLoadingPhotos: Boolean = false,       // ✅ NUOVO: Parametro
    onNavigateToCamera: () -> Unit,
    onNavigateToGallery: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PhotoCountBadge(
            count = photoCount,
            isLoading = isLoadingPhotos,
            onClick = if (photoCount > 0) onNavigateToGallery else onNavigateToCamera
        )

        AddPhotoButton(
            onClick = onNavigateToCamera,
            enabled = !isLoadingPhotos
        )
    }
}

/**
 * ✅ NUOVO: Versione minimalista solo display
 * Ora riceve photoCount come parametro
 */
@Composable
fun PhotoCountDisplay(
    photoCount: Int,                        // ✅ NUOVO: Parametro
    modifier: Modifier = Modifier
) {
    if (photoCount > 0) {
        Badge(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.secondary
        ) {
            Text(
                text = photoCount.toString(),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}