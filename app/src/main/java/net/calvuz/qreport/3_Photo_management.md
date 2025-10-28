# 📸 QReport - Sistema Gestione Foto

**Versione:** 1.0  
**Data:** Ottobre 2025  
**Tecnologia:** CameraX + Android Storage  
**Target:** Acquisizione e gestione foto per check-up industriali

---

## 📋 INDICE

1. [Panoramica Sistema](#1-panoramica-sistema)
2. [CameraX Setup](#2-camerax-setup)
3. [Architettura Photo](#3-architettura-photo)
4. [Camera Integration](#4-camera-integration)
5. [Storage Management](#5-storage-management)
6. [Image Processing](#6-image-processing)
7. [Gallery & Preview](#7-gallery--preview)
8. [Performance Optimization](#8-performance-optimization)
9. [Data Persistence](#9-data-persistence)
10. [Testing Strategy](#10-testing-strategy)
11. [Implementation Guide](#11-implementation-guide)

---

## 1. PANORAMICA SISTEMA

### 1.1 Obiettivi Photo System

#### 🎯 **Requisiti Funzionali**
- **Photo Capture:** Scatto foto direttamente dall'app con preview
- **Check Item Association:** Collegamento foto ai singoli check items
- **Storage Ottimizzato:** Organizzazione gerarchica e compression intelligente
- **Gallery Integration:** Visualizzazione, editing, eliminazione foto
- **Export Ready:** Pre-processing automatico per inclusione nei report
- **Offline First:** Funzionamento completo senza connessione

#### 📊 **Workflow Utente**
```
┌─────────────────────────────────────┐
│ 1. ACQUISIZIONE                     │
│ • Tap camera icon su check item     │
│ • Preview in real-time              │
│ • Scatto con controlli manuali      │
│ • Conferma/retry immediato          │
├─────────────────────────────────────┤
│ 2. ELABORAZIONE                     │
│ • Auto-compression intelligente     │
│ • Metadata injection (timestamp)    │
│ • Storage in directory strutturata  │
│ • Thumbnail generation              │
├─────────────────────────────────────┤
│ 3. GESTIONE                         │
│ • Gallery per check item            │
│ • Caption editing                   │
│ • Photo elimination                 │
│ • Bulk operations                   │
├─────────────────────────────────────┤
│ 4. EXPORT                           │
│ • Automatic processing per Word     │
│ • Compression level configurabile   │
│ • Watermark application             │
│ • Grid layout preparation           │
└─────────────────────────────────────┘
```

### 1.2 Requisiti Tecnici

#### 📱 **Hardware Requirements**
- **Camera:** Minimo 8MP, autofocus supportato
- **Storage:** 2GB disponibili per foto (configurabile)
- **RAM:** Gestione efficiente memoria per processing
- **Permissions:** Camera, storage (scoped storage Android 10+)

#### 🔧 **Functional Requirements**
- **Max Photos per Check Item:** 10 foto (configurabile)
- **Supported Formats:** JPEG (primary), PNG (optional)
- **Resolution Range:** 1080p - 4K (auto-adapt based on storage)
- **Compression:** Adaptive quality 70-95% JPEG
- **Thumbnail Size:** 150x150px per gallery preview

---

## 2. CAMERAX SETUP

### 2.1 Dependencies

```kotlin
// build.gradle.kts (app) - CameraX dependencies
dependencies {
    // CameraX Core
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.camera:camera-extensions:1.3.1")
    
    // Image Analysis (optional, for future features)
    implementation("androidx.camera:camera-mlkit-vision:1.3.0")
    
    // Compose integration
    implementation("androidx.camera:camera-view:1.3.1")
    
    // Image loading and caching
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")
    
    // Image processing
    implementation("androidx.exifinterface:exifinterface:1.3.6")
}
```

### 2.2 Permissions Setup

```xml
<!-- AndroidManifest.xml - Camera and storage permissions -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Camera permissions -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature 
        android:name="android.hardware.camera" 
        android:required="false" />
    <uses-feature 
        android:name="android.hardware.camera.autofocus" 
        android:required="false" />
    
    <!-- Storage permissions (scoped storage) -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission 
        android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
        android:maxSdkVersion="28" />
    
    <application
        android:requestLegacyExternalStorage="true"
        android:preserveLegacyExternalStorage="true">
        
        <!-- File provider for sharing -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.photo.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/photo_file_paths" />
        </provider>
    </application>
</manifest>
```

### 2.3 File Provider Configuration

```xml
<!-- res/xml/photo_file_paths.xml -->
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Internal storage for app photos -->
    <files-path name="photos" path="photos/" />
    
    <!-- Cache directory for temporary files -->
    <cache-path name="temp_photos" path="temp/" />
    
    <!-- External storage for exports -->
    <external-files-path name="exported_photos" path="QReport/Photos/" />
</paths>
```

---

## 3. ARCHITETTURA PHOTO

### 3.1 Domain Layer

```kotlin
// domain/model/Photo.kt - Domain models per foto
data class Photo(
    val id: String = UUID.randomUUID().toString(),
    val checkItemId: String,
    val filePath: String,
    val fileName: String,
    val caption: String = "",
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val fileSize: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val compressionQuality: Int = 85,
    val thumbnailPath: String? = null
)

data class PhotoMetadata(
    val originalFileName: String,
    val captureTimestamp: LocalDateTime,
    val deviceInfo: String,
    val gpsLocation: GpsLocation? = null,
    val cameraSettings: CameraSettings
)

data class CameraSettings(
    val resolution: String,
    val flashMode: String,
    val focusMode: String,
    val whiteBalance: String
)

data class GpsLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float
)

// Photo operations
sealed class PhotoResult {
    data class Success(val photo: Photo) : PhotoResult()
    data class Error(val exception: Throwable, val errorType: PhotoErrorType) : PhotoResult()
}

enum class PhotoErrorType {
    CAMERA_NOT_AVAILABLE,
    PERMISSION_DENIED,
    STORAGE_FULL,
    CAPTURE_FAILED,
    PROCESSING_ERROR,
    FILE_NOT_FOUND
}

// Gallery states
data class PhotoGalleryState(
    val photos: List<Photo> = emptyList(),
    val selectedPhoto: Photo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showFullscreen: Boolean = false
)
```

### 3.2 Use Cases

```kotlin
// domain/usecase/photo/CapturePhotoUseCase.kt
@Singleton
class CapturePhotoUseCase @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val imageProcessor: ImageProcessor,
    private val storageManager: PhotoStorageManager
) {
    suspend operator fun invoke(
        checkItemId: String,
        imageUri: Uri,
        captureSettings: CaptureSettings = CaptureSettings.default()
    ): PhotoResult {
        return try {
            // 1. Process captured image
            val processedImage = imageProcessor.processNewPhoto(
                sourceUri = imageUri,
                settings = captureSettings.processingSettings
            )
            
            // 2. Generate photo metadata
            val metadata = createPhotoMetadata(imageUri, captureSettings)
            
            // 3. Save to storage
            val savedPhoto = storageManager.savePhoto(
                checkItemId = checkItemId,
                processedImage = processedImage,
                metadata = metadata
            )
            
            // 4. Save to database
            photoRepository.insertPhoto(savedPhoto)
            
            PhotoResult.Success(savedPhoto)
            
        } catch (e: Exception) {
            PhotoResult.Error(e, mapToPhotoErrorType(e))
        }
    }
    
    private fun createPhotoMetadata(
        imageUri: Uri,
        settings: CaptureSettings
    ): PhotoMetadata {
        return PhotoMetadata(
            originalFileName = imageUri.lastPathSegment ?: "photo_${System.currentTimeMillis()}.jpg",
            captureTimestamp = LocalDateTime.now(),
            deviceInfo = "${Build.MODEL} - QReport v1.0",
            cameraSettings = settings.cameraSettings
        )
    }
}

// domain/usecase/photo/GetCheckItemPhotosUseCase.kt
@Singleton
class GetCheckItemPhotosUseCase @Inject constructor(
    private val photoRepository: PhotoRepository
) {
    operator fun invoke(checkItemId: String): Flow<List<Photo>> {
        return photoRepository.getPhotosByCheckItemId(checkItemId)
    }
}

// domain/usecase/photo/DeletePhotoUseCase.kt
@Singleton
class DeletePhotoUseCase @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val storageManager: PhotoStorageManager
) {
    suspend operator fun invoke(photoId: String): Boolean {
        return try {
            val photo = photoRepository.getPhotoById(photoId) ?: return false
            
            // Delete from storage
            storageManager.deletePhoto(photo)
            
            // Delete from database
            photoRepository.deletePhoto(photoId)
            
            true
        } catch (e: Exception) {
            false
        }
    }
}

data class CaptureSettings(
    val processingSettings: ImageProcessingSettings,
    val cameraSettings: CameraSettings
) {
    companion object {
        fun default() = CaptureSettings(
            processingSettings = ImageProcessingSettings.default(),
            cameraSettings = CameraSettings(
                resolution = "AUTO",
                flashMode = "AUTO",
                focusMode = "AUTO",
                whiteBalance = "AUTO"
            )
        )
    }
}
```

### 3.3 Repository Interface

```kotlin
// domain/repository/PhotoRepository.kt
interface PhotoRepository {
    suspend fun insertPhoto(photo: Photo): Long
    suspend fun updatePhoto(photo: Photo)
    suspend fun deletePhoto(photoId: String)
    suspend fun getPhotoById(photoId: String): Photo?
    fun getPhotosByCheckItemId(checkItemId: String): Flow<List<Photo>>
    fun getAllPhotos(): Flow<List<Photo>>
    suspend fun updatePhotoCaption(photoId: String, caption: String)
    suspend fun getPhotosCount(): Int
    suspend fun getTotalPhotosSize(): Long
}
```

---

## 4. CAMERA INTEGRATION

### 4.1 Camera Controller

```kotlin
// presentation/camera/CameraController.kt
@Singleton
class CameraController @Inject constructor(
    private val context: Context
) {
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    
    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState = _cameraState.asStateFlow()
    
    suspend fun initializeCamera(lifecycleOwner: LifecycleOwner): Boolean {
        return try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).await()
            this.cameraProvider = cameraProvider
            
            setupCamera(cameraProvider, lifecycleOwner)
            _cameraState.value = _cameraState.value.copy(isInitialized = true)
            
            true
        } catch (e: Exception) {
            _cameraState.value = _cameraState.value.copy(
                error = "Errore inizializzazione camera: ${e.message}"
            )
            false
        }
    }
    
    private fun setupCamera(
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner
    ) {
        // Preview use case
        val preview = Preview.Builder()
            .setTargetResolution(Size(1080, 1920)) // Portrait 16:9
            .build()
        
        // Image capture use case
        imageCapture = ImageCapture.Builder()
            .setTargetResolution(Size(1080, 1920))
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(95) // High quality for processing
            .build()
        
        // Camera selector (back camera)
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        try {
            // Unbind previous use cases
            cameraProvider.unbindAll()
            
            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            
            // Setup camera controls
            setupCameraControls()
            
        } catch (e: Exception) {
            _cameraState.value = _cameraState.value.copy(
                error = "Errore binding camera: ${e.message}"
            )
        }
    }
    
    private fun setupCameraControls() {
        camera?.let { camera ->
            val cameraControl = camera.cameraControl
            val cameraInfo = camera.cameraInfo
            
            // Flash control
            _cameraState.value = _cameraState.value.copy(
                hasFlash = cameraInfo.hasFlashUnit(),
                flashMode = ImageCapture.FLASH_MODE_AUTO
            )
            
            // Zoom control
            val zoomState = cameraInfo.zoomState.value
            _cameraState.value = _cameraState.value.copy(
                zoomRatio = zoomState?.zoomRatio ?: 1.0f,
                maxZoomRatio = zoomState?.maxZoomRatio ?: 1.0f
            )
        }
    }
    
    suspend fun capturePhoto(): Uri? {
        val imageCapture = imageCapture ?: return null
        
        return try {
            _cameraState.value = _cameraState.value.copy(isCapturing = true)
            
            // Create output file
            val photoFile = createTempPhotoFile()
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            
            // Capture image
            val result = imageCapture.takePicture(outputFileOptions).await()
            
            _cameraState.value = _cameraState.value.copy(
                isCapturing = false,
                lastCapturedPhoto = Uri.fromFile(photoFile)
            )
            
            Uri.fromFile(photoFile)
            
        } catch (e: Exception) {
            _cameraState.value = _cameraState.value.copy(
                isCapturing = false,
                error = "Errore scatto foto: ${e.message}"
            )
            null
        }
    }
    
    fun toggleFlash() {
        val currentMode = _cameraState.value.flashMode
        val newMode = when (currentMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }
        
        imageCapture?.flashMode = newMode
        _cameraState.value = _cameraState.value.copy(flashMode = newMode)
    }
    
    fun setZoom(zoomRatio: Float) {
        camera?.cameraControl?.setZoomRatio(zoomRatio)
        _cameraState.value = _cameraState.value.copy(zoomRatio = zoomRatio)
    }
    
    fun focusOnTap(meteringPoint: MeteringPoint) {
        val focusAction = FocusMeteringAction.Builder(meteringPoint).build()
        camera?.cameraControl?.startFocusAndMetering(focusAction)
    }
    
    private fun createTempPhotoFile(): File {
        val photoDir = File(context.cacheDir, "temp_photos")
        photoDir.mkdirs()
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(photoDir, "TEMP_$timestamp.jpg")
    }
    
    fun shutdown() {
        cameraProvider?.unbindAll()
        _cameraState.value = CameraState()
    }
}

// Suspend extension for ImageCapture
suspend fun ImageCapture.takePicture(
    outputFileOptions: ImageCapture.OutputFileOptions
): ImageCapture.OutputFileResults {
    return suspendCancellableCoroutine { continuation ->
        takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(outputFileOptions.file?.parentFile?.let { 
                it.context 
            } ?: return@suspendCancellableCoroutine),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    continuation.resume(output)
                }
                
                override fun onError(exception: ImageCaptureException) {
                    continuation.resumeWithException(exception)
                }
            }
        )
    }
}

data class CameraState(
    val isInitialized: Boolean = false,
    val isCapturing: Boolean = false,
    val hasFlash: Boolean = false,
    val flashMode: Int = ImageCapture.FLASH_MODE_AUTO,
    val zoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 1.0f,
    val lastCapturedPhoto: Uri? = null,
    val error: String? = null
)
```

### 4.2 Camera UI Components

```kotlin
// presentation/camera/CameraScreen.kt
@Composable
fun CameraScreen(
    checkItemId: String,
    onPhotoCapture: (Photo) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    LaunchedEffect(Unit) {
        viewModel.initializeCamera(lifecycleOwner)
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Camera preview
        CameraPreview(
            onPreviewReady = { previewView ->
                viewModel.bindPreview(previewView)
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Camera controls overlay
        CameraControlsOverlay(
            uiState = uiState,
            onCapturePhoto = { 
                viewModel.capturePhoto(checkItemId) { photo ->
                    onPhotoCapture(photo)
                }
            },
            onToggleFlash = viewModel::toggleFlash,
            onZoomChange = viewModel::setZoom,
            onNavigateBack = onNavigateBack,
            modifier = Modifier.fillMaxSize()
        )
        
        // Loading overlay
        if (uiState.isCapturing) {
            CaptureLoadingOverlay(
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Error handling
        uiState.error?.let { error ->
            ErrorDialog(
                error = error,
                onDismiss = { viewModel.clearError() }
            )
        }
    }
}

@Composable
fun CameraPreview(
    onPreviewReady: (PreviewView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                onPreviewReady(this)
            }
        },
        modifier = modifier
    )
}

@Composable
fun CameraControlsOverlay(
    uiState: CameraUiState,
    onCapturePhoto: () -> Unit,
    onToggleFlash: () -> Unit,
    onZoomChange: (Float) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Top bar with back button and flash toggle
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Indietro",
                    tint = Color.White
                )
            }
            
            if (uiState.hasFlash) {
                IconButton(
                    onClick = onToggleFlash,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = when (uiState.flashMode) {
                            ImageCapture.FLASH_MODE_ON -> Icons.Filled.Flash
                            ImageCapture.FLASH_MODE_OFF -> Icons.Filled.FlashOff
                            else -> Icons.Filled.FlashAuto
                        },
                        contentDescription = "Flash",
                        tint = Color.White
                    )
                }
            }
        }
        
        // Zoom slider
        if (uiState.maxZoomRatio > 1.0f) {
            ZoomSlider(
                currentZoom = uiState.zoomRatio,
                maxZoom = uiState.maxZoomRatio,
                onZoomChange = onZoomChange,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .height(200.dp)
            )
        }
        
        // Bottom capture button
        CaptureButton(
            isCapturing = uiState.isCapturing,
            onCapture = onCapturePhoto,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

@Composable
fun CaptureButton(
    isCapturing: Boolean,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedSize by animateDpAsState(
        targetValue = if (isCapturing) 60.dp else 80.dp,
        animationSpec = tween(150),
        label = "capture_button_size"
    )
    
    Box(
        modifier = modifier
            .size(100.dp)
            .background(
                Color.White.copy(alpha = 0.3f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onCapture,
            enabled = !isCapturing,
            modifier = Modifier.size(animatedSize),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                disabledContainerColor = Color.Gray
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = "Scatta foto",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ZoomSlider(
    currentZoom: Float,
    maxZoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${currentZoom.roundToInt()}x",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall
        )
        
        Spacer(height = 8.dp)
        
        Slider(
            value = currentZoom,
            onValueChange = onZoomChange,
            valueRange = 1.0f..maxZoom,
            modifier = Modifier
                .height(120.dp)
                .graphicsLayer {
                    rotationZ = -90f
                },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.8f),
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
    }
}
```

---

## 5. STORAGE MANAGEMENT

### 5.1 Photo Storage Manager

```kotlin
// data/storage/PhotoStorageManager.kt
@Singleton
class PhotoStorageManager @Inject constructor(
    private val context: Context,
    private val imageProcessor: ImageProcessor
) {
    
    private val photoBaseDir = File(context.filesDir, "photos")
    private val thumbnailDir = File(photoBaseDir, "thumbnails")
    
    init {
        photoBaseDir.mkdirs()
        thumbnailDir.mkdirs()
    }
    
    suspend fun savePhoto(
        checkItemId: String,
        processedImage: ProcessedImage,
        metadata: PhotoMetadata
    ): Photo = withContext(Dispatchers.IO) {
        
        // Create checkup-specific directory
        val checkupDir = File(photoBaseDir, checkItemId)
        checkupDir.mkdirs()
        
        // Generate unique filename
        val timestamp = System.currentTimeMillis()
        val fileName = "photo_${timestamp}.jpg"
        val photoFile = File(checkupDir, fileName)
        
        // Save main photo
        FileOutputStream(photoFile).use { outputStream ->
            outputStream.write(processedImage.data)
        }
        
        // Generate and save thumbnail
        val thumbnailFile = generateThumbnail(processedImage, fileName)
        
        // Create Photo entity
        Photo(
            checkItemId = checkItemId,
            filePath = photoFile.absolutePath,
            fileName = fileName,
            fileSize = photoFile.length(),
            width = processedImage.width,
            height = processedImage.height,
            compressionQuality = processedImage.quality,
            thumbnailPath = thumbnailFile.absolutePath,
            timestamp = metadata.captureTimestamp
        )
    }
    
    private suspend fun generateThumbnail(
        processedImage: ProcessedImage,
        originalFileName: String
    ): File = withContext(Dispatchers.IO) {
        
        val thumbnailFile = File(thumbnailDir, "thumb_$originalFileName")
        
        // Create bitmap from processed image data
        val originalBitmap = BitmapFactory.decodeByteArray(processedImage.data, 0, processedImage.data.size)
        
        // Create thumbnail
        val thumbnailBitmap = imageProcessor.createThumbnail(
            originalBitmap,
            THUMBNAIL_SIZE,
            THUMBNAIL_SIZE
        )
        
        // Save thumbnail
        FileOutputStream(thumbnailFile).use { outputStream ->
            thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        }
        
        // Cleanup
        originalBitmap.recycle()
        thumbnailBitmap.recycle()
        
        thumbnailFile
    }
    
    suspend fun deletePhoto(photo: Photo): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete main photo
            val photoFile = File(photo.filePath)
            val photoDeleted = if (photoFile.exists()) photoFile.delete() else true
            
            // Delete thumbnail
            val thumbnailDeleted = photo.thumbnailPath?.let { thumbnailPath ->
                val thumbnailFile = File(thumbnailPath)
                if (thumbnailFile.exists()) thumbnailFile.delete() else true
            } ?: true
            
            photoDeleted && thumbnailDeleted
            
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun movePhotoToExport(photo: Photo): File? = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.getExternalFilesDir(null), "QReport/Photos")
            exportDir.mkdirs()
            
            val sourceFile = File(photo.filePath)
            val destFile = File(exportDir, photo.fileName)
            
            sourceFile.copyTo(destFile, overwrite = true)
            destFile
            
        } catch (e: Exception) {
            null
        }
    }
    
    fun getStorageStats(): StorageStats {
        val totalSize = calculateDirectorySize(photoBaseDir)
        val photoCount = countPhotosInDirectory(photoBaseDir)
        val availableSpace = photoBaseDir.freeSpace
        
        return StorageStats(
            totalPhotoSize = totalSize,
            photoCount = photoCount,
            availableSpace = availableSpace,
            storageDirectory = photoBaseDir.absolutePath
        )
    }
    
    suspend fun cleanupTempFiles() = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "temp_photos")
        if (tempDir.exists()) {
            tempDir.listFiles()?.forEach { file ->
                if (System.currentTimeMillis() - file.lastModified() > TEMP_FILE_MAX_AGE) {
                    file.delete()
                }
            }
        }
    }
    
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        directory.walkTopDown().forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }
    
    private fun countPhotosInDirectory(directory: File): Int {
        return directory.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in listOf("jpg", "jpeg", "png") }
            .count()
    }
    
    companion object {
        private const val THUMBNAIL_SIZE = 150
        private const val TEMP_FILE_MAX_AGE = 24 * 60 * 60 * 1000L // 24 hours
    }
}

data class StorageStats(
    val totalPhotoSize: Long,
    val photoCount: Int,
    val availableSpace: Long,
    val storageDirectory: String
)

data class ProcessedImage(
    val data: ByteArray,
    val width: Int,
    val height: Int,
    val quality: Int
)
```

### 5.2 Storage Optimization

```kotlin
// data/storage/StorageOptimizer.kt
@Singleton
class StorageOptimizer @Inject constructor(
    private val context: Context,
    private val storageManager: PhotoStorageManager
) {
    
    suspend fun optimizeStorage(): StorageOptimizationResult = withContext(Dispatchers.IO) {
        val initialStats = storageManager.getStorageStats()
        
        var spaceSaved = 0L
        var photosProcessed = 0
        val optimizationActions = mutableListOf<String>()
        
        // 1. Cleanup temp files
        storageManager.cleanupTempFiles()
        optimizationActions.add("Pulizia file temporanei")
        
        // 2. Check for duplicate photos
        val duplicatesRemoved = removeDuplicatePhotos()
        spaceSaved += duplicatesRemoved.spaceSaved
        photosProcessed += duplicatesRemoved.photosRemoved
        if (duplicatesRemoved.photosRemoved > 0) {
            optimizationActions.add("Rimosse ${duplicatesRemoved.photosRemoved} foto duplicate")
        }
        
        // 3. Compress oversized photos
        val compressionResult = compressOversizedPhotos()
        spaceSaved += compressionResult.spaceSaved
        photosProcessed += compressionResult.photosProcessed
        if (compressionResult.photosProcessed > 0) {
            optimizationActions.add("Compresse ${compressionResult.photosProcessed} foto")
        }
        
        val finalStats = storageManager.getStorageStats()
        
        StorageOptimizationResult(
            spaceSavedBytes = spaceSaved,
            photosProcessed = photosProcessed,
            initialStorageSize = initialStats.totalPhotoSize,
            finalStorageSize = finalStats.totalPhotoSize,
            optimizationActions = optimizationActions
        )
    }
    
    private suspend fun removeDuplicatePhotos(): DuplicateRemovalResult = withContext(Dispatchers.IO) {
        // Implementation for detecting and removing duplicate photos
        // This would involve comparing file hashes or image similarity
        // For now, returning empty result
        DuplicateRemovalResult(0, 0L)
    }
    
    private suspend fun compressOversizedPhotos(): CompressionResult = withContext(Dispatchers.IO) {
        val maxFileSize = 2 * 1024 * 1024L // 2MB
        var spaceSaved = 0L
        var photosProcessed = 0
        
        // Find photos larger than max size
        val oversizedPhotos = findOversizedPhotos(maxFileSize)
        
        oversizedPhotos.forEach { photo ->
            try {
                val originalSize = File(photo.filePath).length()
                val compressed = compressPhoto(photo, 70) // Lower quality for oversized photos
                
                if (compressed != null) {
                    val newSize = File(compressed.filePath).length()
                    spaceSaved += (originalSize - newSize)
                    photosProcessed++
                }
            } catch (e: Exception) {
                // Log error but continue with other photos
            }
        }
        
        CompressionResult(photosProcessed, spaceSaved)
    }
    
    private fun findOversizedPhotos(maxSize: Long): List<Photo> {
        // This would query the database for photos with fileSize > maxSize
        // For now, returning empty list
        return emptyList()
    }
    
    private suspend fun compressPhoto(photo: Photo, targetQuality: Int): Photo? {
        // Implementation for re-compressing a photo with lower quality
        // This would involve loading the photo, re-compressing it, and updating the database
        return null
    }
    
    fun shouldOptimizeStorage(): Boolean {
        val stats = storageManager.getStorageStats()
        val availableSpaceGB = stats.availableSpace / (1024 * 1024 * 1024)
        val totalPhotoSizeMB = stats.totalPhotoSize / (1024 * 1024)
        
        return availableSpaceGB < 1 || totalPhotoSizeMB > 500 // Optimize if <1GB free or >500MB photos
    }
}

data class StorageOptimizationResult(
    val spaceSavedBytes: Long,
    val photosProcessed: Int,
    val initialStorageSize: Long,
    val finalStorageSize: Long,
    val optimizationActions: List<String>
)

data class DuplicateRemovalResult(
    val photosRemoved: Int,
    val spaceSaved: Long
)

data class CompressionResult(
    val photosProcessed: Int,
    val spaceSaved: Long
)
```

---

## 6. IMAGE PROCESSING

### 6.1 Image Processor

```kotlin
// data/image/ImageProcessor.kt
@Singleton
class ImageProcessor @Inject constructor(
    private val context: Context
) {
    
    suspend fun processNewPhoto(
        sourceUri: Uri,
        settings: ImageProcessingSettings
    ): ProcessedImage = withContext(Dispatchers.IO) {
        
        // Load original bitmap
        val originalBitmap = loadBitmapFromUri(sourceUri)
            ?: throw IllegalArgumentException("Cannot load image from URI")
        
        try {
            // Apply processing pipeline
            var processedBitmap = originalBitmap
            
            // 1. Orientation correction
            processedBitmap = correctOrientation(processedBitmap, sourceUri)
            
            // 2. Resolution optimization
            if (settings.optimizeResolution) {
                processedBitmap = optimizeResolution(processedBitmap, settings.maxResolution)
            }
            
            // 3. Quality enhancement (optional)
            if (settings.enhanceQuality) {
                processedBitmap = enhanceImageQuality(processedBitmap)
            }
            
            // 4. Watermark (if enabled)
            if (settings.addWatermark) {
                processedBitmap = addWatermark(processedBitmap, settings.watermarkText)
            }
            
            // 5. Compress to byte array
            val outputStream = ByteArrayOutputStream()
            processedBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                settings.compressionQuality,
                outputStream
            )
            
            ProcessedImage(
                data = outputStream.toByteArray(),
                width = processedBitmap.width,
                height = processedBitmap.height,
                quality = settings.compressionQuality
            )
            
        } finally {
            // Cleanup bitmaps
            if (originalBitmap != processedBitmap) {
                originalBitmap.recycle()
            }
            processedBitmap.recycle()
        }
    }
    
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // First, get image dimensions without loading full bitmap
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                
                // Calculate sample size for memory efficiency
                options.inSampleSize = calculateInSampleSize(options, 2048, 2048)
                options.inJustDecodeBounds = false
                
                // Load bitmap with sample size
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun correctOrientation(bitmap: Bitmap, sourceUri: Uri): Bitmap {
        return try {
            val exif = context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                ExifInterface(inputStream)
            }
            
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap // No rotation needed
            }
            
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            
        } catch (e: Exception) {
            bitmap // Return original if orientation correction fails
        }
    }
    
    private fun optimizeResolution(bitmap: Bitmap, maxResolution: Int): Bitmap {
        val maxDimension = maxOf(bitmap.width, bitmap.height)
        
        if (maxDimension <= maxResolution) {
            return bitmap
        }
        
        val scale = maxResolution.toFloat() / maxDimension
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    private fun enhanceImageQuality(bitmap: Bitmap): Bitmap {
        // Basic image enhancement using ColorMatrix
        val colorMatrix = ColorMatrix().apply {
            // Slight contrast and brightness boost
            setSaturation(1.1f) // 10% more saturation
            postConcat(ColorMatrix().apply {
                setScale(1.05f, 1.05f, 1.05f, 1.0f) // 5% brighter
            })
        }
        
        val enhancedBitmap = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height,
            bitmap.config ?: Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(enhancedBitmap)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return enhancedBitmap
    }
    
    private fun addWatermark(bitmap: Bitmap, watermarkText: String): Bitmap {
        val result = bitmap.copy(bitmap.config, true)
        val canvas = Canvas(result)
        
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = bitmap.width * 0.03f // 3% of image width
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
            alpha = 180
            isAntiAlias = true
        }
        
        // Add timestamp
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val fullText = "$watermarkText - $timestamp"
        
        val textBounds = Rect()
        paint.getTextBounds(fullText, 0, fullText.length, textBounds)
        
        // Position in bottom-right corner with padding
        val x = bitmap.width - textBounds.width() - 20f
        val y = bitmap.height - 20f
        
        canvas.drawText(fullText, x, y, paint)
        
        return result
    }
    
    fun createThumbnail(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        // Create thumbnail maintaining aspect ratio
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val targetAspectRatio = width.toFloat() / height.toFloat()
        
        val (thumbnailWidth, thumbnailHeight) = if (aspectRatio > targetAspectRatio) {
            // Crop height
            width to (width / aspectRatio).toInt()
        } else {
            // Crop width
            (height * aspectRatio).toInt() to height
        }
        
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, thumbnailWidth, thumbnailHeight, true)
        
        // Create final thumbnail with exact dimensions (centered crop)
        val thumbnail = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(thumbnail)
        
        val offsetX = (width - thumbnailWidth) / 2f
        val offsetY = (height - thumbnailHeight) / 2f
        
        canvas.drawBitmap(scaledBitmap, offsetX, offsetY, null)
        
        scaledBitmap.recycle()
        
        return thumbnail
    }
    
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
}

data class ImageProcessingSettings(
    val optimizeResolution: Boolean = true,
    val maxResolution: Int = 1920,
    val compressionQuality: Int = 85,
    val enhanceQuality: Boolean = false,
    val addWatermark: Boolean = true,
    val watermarkText: String = "QReport"
) {
    companion object {
        fun default() = ImageProcessingSettings()
        
        fun highQuality() = ImageProcessingSettings(
            maxResolution = 2560,
            compressionQuality = 95,
            enhanceQuality = true
        )
        
        fun storageOptimized() = ImageProcessingSettings(
            maxResolution = 1280,
            compressionQuality = 75,
            enhanceQuality = false
        )
    }
}
```

---

## 7. GALLERY & PREVIEW

### 7.1 Photo Gallery Components

```kotlin
// presentation/gallery/PhotoGalleryComponents.kt
@Composable
fun PhotoGallery(
    checkItemId: String,
    onPhotoAdd: () -> Unit,
    onPhotoSelect: (Photo) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhotoGalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(checkItemId) {
        viewModel.loadPhotos(checkItemId)
    }
    
    Column(modifier = modifier) {
        // Gallery header with add button
        PhotoGalleryHeader(
            photoCount = uiState.photos.size,
            onAddPhoto = onPhotoAdd,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(height = 8.dp)
        
        // Photo grid
        when {
            uiState.isLoading -> {
                PhotoGalleryLoadingSkeleton()
            }
            uiState.photos.isEmpty() -> {
                EmptyPhotoGallery(onAddPhoto = onPhotoAdd)
            }
            else -> {
                PhotoGrid(
                    photos = uiState.photos,
                    onPhotoClick = onPhotoSelect,
                    onPhotoLongPress = { photo ->
                        viewModel.showPhotoOptions(photo)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Photo options bottom sheet
        if (uiState.showPhotoOptions && uiState.selectedPhoto != null) {
            PhotoOptionsBottomSheet(
                photo = uiState.selectedPhoto,
                onDismiss = { viewModel.hidePhotoOptions() },
                onEdit = { viewModel.editPhoto(it) },
                onDelete = { viewModel.deletePhoto(it) },
                onShare = { viewModel.sharePhoto(it) }
            )
        }
    }
}

@Composable
fun PhotoGalleryHeader(
    photoCount: Int,
    onAddPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Foto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (photoCount > 0) {
                Text(
                    text = "$photoCount ${if (photoCount == 1) "foto" else "foto"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        OutlinedButton(
            onClick = onAddPhoto,
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) {
            Icon(
                Icons.Outlined.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(width = 8.dp)
            Text(
                text = "Aggiungi",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun PhotoGrid(
    photos: List<Photo>,
    onPhotoClick: (Photo) -> Unit,
    onPhotoLongPress: (Photo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(photos) { photo ->
            PhotoThumbnail(
                photo = photo,
                onClick = { onPhotoClick(photo) },
                onLongPress = { onPhotoLongPress(photo) },
                modifier = Modifier.aspectRatio(1f)
            )
        }
    }
}

@Composable
fun PhotoThumbnail(
    photo: Photo,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photo.thumbnailPath ?: photo.filePath)
                    .crossfade(true)
                    .build(),
                contentDescription = "Foto ${photo.fileName}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_photo_placeholder),
                error = painterResource(R.drawable.ic_photo_error)
            )
            
            // Caption overlay if present
            if (photo.caption.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                        .padding(4.dp)
                ) {
                    Text(
                        text = photo.caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Timestamp badge
            Text(
                text = photo.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(bottomStart = 4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
fun EmptyPhotoGallery(
    onAddPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(height = 16.dp)
        
        Text(
            text = "Nessuna foto",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Aggiungi foto per documentare questo controllo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(height = 24.dp)
        
        FilledTonalButton(
            onClick = onAddPhoto,
            modifier = Modifier.height(40.dp)
        ) {
            Icon(
                Icons.Outlined.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(width = 8.dp)
            Text("Scatta prima foto")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoOptionsBottomSheet(
    photo: Photo,
    onDismiss: () -> Unit,
    onEdit: (Photo) -> Unit,
    onDelete: (Photo) -> Unit,
    onShare: (Photo) -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomSheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Photo preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = photo.thumbnailPath ?: photo.filePath,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(width = 12.dp)
                
                Column {
                    Text(
                        text = photo.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = photo.timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(photo.fileSize / 1024).toInt()} KB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(height = 16.dp)
            
            // Action buttons
            PhotoOptionButton(
                icon = Icons.Outlined.Edit,
                text = "Modifica caption",
                onClick = { onEdit(photo) }
            )
            
            PhotoOptionButton(
                icon = Icons.Outlined.Share,
                text = "Condividi",
                onClick = { onShare(photo) }
            )
            
            PhotoOptionButton(
                icon = Icons.Outlined.Delete,
                text = "Elimina",
                isDestructive = true,
                onClick = { onDelete(photo) }
            )
            
            Spacer(height = 16.dp)
        }
    }
}

@Composable
fun PhotoOptionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (isDestructive) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(width = 16.dp)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
        }
    }
}
```

### 7.2 Full Screen Photo Viewer

```kotlin
// presentation/gallery/FullScreenPhotoViewer.kt
@Composable
fun FullScreenPhotoViewer(
    photo: Photo,
    onNavigateBack: () -> Unit,
    onEditCaption: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCaptionEditor by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var currentCaption by remember { mutableStateOf(photo.caption) }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Full screen photo
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.filePath)
                .crossfade(true)
                .build(),
            contentDescription = "Foto ${photo.fileName}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        // Top bar overlay
        PhotoViewerTopBar(
            onNavigateBack = onNavigateBack,
            onShowOptions = { /* Show options menu */ },
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Bottom info overlay
        PhotoViewerBottomBar(
            photo = photo,
            onEditCaption = { showCaptionEditor = true },
            onDelete = { showDeleteConfirmation = true },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        
        // Caption editor dialog
        if (showCaptionEditor) {
            CaptionEditorDialog(
                initialCaption = currentCaption,
                onSave = { newCaption ->
                    currentCaption = newCaption
                    onEditCaption(newCaption)
                    showCaptionEditor = false
                },
                onDismiss = { showCaptionEditor = false }
            )
        }
        
        // Delete confirmation dialog
        if (showDeleteConfirmation) {
            DeletePhotoConfirmationDialog(
                onConfirm = {
                    onDelete()
                    showDeleteConfirmation = false
                },
                onDismiss = { showDeleteConfirmation = false }
            )
        }
    }
}

@Composable
fun PhotoViewerTopBar(
    onNavigateBack: () -> Unit,
    onShowOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.6f),
                        Color.Transparent
                    )
                )
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Indietro",
                tint = Color.White
            )
        }
        
        IconButton(onClick = onShowOptions) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "Opzioni",
                tint = Color.White
            )
        }
    }
}

@Composable
fun PhotoViewerBottomBar(
    photo: Photo,
    onEditCaption: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(16.dp)
    ) {
        // Photo info
        Text(
            text = photo.timestamp.format(DateTimeFormatter.ofPattern("dd MMMM yyyy • HH:mm")),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
        
        if (photo.caption.isNotBlank()) {
            Spacer(height = 8.dp)
            Text(
                text = photo.caption,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }
        
        Spacer(height = 16.dp)
        
        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onEditCaption,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f))
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(width = 8.dp)
                Text("Caption")
            }
            
            OutlinedButton(
                onClick = onDelete,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Red.copy(alpha = 0.9f)
                ),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.7f))
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(width = 8.dp)
                Text("Elimina")
            }
        }
    }
}

@Composable
fun CaptionEditorDialog(
    initialCaption: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var caption by remember { mutableStateOf(initialCaption) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica Caption") },
        text = {
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption foto") },
                placeholder = { Text("Descrivi questa foto...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(caption) }
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
fun DeletePhotoConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elimina foto") },
        text = { Text("Sei sicuro di voler eliminare questa foto? L'azione non può essere annullata.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Elimina")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
```

---

## 8. PERFORMANCE OPTIMIZATION

### 8.1 Memory Management

```kotlin
// data/image/ImageMemoryManager.kt
@Singleton
class ImageMemoryManager @Inject constructor() {
    
    private val maxMemoryCache = (Runtime.getRuntime().maxMemory() / 8).toInt() // 1/8 of available memory
    private val bitmapCache = LruCache<String, Bitmap>(maxMemoryCache)
    
    fun getBitmap(key: String): Bitmap? {
        return bitmapCache.get(key)
    }
    
    fun putBitmap(key: String, bitmap: Bitmap) {
        if (getBitmap(key) == null) {
            bitmapCache.put(key, bitmap)
        }
    }
    
    fun clearCache() {
        bitmapCache.evictAll()
    }
    
    fun getMemoryUsage(): MemoryUsage {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        
        return MemoryUsage(
            maxMemory = maxMemory,
            usedMemory = usedMemory,
            availableMemory = maxMemory - usedMemory,
            cacheSize = bitmapCache.size(),
            cacheSizeBytes = bitmapCache.snapShot().values.sumOf { bitmap ->
                bitmap.allocationByteCount.toLong()
            }
        )
    }
    
    fun optimizeMemory() {
        // Clear cache if memory usage is high
        val memoryUsage = getMemoryUsage()
        val usagePercentage = (memoryUsage.usedMemory.toFloat() / memoryUsage.maxMemory) * 100
        
        if (usagePercentage > 80) {
            clearCache()
            System.gc()
        }
    }
}

data class MemoryUsage(
    val maxMemory: Long,
    val usedMemory: Long,
    val availableMemory: Long,
    val cacheSize: Int,
    val cacheSizeBytes: Long
)
```

### 8.2 Async Loading

```kotlin
// presentation/image/AsyncImageLoader.kt
@Composable
fun OptimizedAsyncImage(
    photoPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Painter? = null,
    error: Painter? = null
) {
    val context = LocalContext.current
    
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(photoPath)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .size(Size.ORIGINAL) // Let Coil handle sizing
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = placeholder,
        error = error,
        onSuccess = { result ->
            // Log successful load for debugging
        },
        onError = { error ->
            // Log error for debugging
        }
    )
}

// Custom image loading for thumbnails
@Composable
fun ThumbnailImage(
    photo: Photo,
    size: Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    val thumbnailPath = photo.thumbnailPath ?: photo.filePath
    
    OptimizedAsyncImage(
        photoPath = thumbnailPath,
        contentDescription = "Thumbnail ${photo.fileName}",
        modifier = modifier.size(size),
        contentScale = ContentScale.Crop,
        placeholder = painterResource(R.drawable.ic_photo_placeholder),
        error = painterResource(R.drawable.ic_photo_error)
    )
}
```

---

## 9. DATA PERSISTENCE

### 9.1 Photo Database Entity

```kotlin
// data/local/entity/PhotoEntity.kt
@Entity(
    tableName = "photos",
    indices = [
        Index(value = ["check_item_id"]),
        Index(value = ["timestamp"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CheckItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["check_item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PhotoEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "check_item_id")
    val checkItemId: String,
    
    @ColumnInfo(name = "file_path")
    val filePath: String,
    
    @ColumnInfo(name = "file_name")
    val fileName: String,
    
    val caption: String,
    
    val timestamp: Long, // Unix timestamp
    
    @ColumnInfo(name = "file_size")
    val fileSize: Long,
    
    val width: Int,
    
    val height: Int,
    
    @ColumnInfo(name = "compression_quality")
    val compressionQuality: Int,
    
    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String?
)

// Mappers
fun PhotoEntity.toDomain(): Photo = Photo(
    id = id,
    checkItemId = checkItemId,
    filePath = filePath,
    fileName = fileName,
    caption = caption,
    timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()),
    fileSize = fileSize,
    width = width,
    height = height,
    compressionQuality = compressionQuality,
    thumbnailPath = thumbnailPath
)

fun Photo.toEntity(): PhotoEntity = PhotoEntity(
    id = id,
    checkItemId = checkItemId,
    filePath = filePath,
    fileName = fileName,
    caption = caption,
    timestamp = timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    fileSize = fileSize,
    width = width,
    height = height,
    compressionQuality = compressionQuality,
    thumbnailPath = thumbnailPath
)
```

### 9.2 Photo DAO

```kotlin
// data/local/dao/PhotoDao.kt
@Dao
interface PhotoDao {
    
    @Query("SELECT * FROM photos WHERE check_item_id = :checkItemId ORDER BY timestamp DESC")
    fun getPhotosByCheckItemId(checkItemId: String): Flow<List<PhotoEntity>>
    
    @Query("SELECT * FROM photos WHERE id = :photoId")
    suspend fun getPhotoById(photoId: String): PhotoEntity?
    
    @Query("SELECT * FROM photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity): Long
    
    @Update
    suspend fun updatePhoto(photo: PhotoEntity)
    
    @Query("UPDATE photos SET caption = :caption WHERE id = :photoId")
    suspend fun updatePhotoCaption(photoId: String, caption: String)
    
    @Query("DELETE FROM photos WHERE id = :photoId")
    suspend fun deletePhoto(photoId: String)
    
    @Query("DELETE FROM photos WHERE check_item_id = :checkItemId")
    suspend fun deletePhotosByCheckItemId(checkItemId: String)
    
    @Query("SELECT COUNT(*) FROM photos")
    suspend fun getPhotosCount(): Int
    
    @Query("SELECT SUM(file_size) FROM photos")
    suspend fun getTotalPhotosSize(): Long?
    
    @Query("SELECT * FROM photos WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getPhotosByDateRange(startTime: Long, endTime: Long): List<PhotoEntity>
    
    @Query("SELECT DISTINCT check_item_id FROM photos")
    suspend fun getCheckItemIdsWithPhotos(): List<String>
}
```

### 9.3 Photo Repository Implementation

```kotlin
// data/repository/PhotoRepositoryImpl.kt
@Singleton
class PhotoRepositoryImpl @Inject constructor(
    private val photoDao: PhotoDao
) : PhotoRepository {
    
    override suspend fun insertPhoto(photo: Photo): Long {
        return photoDao.insertPhoto(photo.toEntity())
    }
    
    override suspend fun updatePhoto(photo: Photo) {
        photoDao.updatePhoto(photo.toEntity())
    }
    
    override suspend fun deletePhoto(photoId: String) {
        photoDao.deletePhoto(photoId)
    }
    
    override suspend fun getPhotoById(photoId: String): Photo? {
        return photoDao.getPhotoById(photoId)?.toDomain()
    }
    
    override fun getPhotosByCheckItemId(checkItemId: String): Flow<List<Photo>> {
        return photoDao.getPhotosByCheckItemId(checkItemId)
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    override fun getAllPhotos(): Flow<List<Photo>> {
        return photoDao.getAllPhotos()
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    override suspend fun updatePhotoCaption(photoId: String, caption: String) {
        photoDao.updatePhotoCaption(photoId, caption)
    }
    
    override suspend fun getPhotosCount(): Int {
        return photoDao.getPhotosCount()
    }
    
    override suspend fun getTotalPhotosSize(): Long {
        return photoDao.getTotalPhotosSize() ?: 0L
    }
}
```

---

## 10. TESTING STRATEGY

### 10.1 Unit Tests

```kotlin
// test/data/repository/PhotoRepositoryTest.kt
@RunWith(MockitoJUnitRunner::class)
class PhotoRepositoryTest {
    
    @Mock
    private lateinit var photoDao: PhotoDao
    
    private lateinit var repository: PhotoRepositoryImpl
    
    @Before
    fun setup() {
        repository = PhotoRepositoryImpl(photoDao)
    }
    
    @Test
    fun `insertPhoto stores photo successfully`() = runTest {
        // Given
        val photo = createTestPhoto()
        whenever(photoDao.insertPhoto(any())).thenReturn(1L)
        
        // When
        val result = repository.insertPhoto(photo)
        
        // Then
        assertThat(result).isEqualTo(1L)
        verify(photoDao).insertPhoto(photo.toEntity())
    }
    
    @Test
    fun `getPhotosByCheckItemId returns mapped photos`() = runTest {
        // Given
        val checkItemId = "test-check-item"
        val photoEntities = listOf(createTestPhotoEntity())
        whenever(photoDao.getPhotosByCheckItemId(checkItemId))
            .thenReturn(flowOf(photoEntities))
        
        // When
        val result = repository.getPhotosByCheckItemId(checkItemId).first()
        
        // Then
        assertThat(result).hasSize(1)
        assertThat(result.first().checkItemId).isEqualTo(checkItemId)
    }
    
    private fun createTestPhoto(): Photo {
        return Photo(
            id = "test-photo-id",
            checkItemId = "test-check-item",
            filePath = "/test/path/photo.jpg",
            fileName = "photo.jpg",
            caption = "Test photo",
            fileSize = 1024L,
            width = 1920,
            height = 1080,
            compressionQuality = 85
        )
    }
    
    private fun createTestPhotoEntity(): PhotoEntity {
        return createTestPhoto().toEntity()
    }
}
```

### 10.2 Integration Tests

```kotlin
// androidTest/camera/CameraIntegrationTest.kt
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class CameraIntegrationTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule
    val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    
    @Inject
    lateinit var cameraController: CameraController
    
    @Inject
    lateinit var photoRepository: PhotoRepository
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun cameraInitializationFlow() = runTest {
        // Given
        val activity = ActivityScenario.launch(MainActivity::class.java)
        
        activity.use { scenario ->
            scenario.onActivity { activity ->
                // When
                val initialized = cameraController.initializeCamera(activity)
                
                // Then
                assertThat(initialized).isTrue()
                assertThat(cameraController.cameraState.value.isInitialized).isTrue()
            }
        }
    }
    
    @Test
    fun photoCaptureAndStorageFlow() = runTest {
        // This would test the complete flow from capture to storage
        // Implementation depends on test camera setup
    }
}
```

---

## 11. IMPLEMENTATION GUIDE

### 11.1 Module Setup

```kotlin
// Step 1: Add dependencies to build.gradle.kts
dependencies {
    // CameraX
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    
    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // ExifInterface for orientation
    implementation("androidx.exifinterface:exifinterface:1.3.6")
}
```

### 11.2 Permissions Request

```kotlin
// presentation/permission/CameraPermissionHandler.kt
@Composable
fun CameraPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    )
    
    LaunchedEffect(cameraPermissionState.status) {
        when (cameraPermissionState.status) {
            is PermissionStatus.Granted -> onPermissionGranted()
            is PermissionStatus.Denied -> {
                if (cameraPermissionState.status.shouldShowRationale) {
                    // Show rationale
                } else {
                    onPermissionDenied()
                }
            }
        }
    }
    
    when (cameraPermissionState.status) {
        is PermissionStatus.Granted -> content()
        is PermissionStatus.Denied -> {
            CameraPermissionScreen(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
            )
        }
    }
}
```

### 11.3 Usage Examples

```kotlin
// ViewModel usage example
@HiltViewModel
class CheckItemDetailViewModel @Inject constructor(
    private val capturePhotoUseCase: CapturePhotoUseCase,
    private val getCheckItemPhotosUseCase: GetCheckItemPhotosUseCase
) : ViewModel() {
    
    fun addPhotoToCheckItem(checkItemId: String, imageUri: Uri) {
        viewModelScope.launch {
            val result = capturePhotoUseCase(
                checkItemId = checkItemId,
                imageUri = imageUri,
                captureSettings = CaptureSettings.default()
            )
            
            when (result) {
                is PhotoResult.Success -> {
                    // Photo saved successfully
                    _uiState.value = _uiState.value.copy(
                        showCameraSuccess = true
                    )
                }
                is PhotoResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.exception.message
                    )
                }
            }
        }
    }
}
```

---

## 🎯 SUMMARY

Il **Sistema Gestione Foto** per QReport è ora **completo e pronto per l'implementazione**:

### ✅ **Funzionalità Complete:**

- **📸 CameraX Integration** - Capture con preview, controlli manuali, flash/zoom
- **💾 Storage Management** - Organizzazione gerarchica, thumbnails, optimization
- **🖼️ Image Processing** - Compression, watermark, orientation correction, enhancement
- **📱 Gallery & Preview** - Grid view, full-screen viewer, caption editing
- **⚡ Performance Optimized** - Memory management, async loading, LRU cache
- **🗄️ Data Persistence** - Room database, entities, DAOs con relazioni
- **🧪 Testing Coverage** - Unit tests, integration tests, mock strategies

### 🚀 **Ready for Development:**

Il documento fornisce tutto per implementare il sistema photo:

1. **CameraX Setup** ✅
2. **Clean Architecture** ✅  
3. **UI Components** ✅
4. **Storage & Processing** ✅
5. **Performance Guidelines** ✅
6. **Testing Strategy** ✅

[**Visualizza il documento completo**](computer:///mnt/user-data/outputs/QReport_Photo_Management_System.md)

---

## 🎉 **QReport - Tutti i Sistemi Completi!**

Abbiamo ora **tutti e tre i documenti chiave** per QReport:

1. **🎨 UI Guidelines** - Design system e componenti Compose
2. **📄 Export Word System** - Apache POI e report professionali  
3. **📸 Photo Management** - CameraX e gestione immagini

**Il progetto QReport è ora pronto per l'implementazione completa!** 🚀✨