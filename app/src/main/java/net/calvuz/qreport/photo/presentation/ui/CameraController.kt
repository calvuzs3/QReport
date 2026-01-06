package net.calvuz.qreport.photo.presentation.ui

import android.content.Context
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Controller per gestire CameraX e le operazioni di cattura foto.
 * Centralizza la logica della camera per essere utilizzata da diversi screen.
 */
@Singleton
class CameraController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    /**
     * Inizializza la camera e la collega al PreviewView.
     */
    suspend fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ): Boolean = suspendCancellableCoroutine { continuation ->

        _cameraState.value = _cameraState.value.copy(isInitializing = true)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                this.cameraProvider = provider

                setupCamera(provider, lifecycleOwner, previewView)

                _cameraState.value = _cameraState.value.copy(
                    isInitialized = true,
                    isInitializing = false,
                    error = null
                )

                continuation.resume(true)

            } catch (e: Exception) {
                _cameraState.value = _cameraState.value.copy(
                    isInitialized = false,
                    isInitializing = false,
                    error = "Errore inizializzazione camera: ${e.message}"
                )
                continuation.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Configura e collega i use cases della camera.
     */
    private fun setupCamera(
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        // Preview use case
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        // ImageCapture use case
        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(90)
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

    /**
     * Configura i controlli della camera (flash, zoom, focus).
     */
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
                zoomRatio = zoomState?.zoomRatio ?: 1f,
                maxZoomRatio = zoomState?.maxZoomRatio ?: 1f,
                minZoomRatio = zoomState?.minZoomRatio ?: 1f
            )
        }
    }

    /**
     * Cattura una foto e la salva nel file system.
     */
    suspend fun capturePhoto(): CaptureResult = suspendCancellableCoroutine { continuation ->

        val imageCapture = imageCapture ?: run {
            continuation.resume(CaptureResult.Error("Camera non inizializzata"))
            return@suspendCancellableCoroutine
        }

        _cameraState.value = _cameraState.value.copy(isCapturing = true)

        // Crea file di output
        val photoFile = createImageFile()
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    _cameraState.value = _cameraState.value.copy(isCapturing = false)
                    continuation.resume(CaptureResult.Success(Uri.fromFile(photoFile)))
                }

                override fun onError(exception: ImageCaptureException) {
                    _cameraState.value = _cameraState.value.copy(isCapturing = false)
                    continuation.resume(CaptureResult.Error("Errore cattura: ${exception.message}"))
                }
            }
        )
    }

    /**
     * Cambia la modalità flash.
     */
    fun setFlashMode(flashMode: Int) {
        imageCapture?.flashMode = flashMode
        _cameraState.value = _cameraState.value.copy(flashMode = flashMode)
    }

    /**
     * Imposta il livello di zoom.
     */
    fun setZoomRatio(zoomRatio: Float) {
        camera?.cameraControl?.setZoomRatio(zoomRatio)
        _cameraState.value = _cameraState.value.copy(zoomRatio = zoomRatio)
    }

    /**
     * Attiva/disattiva il tap to focus.
     */
    fun focusOnPoint(x: Float, y: Float) {
        val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point).build()

        camera?.cameraControl?.startFocusAndMetering(action)
    }

    /**
     * Libera le risorse della camera.
     */
    fun release() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        _cameraState.value = CameraState()
    }

    /**
     * Crea un file temporaneo per salvare la foto.
     */
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(null)

        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
    }
}

/**
 * Stato della camera.
 */
data class CameraState(
    val isInitialized: Boolean = false,
    val isInitializing: Boolean = false,
    val isCapturing: Boolean = false,
    val hasFlash: Boolean = false,
    val flashMode: Int = ImageCapture.FLASH_MODE_AUTO,
    val zoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val error: String? = null
)

/**
 * Risultato della cattura foto.
 */
sealed class CaptureResult {
    data class Success(val imageUri: Uri) : CaptureResult()
    data class Error(val message: String) : CaptureResult()
}

/**
 * Modalità flash disponibili.
 */
object FlashMode {
    const val OFF = ImageCapture.FLASH_MODE_OFF
    const val ON = ImageCapture.FLASH_MODE_ON
    const val AUTO = ImageCapture.FLASH_MODE_AUTO
}