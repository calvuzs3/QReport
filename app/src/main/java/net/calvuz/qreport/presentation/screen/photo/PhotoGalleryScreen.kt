package net.calvuz.qreport.presentation.screen.photo

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.calvuz.qreport.domain.model.photo.Photo
import net.calvuz.qreport.util.photo.rememberPhotoPickerLauncher
import timber.log.Timber
import java.io.File

/**
 * ✅ GALLERY AGGIORNATA: Con debugging completo dei nuovi metadati PhotoStorageManager
 * Mostra tutti i metadati che ora vengono salvati correttamente:
 * - width/height (utilizzando imageMetadata)
 * - GPS location (PhotoLocation)
 * - perspective, resolution, cameraSettings
 * - Validazione che imageMetadata non sia più inutilizzata
 */
@Composable
fun PhotoGalleryScreen(
    checkItemId: String,
    onNavigateBack: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToPhotoImport: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhotoViewModel = hiltViewModel()
) {
    val galleryState by viewModel.galleryUiState.collectAsStateWithLifecycle()
    val previewState by viewModel.previewUiState.collectAsStateWithLifecycle()

    // Carica le foto quando viene aperta la gallery
    LaunchedEffect(checkItemId) {
        Timber.d("PhotoGalleryScreen: Caricamento foto per checkItemId: $checkItemId")
        viewModel.loadPhotos(checkItemId)
    }

    // ✅ DEBUG: Log dello stato gallery
    LaunchedEffect(galleryState.photos.size) {
        Timber.d("PhotoGalleryScreen: ${galleryState.photos.size} foto caricate")
        galleryState.photos.forEach { photo ->
            Timber.d("Foto: ${photo.fileName}")
            Timber.d("  - FilePath: ${photo.filePath}")
            Timber.d("  - ThumbnailPath: ${photo.thumbnailPath}")
            Timber.d("  - File exists: ${File(photo.filePath).exists()}")
            photo.thumbnailPath?.let { thumbnailPath ->
                Timber.d("  - Thumbnail exists: ${File(thumbnailPath).exists()}")
            }
        }
    }

//    // ✅ NUOVO: DEBUG COMPLETO DEI METADATI PHOTOSTORAGEMANAGER
//    CompletePhotoMetadataAnalysis(galleryState.photos)
//
//    // ✅ NUOVO: VALIDAZIONE CHE imageMetadata ORA SIA UTILIZZATA
//    PhotoStorageManagerValidation(galleryState.photos)
//
//    // ✅ NUOVO: ANALISI GPS LOCATION
//    GPSLocationAnalysis(galleryState.photos)
//
//    // ✅ NUOVO: VALIDAZIONE DIMENSIONI FOTO
//    PhotoDimensionsValidation(galleryState.photos)
//
//    // DEBUG AVANZATO ESISTENTI
//    OriginalPhotoEXIFCheck(galleryState.photos)
//    ThumbnailOrientationAnalysis(galleryState.photos)

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        PhotoGalleryTopBar(
            photosCount = galleryState.photosCount,
            onNavigateBack = onNavigateBack,
            onNavigateToPhotoImport = onNavigateToPhotoImport,
            onNavigateToCamera = onNavigateToCamera,
        )

        when {
            galleryState.isLoading -> {
                PhotoGalleryLoading()
            }
            galleryState.isEmpty -> {

                PhotoGalleryEmpty(
                    onNavigateToCamera = onNavigateToCamera
                )
            }
            else -> {
                PhotoGalleryContent(
                    photos = galleryState.photos,
                    selectedPhoto = galleryState.selectedPhoto,
                    showFullscreen = galleryState.showFullscreen,
                    onPhotoClick = { photo ->
                        viewModel.selectPhoto(photo)      // ✅ Seleziona
                        viewModel.showFullscreen(true)    // ✅ E apre il dialog!
                    },                    onShowFullscreen = viewModel::showFullscreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Error handling
        galleryState.error?.let { error ->
            PhotoGalleryError(
                error = error,
                onDismiss = viewModel::clearError
            )
        }
    }

    // Photo Preview Dialog
    if (galleryState.showFullscreen && galleryState.selectedPhoto != null) {
        PhotoPreviewDialog(
            photos = galleryState.photos,
            initialPhoto = galleryState.selectedPhoto!!,
            previewState = previewState,
            onDismiss = { viewModel.showFullscreen(false) },
            onEditCaption = viewModel::startEditingCaption,
            onSaveCaption = viewModel::saveCaption,
            onCancelEdit = viewModel::cancelEditingCaption,
            onUpdateTempCaption = viewModel::updateTempCaption,
            onDeletePhoto = viewModel::deletePhoto,
            onShowDeleteConfirmation = viewModel::showDeleteConfirmation
        )
    }
}

/**
 * ✅ NUOVO: Analisi completa di tutti i metadati PhotoMetadata
 * Mostra tutti i campi che ora vengono salvati correttamente
 */
@Composable
fun CompletePhotoMetadataAnalysis(photos: List<Photo>) {
    LaunchedEffect(photos.size) {
        if (photos.isNotEmpty()) {
            Timber.d("📊 COMPLETE PHOTO METADATA ANALYSIS")
            Timber.d("=====================================")

            photos.take(3).forEach { photo ->
                try {
                    Timber.d("📷 PHOTO: ${photo.fileName}")
                    Timber.d("📁 ID: ${photo.id}")
                    Timber.d("🔗 CheckItemId: ${photo.checkItemId}")
                    Timber.d("📝 Caption: '${photo.caption}'")
                    Timber.d("📅 TakenAt: ${photo.takenAt}")
                    Timber.d("📦 FileSize: ${photo.fileSize} bytes")
                    Timber.d("🔢 OrderIndex: ${photo.orderIndex}")

                    // ✅ METADATI COMPLETI
                    val metadata = photo.metadata
                    Timber.d("📊 METADATA ANALYSIS:")

                    // ✅ DIMENSIONI (OPZIONE A: dirette nel metadata)
                    if (metadata.toString().contains("width")) {
                        // Se usa PhotoMetadata con width/height diretti
                        Timber.d("   📐 Dimensions: ${metadata.toString().extractDimensions()}")
                    } else {
                        // ✅ DIMENSIONI (OPZIONE B: negli EXIF data)
                        val width = metadata.exifData["ImageWidth"]
                        val height = metadata.exifData["ImageHeight"]
                        if (width != null && height != null) {
                            Timber.d("   📐 Dimensions (from EXIF): ${width}x${height}")
                        } else {
                            Timber.w("   ⚠️ Dimensions not found in metadata!")
                        }
                    }

                    // ✅ EXIF DATA
                    Timber.d("   🏷️ EXIF Data (${metadata.exifData.size} entries):")
                    metadata.exifData.forEach { (key, value) ->
                        Timber.d("      $key: $value")
                    }

                    // ✅ PERSPECTIVE
                    Timber.d("   📸 Perspective: ${metadata.perspective ?: "NULL"}")

                    // ✅ GPS LOCATION
                    val gps = metadata.gpsLocation
                    if (gps != null) {
                        Timber.d("   🌍 GPS Location:")
                        Timber.d("      Lat: ${gps.latitude}")
                        Timber.d("      Lng: ${gps.longitude}")
                        Timber.d("      Alt: ${gps.altitude ?: "NULL"}")
                        Timber.d("      Acc: ${gps.accuracy ?: "NULL"}")
                    } else {
                        Timber.d("   🌍 GPS Location: NULL")
                    }

                    // ✅ TIMESTAMP E FILESIZE METADATA
                    Timber.d("   🕐 Metadata Timestamp: ${metadata.timestamp ?: "NULL"}")
                    Timber.d("   📦 Metadata FileSize: ${metadata.fileSize}")

                    // ✅ RESOLUTION
                    Timber.d("   🎯 Resolution: ${metadata.resolution ?: "NULL"}")

                    // ✅ CAMERA SETTINGS
                    val camera = metadata.cameraSettings
                    if (camera != null) {
                        Timber.d("   📷 Camera Settings:")
                        Timber.d("      Resolution: ${camera.resolution}")
                        Timber.d("      Perspective: ${camera.perspective}")
                        Timber.d("      AutoCorrect: ${camera.autoCorrectOrientation}")
                        Timber.d("      GPS Tagging: ${camera.enableGpsTagging}")
                    } else {
                        Timber.d("   📷 Camera Settings: NULL")
                    }

                } catch (e: Exception) {
                    Timber.e("❌ Error analyzing metadata for ${photo.fileName}: ${e.message}")
                }

                Timber.d("-----------------------------------")
            }

            Timber.d("=====================================")
        }
    }
}

/**
 * ✅ NUOVO: Validazione che imageMetadata ora sia utilizzata correttamente
 * Confronta dimensioni reali del file con quelle salvate nei metadati
 */
@Composable
fun PhotoStorageManagerValidation(photos: List<Photo>) {
    LaunchedEffect(photos.size) {
        if (photos.isNotEmpty()) {
            Timber.d("🔧 PHOTOSTORAGEMANAGER VALIDATION")
            Timber.d("==================================")

            var correctMetadata = 0
            var incorrectMetadata = 0

            photos.take(3).forEach { photo ->
                try {
                    val file = File(photo.filePath)
                    if (!file.exists()) {
                        Timber.w("⚠️ ${photo.fileName}: File not found")
                        return@forEach
                    }

                    // ✅ LEGGI DIMENSIONI REALI DAL FILE
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(photo.filePath, options)
                    val realWidth = options.outWidth
                    val realHeight = options.outHeight

                    // ✅ ESTRAI DIMENSIONI DAI METADATI
                    val (metadataWidth, metadataHeight) = extractDimensionsFromMetadata(photo)

                    Timber.d("📷 ${photo.fileName}")
                    Timber.d("   📐 Real dimensions: ${realWidth}x${realHeight}")
                    Timber.d("   📊 Metadata dimensions: ${metadataWidth}x${metadataHeight}")

                    // ✅ VALIDAZIONE
                    if (realWidth == metadataWidth && realHeight == metadataHeight) {
                        correctMetadata++
                        Timber.d("   ✅ imageMetadata CORRECTLY USED!")
                    } else {
                        incorrectMetadata++
                        Timber.e("   ❌ DIMENSION MISMATCH!")
                        if (metadataWidth == 0 && metadataHeight == 0) {
                            Timber.e("   ❌ imageMetadata NOT USED (still 0x0)!")
                        }
                    }

                } catch (e: Exception) {
                    incorrectMetadata++
                    Timber.e("❌ Error validating ${photo.fileName}: ${e.message}")
                }
            }

            Timber.d("==================================")
            Timber.d("📊 VALIDATION SUMMARY:")
            Timber.d("   ✅ Correct metadata: $correctMetadata")
            Timber.d("   ❌ Incorrect metadata: $incorrectMetadata")

            if (correctMetadata > 0) {
                Timber.d("🎉 imageMetadata IS NOW BEING USED!")
            }
            if (incorrectMetadata > 0) {
                Timber.e("❌ Some photos still have incorrect metadata")
            }
        }
    }
}

/**
 * ✅ NUOVO: Analisi specifica GPS Location
 */
@Composable
fun GPSLocationAnalysis(photos: List<Photo>) {
    LaunchedEffect(photos.size) {
        if (photos.isNotEmpty()) {
            Timber.d("🌍 GPS LOCATION ANALYSIS")
            Timber.d("========================")

            var photosWithGPS = 0
            var photosWithoutGPS = 0
            var gpsErrors = 0

            photos.take(5).forEach { photo ->
                try {
                    val gps = photo.metadata.gpsLocation

                    Timber.d("📷 ${photo.fileName}")

                    if (gps != null) {
                        photosWithGPS++
                        Timber.d("   ✅ GPS Present:")
                        Timber.d("      📍 Coordinates: ${gps.latitude}, ${gps.longitude}")

                        // ✅ VALIDAZIONE COORDINATE
                        if (gps.latitude in -90.0..90.0 && gps.longitude in -180.0..180.0) {
                            Timber.d("      ✅ Valid coordinates")
                        } else {
                            Timber.w("      ⚠️ Invalid coordinates!")
                            gpsErrors++
                        }

                        // ✅ ALTITUDINE E PRECISIONE
                        if (gps.altitude != null) {
                            Timber.d("      🏔️ Altitude: ${gps.altitude}m")
                        }
                        if (gps.accuracy != null) {
                            Timber.d("      🎯 Accuracy: ±${gps.accuracy}m")
                        }

                        // ✅ VERIFICA EXIF GPS ORIGINALI
                        try {
                            val exif = ExifInterface(photo.filePath)
                            val latLong = FloatArray(2)
                            val hasExifGPS = exif.getLatLong(latLong)

                            if (hasExifGPS) {
                                val exifLat = latLong[0].toDouble()
                                val exifLng = latLong[1].toDouble()
                                val latDiff = kotlin.math.abs(gps.latitude - exifLat)
                                val lngDiff = kotlin.math.abs(gps.longitude - exifLng)

                                if (latDiff < 0.0001 && lngDiff < 0.0001) {
                                    Timber.d("      ✅ GPS matches EXIF data")
                                } else {
                                    Timber.w("      ⚠️ GPS differs from EXIF: ${exifLat}, ${exifLng}")
                                }
                            } else {
                                Timber.d("      📝 GPS from metadata (no EXIF GPS)")
                            }

                        } catch (e: Exception) {
                            Timber.w("      ⚠️ Cannot verify EXIF GPS: ${e.message}")
                        }

                    } else {
                        photosWithoutGPS++
                        Timber.d("   ❌ No GPS data")

                        // ✅ VERIFICA SE GPS ERA DISPONIBILE MA NON ESTRATTO
                        try {
                            val exif = ExifInterface(photo.filePath)
                            val latLong = FloatArray(2)
                            val hasExifGPS = exif.getLatLong(latLong)

                            if (hasExifGPS) {
                                Timber.w("      ⚠️ GPS available in EXIF but not extracted!")
                                Timber.w("      📍 Available GPS: ${latLong[0]}, ${latLong[1]}")
                                gpsErrors++
                            }
                        } catch (e: Exception) {
                            // GPS effettivamente non disponibile
                        }
                    }

                } catch (e: Exception) {
                    gpsErrors++
                    Timber.e("❌ Error analyzing GPS for ${photo.fileName}: ${e.message}")
                }
            }

            Timber.d("========================")
            Timber.d("📊 GPS SUMMARY:")
            Timber.d("   ✅ Photos with GPS: $photosWithGPS")
            Timber.d("   ❌ Photos without GPS: $photosWithoutGPS")
            Timber.d("   ⚠️ GPS errors: $gpsErrors")

            if (gpsErrors > 0) {
                Timber.e("❌ GPS extraction issues detected!")
            }
        }
    }
}

/**
 * ✅ NUOVO: Validazione specifica delle dimensioni delle foto
 */
@Composable
fun PhotoDimensionsValidation(photos: List<Photo>) {
    LaunchedEffect(photos.size) {
        if (photos.isNotEmpty()) {
            Timber.d("📐 PHOTO DIMENSIONS VALIDATION")
            Timber.d("==============================")

            photos.take(3).forEach { photo ->
                try {
                    // ✅ DIMENSIONI REALI
                    val file = File(photo.filePath)
                    if (!file.exists()) return@forEach

                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(photo.filePath, options)
                    val realWidth = options.outWidth
                    val realHeight = options.outHeight

                    // ✅ DIMENSIONI DAI METADATI
                    val (metaWidth, metaHeight) = extractDimensionsFromMetadata(photo)

                    // ✅ CAMERA SETTINGS RESOLUTION
                    val cameraResolution = photo.metadata.cameraSettings?.resolution

                    Timber.d("📷 ${photo.fileName}")
                    Timber.d("   📁 File size: ${photo.fileSize} bytes")
                    Timber.d("   📐 Real dimensions: ${realWidth}x${realHeight}")
                    Timber.d("   📊 Metadata dimensions: ${metaWidth}x${metaHeight}")
                    Timber.d("   🎯 Target resolution: ${cameraResolution}")

                    // ✅ ANALISI ASPECT RATIO
                    val realAspectRatio = realWidth.toFloat() / realHeight.toFloat()
                    val isLandscape = realAspectRatio > 1.0f
                    val isPortrait = realAspectRatio < 1.0f
                    val isSquare = kotlin.math.abs(realAspectRatio - 1.0f) < 0.1f

                    Timber.d("   📏 Aspect ratio: ${"%.2f".format(realAspectRatio)} (${
                        when {
                            isSquare -> "Square"
                            isLandscape -> "Landscape"
                            isPortrait -> "Portrait"
                            else -> "Unknown"
                        }
                    })")

                    // ✅ ORIENTATION ANALYSIS
                    try {
                        val exif = ExifInterface(photo.filePath)
                        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)

                        Timber.d("   🔄 EXIF Orientation: $orientation")

                        when (orientation) {
                            1 -> Timber.d("      ✅ Normal orientation")
                            6 -> Timber.d("      📱 Rotated 90° CW")
                            8 -> Timber.d("      📱 Rotated 90° CCW")
                            3 -> Timber.d("      🔄 Rotated 180°")
                            else -> Timber.w("      ⚠️ Unusual orientation: $orientation")
                        }

                        // ✅ VERIFICA SE DIMENSIONI SONO POST-ROTAZIONE
                        val needsRotation = orientation in listOf(6, 8)
                        if (needsRotation) {
                            Timber.d("      📝 Dimensions should be post-rotation")
                        }

                    } catch (e: Exception) {
                        Timber.w("   ⚠️ Cannot read EXIF orientation: ${e.message}")
                    }

                    // ✅ VALIDATION RESULT
                    if (realWidth == metaWidth && realHeight == metaHeight) {
                        Timber.d("   ✅ DIMENSIONS PERFECT MATCH!")
                    } else if (metaWidth == 0 && metaHeight == 0) {
                        Timber.e("   ❌ METADATA DIMENSIONS NOT SET!")
                    } else {
                        Timber.w("   ⚠️ Dimension mismatch - investigate")
                    }

                } catch (e: Exception) {
                    Timber.e("❌ Error validating dimensions for ${photo.fileName}: ${e.message}")
                }

                Timber.d("------------------------------")
            }

            Timber.d("==============================")
        }
    }
}

/**
 * ✅ HELPER: Estrae dimensioni dai metadati (supporta entrambe le opzioni)
 */
private fun extractDimensionsFromMetadata(photo: Photo): Pair<Int, Int> {
    return try {
        // ✅ OPZIONE A: width/height diretti nel PhotoMetadata
        // (questo richiede reflection o toString parsing se non hai accesso diretto ai campi)
        val metadataString = photo.metadata.toString()
        if (metadataString.contains("width=") && metadataString.contains("height=")) {
            val width = metadataString.extractWidth()
            val height = metadataString.extractHeight()
            Pair(width, height)
        } else {
            // ✅ OPZIONE B: width/height negli EXIF data
            val width = photo.metadata.exifData["ImageWidth"]?.toIntOrNull() ?: 0
            val height = photo.metadata.exifData["ImageHeight"]?.toIntOrNull() ?: 0
            Pair(width, height)
        }
    } catch (e: Exception) {
        Pair(0, 0)
    }
}

/**
 * ✅ HELPER: Estrae width da toString (per PhotoMetadata con width diretto)
 */
private fun String.extractWidth(): Int {
    return try {
        val regex = "width=(\\d+)".toRegex()
        regex.find(this)?.groupValues?.get(1)?.toInt() ?: 0
    } catch (e: Exception) {
        0
    }
}

/**
 * ✅ HELPER: Estrae height da toString (per PhotoMetadata con height diretto)
 */
private fun String.extractHeight(): Int {
    return try {
        val regex = "height=(\\d+)".toRegex()
        regex.find(this)?.groupValues?.get(1)?.toInt() ?: 0
    } catch (e: Exception) {
        0
    }
}

/**
 * ✅ HELPER: Estrae dimensioni da toString (per PhotoMetadata con width/height diretti)
 */
private fun String.extractDimensions(): String {
    return try {
        val width = this.extractWidth()
        val height = this.extractHeight()
        "${width}x${height}"
    } catch (e: Exception) {
        "Unknown"
    }
}

// ===== COMPONENTI UI ESISTENTI (invariati) =====

/**
 * Top bar della galleria con titolo e azioni.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoGalleryTopBar(
    photosCount: Int,
    onNavigateBack: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToPhotoImport: (Uri) -> Unit,
) {
    // Photo picker launcher
    val photoPickerLauncher = rememberPhotoPickerLauncher { uri ->
        uri?.let { onNavigateToPhotoImport(it) }
    }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Foto Check Item",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "$photosCount foto",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Indietro"
                )
            }
        },
        actions = {
            // Import Photo
            IconButton(
                onClick = { photoPickerLauncher.launch() }
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Importa dalla galleria"
                )
            }

            // Capture Photo
            IconButton(onClick = onNavigateToCamera) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Scatta Foto"
                )
            }
        }
    )
}

/**
 * Contenuto principale della galleria con griglia di foto.
 */
@Composable
private fun PhotoGalleryContent(
    photos: List<Photo>,
    selectedPhoto: Photo?,
    showFullscreen: Boolean,
    onPhotoClick: (Photo) -> Unit,
    onShowFullscreen: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(photos) { photo ->
            PhotoGridItem(
                photo = photo,
                isSelected = photo.id == selectedPhoto?.id,
                onClick = { onPhotoClick(photo) },
                onDoubleClick = {
                    onPhotoClick(photo)
                    onShowFullscreen(true)
                }
            )
        }
    }
}

/**
 * Item della griglia foto con gestione fallback thumbnail.
 */
@Composable
private fun PhotoGridItem(
    photo: Photo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // ✅ FALLBACK: Se thumbnail non esiste, usa foto originale
    val imageSource = when {
        photo.thumbnailPath != null && File(photo.thumbnailPath).exists() -> {
            photo.thumbnailPath
        }
        File(photo.filePath).exists() -> {
            photo.filePath
        }
        else -> {
            null
        }
    }

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        elevation = if (isSelected) {
            CardDefaults.cardElevation(defaultElevation = 8.dp)
        } else {
            CardDefaults.cardElevation(defaultElevation = 2.dp)
        },
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (imageSource != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageSource)
                        .crossfade(true)
                        .build(),
                    contentDescription = photo.caption.ifEmpty { "Foto ${photo.fileName}" },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder per foto non trovata
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "File non trovato",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Overlay informazioni foto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.Black.copy(alpha = 0.7f)
                    )
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = photo.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (photo.caption.isNotEmpty()) {
                        Text(
                            text = photo.caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Stato di caricamento della galleria.
 */
@Composable
private fun PhotoGalleryLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Caricamento foto...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Stato vuoto della galleria.
 */
@Composable
private fun PhotoGalleryEmpty(
    onNavigateToCamera: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nessuna foto presente",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Scatta la prima foto per questo check item",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onNavigateToCamera
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Scatta Foto")
            }
        }
    }
}

/**
 * Gestione errori della galleria.
 */
@Composable
private fun PhotoGalleryError(
    error: String,
    onDismiss: () -> Unit
) {
    LaunchedEffect(error) {
        // Mostra errore per 3 secondi poi dismissi automaticamente
        kotlinx.coroutines.delay(3000)
        onDismiss()
    }

    Snackbar(
        action = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
        modifier = Modifier.padding(16.dp)
    ) {
        Text(error)
    }
}

// ===== METODI DEBUG ESISTENTI (mantenuti per compatibilità) =====

/**
 * ✅ CORREZIONE: Debug che controlla FILE ORIGINALI, non thumbnail
 */
@Composable
fun OriginalPhotoEXIFCheck(photos: List<Photo>) {
    LaunchedEffect(photos.size) {
        if (photos.isNotEmpty()) {
            Timber.d("🔍 ORIGINAL PHOTO EXIF CHECK")
            Timber.d("============================")

            photos.take(3).forEach { photo ->
                try {
                    // ✅ FORCE: Usa sempre il file ORIGINALE
                    val originalPath = photo.filePath
                    val thumbnailPath = photo.thumbnailPath

                    Timber.d("📷 PHOTO: ${photo.fileName}")
                    Timber.d("📁 Original: $originalPath")
                    Timber.d("🖼️ Thumbnail: $thumbnailPath")

                    val originalFile = File(originalPath)
                    val thumbnailFile = if (thumbnailPath != null) File(thumbnailPath) else null

                    Timber.d("📦 Original exists: ${originalFile.exists()}")
                    Timber.d("📦 Thumbnail exists: ${thumbnailFile?.exists() ?: false}")

                    // ✅ ANALIZZA FOTO ORIGINALE
                    if (originalFile.exists()) {
                        Timber.d("📏 Original size: ${originalFile.length()} bytes")

                        try {
                            val exif = ExifInterface(originalPath)

                            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
                            val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                            val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                            val datetime = exif.getAttribute(ExifInterface.TAG_DATETIME)
                            val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)

                            Timber.d("🔄 ORIGINAL EXIF Orientation: $orientation")
                            Timber.d("📱 ORIGINAL Camera Make: ${make ?: "NULL"}")
                            Timber.d("📱 ORIGINAL Camera Model: ${model ?: "NULL"}")
                            Timber.d("🕐 ORIGINAL DateTime: ${datetime ?: "NULL"}")
                            Timber.d("💻 ORIGINAL Software: ${software ?: "NULL"}")

                            // ✅ BITMAP ORIGINALE
                            val options = BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                            }
                            BitmapFactory.decodeFile(originalPath, options)
                            Timber.d("📐 ORIGINAL Bitmap: ${options.outWidth}x${options.outHeight}")

                            // ✅ CONFRONTA CON THUMBNAIL SE ESISTE
                            if (thumbnailFile?.exists() == true) {
                                val thumbExif = ExifInterface(thumbnailPath!!)
                                val thumbOrientation = thumbExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)

                                val thumbOptions = BitmapFactory.Options().apply {
                                    inJustDecodeBounds = true
                                }
                                BitmapFactory.decodeFile(thumbnailPath, thumbOptions)

                                Timber.d("🖼️ THUMBNAIL Orientation: $thumbOrientation")
                                Timber.d("🖼️ THUMBNAIL Bitmap: ${thumbOptions.outWidth}x${thumbOptions.outHeight}")

                                // ✅ DIAGNOSTICA
                                if (orientation != thumbOrientation) {
                                    Timber.e("❌ ORIENTATION MISMATCH! Original=$orientation, Thumbnail=$thumbOrientation")
                                }
                            }

                            // ✅ ANALISI FINALE
                            when (orientation) {
                                1 -> Timber.d("✅ ORIGINAL has NORMAL orientation")
                                6 -> Timber.w("⚠️ ORIGINAL needs 90° rotation")
                                8 -> Timber.w("⚠️ ORIGINAL needs 270° rotation")
                                3 -> Timber.w("⚠️ ORIGINAL needs 180° rotation")
                                0, -1 -> Timber.e("❌ ORIGINAL has missing/corrupt EXIF")
                                else -> Timber.w("⚠️ ORIGINAL has unusual orientation: $orientation")
                            }

                        } catch (e: Exception) {
                            Timber.e("❌ Error reading ORIGINAL EXIF: ${e.message}")
                        }

                    } else {
                        Timber.e("❌ ORIGINAL FILE NOT FOUND: $originalPath")
                    }

                } catch (e: Exception) {
                    Timber.e("❌ Error analyzing ${photo.fileName}: ${e.message}")
                }

                Timber.d("----------------------------")
            }

            Timber.d("============================")
        }
    }
}

/**
 * ✅ THUMBNAIL GENERATION ANALYSIS
 */
@Composable
fun ThumbnailOrientationAnalysis(photos: List<Photo>) {
    LaunchedEffect(photos.size) {
        if (photos.isNotEmpty()) {
            Timber.d("🖼️ THUMBNAIL ORIENTATION ANALYSIS")
            Timber.d("==================================")

            var correctThumbnails = 0
            var incorrectThumbnails = 0
            var missingThumbnails = 0

            photos.take(5).forEach { photo ->
                try {
                    val originalPath = photo.filePath
                    val thumbnailPath = photo.thumbnailPath

                    if (thumbnailPath == null) {
                        missingThumbnails++
                        Timber.w("⚠️ ${photo.fileName}: No thumbnail path")
                        return@forEach
                    }

                    val originalFile = File(originalPath)
                    val thumbnailFile = File(thumbnailPath)

                    if (!originalFile.exists() || !thumbnailFile.exists()) {
                        missingThumbnails++
                        Timber.w("⚠️ ${photo.fileName}: Missing files (orig:${originalFile.exists()}, thumb:${thumbnailFile.exists()})")
                        return@forEach
                    }

                    // Leggi EXIF da entrambi
                    val originalExif = ExifInterface(originalPath)
                    val thumbnailExif = ExifInterface(thumbnailPath)

                    val originalOrientation = originalExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
                    val thumbnailOrientation = thumbnailExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)

                    // Leggi dimensioni
                    val origOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(originalPath, origOptions)

                    val thumbOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(thumbnailPath, thumbOptions)

                    Timber.d("📷 ${photo.fileName}")
                    Timber.d("   Original: ${origOptions.outWidth}x${origOptions.outHeight} (orientation: $originalOrientation)")
                    Timber.d("   Thumbnail: ${thumbOptions.outWidth}x${thumbOptions.outHeight} (orientation: $thumbnailOrientation)")

                    // ✅ CHECK: Il thumbnail dovrebbe essere sempre square E orientation 1
                    val isThumbnailCorrect = (thumbnailOrientation == 1) &&
                            (thumbOptions.outWidth == thumbOptions.outHeight)

                    if (isThumbnailCorrect) {
                        correctThumbnails++
                        Timber.d("   ✅ Thumbnail orientamento corretto")
                    } else {
                        incorrectThumbnails++
                        Timber.w("   ⚠️ Thumbnail problematico!")
                    }

                } catch (e: Exception) {
                    incorrectThumbnails++
                    Timber.e("❌ Error analyzing ${photo.fileName}: ${e.message}")
                }
            }

            Timber.d("==================================")
            Timber.d("📊 SUMMARY:")
            Timber.d("   ✅ Correct thumbnails: $correctThumbnails")
            Timber.d("   ⚠️ Incorrect thumbnails: $incorrectThumbnails")
            Timber.d("   ❌ Missing thumbnails: $missingThumbnails")

            if (incorrectThumbnails > 0) {
                Timber.e("❌ THUMBNAIL ORIENTATION ISSUE CONFIRMED!")
                Timber.e("   Solution: Fix thumbnail generation process")
            }
        }
    }
}

/**
 * Dialog placeholder per preview foto (da implementare)
 */
@Composable
fun PhotoPreviewDialog(
    photos: List<Photo>,
    initialPhoto: Photo,
    previewState: Any, // Sostituisci con il tipo corretto
    onDismiss: () -> Unit,
    onEditCaption: (Photo) -> Unit,
    onSaveCaption: (Photo, String) -> Unit,
    onCancelEdit: () -> Unit,
    onUpdateTempCaption: (String) -> Unit,
    onDeletePhoto: (Photo) -> Unit,
    onShowDeleteConfirmation: (Photo) -> Unit
) {
    // TODO: Implementa dialog preview foto
    // Per ora mostra placeholder
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Preview Foto") },
        text = { Text("Preview foto: ${initialPhoto.fileName}") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi")
            }
        }
    )
}