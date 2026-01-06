package net.calvuz.qreport.photo.presentation.ui

import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.photo.domain.model.CameraSettings
import net.calvuz.qreport.photo.domain.model.PhotoPerspective
import net.calvuz.qreport.photo.domain.model.PhotoResolution
import net.calvuz.qreport.photo.domain.model.PhotoResult
import net.calvuz.qreport.photo.domain.usecase.CapturePhotoUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Stato UI per la camera screen.
 */
data class CameraUiState(
    val isInitialized: Boolean = false,
    val isInitializing: Boolean = false,
    val isCapturing: Boolean = false,
    val hasFlash: Boolean = false,
    val flashMode: Int = 0,
    val zoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val error: String? = null,
    val captureSuccess: Boolean = false,
    val permissionGranted: Boolean = false,
    val showPermissionRationale: Boolean = false,
    val selectedPerspective: PhotoPerspective = PhotoPerspective.OVERVIEW,
    val selectedResolution: PhotoResolution = PhotoResolution.HIGH
) {
    val canCapture: Boolean get() = isInitialized && !isCapturing
    val showZoomControls: Boolean get() = maxZoomRatio > 1f
}

/**
 * ViewModel dedicato per la gestione della camera.
 * ✅ CORRETTO: Separato da PhotoViewModel per seguire Single Responsibility
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraController: CameraController,
    private val capturePhotoUseCase: CapturePhotoUseCase
) : ViewModel() {

    // ===== CAMERA STATE =====

    private val _cameraUiState = MutableStateFlow(CameraUiState())
    val cameraUiState: StateFlow<CameraUiState> = _cameraUiState.asStateFlow()

    init {
        // Ascolta lo stato della camera dal controller
        viewModelScope.launch {
            cameraController.cameraState.collect { cameraState ->
                _cameraUiState.value = _cameraUiState.value.copy(
                    isInitialized = cameraState.isInitialized,
                    isInitializing = cameraState.isInitializing,
                    isCapturing = cameraState.isCapturing,
                    hasFlash = cameraState.hasFlash,
                    flashMode = cameraState.flashMode,
                    zoomRatio = cameraState.zoomRatio,
                    maxZoomRatio = cameraState.maxZoomRatio,
                    minZoomRatio = cameraState.minZoomRatio,
                    error = cameraState.error
                )
            }
        }
    }

    // ===== CAMERA INITIALIZATION =====

    /**
     * ✅ CORREZIONE: Inizializza la camera con PreviewView.
     * Questa funzione mancava nel PhotoViewModel!
     */
    fun initializeCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        viewModelScope.launch {
            try {
                _cameraUiState.value = _cameraUiState.value.copy(isInitializing = true)

                val success = cameraController.initializeCamera(lifecycleOwner, previewView)

                if (success) {
                    Timber.d("Camera inizializzata con successo")
                } else {
                    _cameraUiState.value = _cameraUiState.value.copy(
                        error = "Impossibile inizializzare la camera"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Errore durante l'inizializzazione camera")
                _cameraUiState.value = _cameraUiState.value.copy(
                    error = "Errore inizializzazione: ${e.message}"
                )
            }
        }
    }

    // ===== CAMERA CONTROLS =====

    /**
     * Cattura una foto per il check item specificato.
     */
    fun capturePhoto(checkItemId: String, caption: String = "") {
        viewModelScope.launch {
            try {
                // Verifica che la camera sia inizializzata
                if (!_cameraUiState.value.isInitialized) {
                    _cameraUiState.value = _cameraUiState.value.copy(
                        error = "Camera non inizializzata"
                    )
                    return@launch
                }

                _cameraUiState.value = _cameraUiState.value.copy(isCapturing = true)

                val result = cameraController.capturePhoto()

                when (result) {
                    is CaptureResult.Success -> {
                        // Crea le capture settings
                        val captureSettings = CameraSettings(
                            perspective = _cameraUiState.value.selectedPerspective,
                            resolution = _cameraUiState.value.selectedResolution
                        )

                        // Salva la foto
                        savePhoto(checkItemId, result.imageUri, caption, captureSettings)
                    }
                    is CaptureResult.Error -> {
                        _cameraUiState.value = _cameraUiState.value.copy(
                            isCapturing = false,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Errore durante la cattura foto")
                _cameraUiState.value = _cameraUiState.value.copy(
                    isCapturing = false,
                    error = "Errore cattura: ${e.message}"
                )
            }
        }
    }

    /**
     * Salva la foto catturata usando il CapturePhotoUseCase.
     */
    private suspend fun savePhoto(
        checkItemId: String,
        imageUri: Uri,
        caption: String,
        cameraSettings: CameraSettings
    ) {
        try {
            val result = capturePhotoUseCase(
                checkItemId = checkItemId,
                imageUri = imageUri,
                caption = caption,
                cameraSettings = cameraSettings
            )

            when (result) {
                is PhotoResult.Success -> {
                    _cameraUiState.value = _cameraUiState.value.copy(
                        isCapturing = false,
                        captureSuccess = true,
                        error = null
                    )
                    Timber.d("Foto salvata con successo: ${result.data.id}")
                }
                is PhotoResult.Error -> {
                    _cameraUiState.value = _cameraUiState.value.copy(
                        isCapturing = false,
                        error = "Errore salvataggio: ${result.exception.message}"
                    )
                }
                is PhotoResult.Loading -> {
                    // Handle loading state
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Errore nel salvataggio foto")
            _cameraUiState.value = _cameraUiState.value.copy(
                isCapturing = false,
                error = "Errore salvataggio: ${e.message}"
            )
        }
    }

    /**
     * Imposta la modalità flash.
     */
    fun setFlashMode(flashMode: Int) {
        cameraController.setFlashMode(flashMode)
    }

    /**
     * Imposta il livello di zoom.
     */
    fun setZoomRatio(zoomRatio: Float) {
        cameraController.setZoomRatio(zoomRatio)
    }

    /**
     * Focus su un punto specifico del preview.
     */
    fun focusOnPoint(x: Float, y: Float) {
        cameraController.focusOnPoint(x, y)
    }

    /**
     * Imposta la perspective per la prossima foto.
     */
    fun setPerspective(perspective: PhotoPerspective) {
        _cameraUiState.value = _cameraUiState.value.copy(selectedPerspective = perspective)
    }

    /**
     * Imposta la resolution per la prossima foto.
     */
    fun setResolution(resolution: PhotoResolution) {
        _cameraUiState.value = _cameraUiState.value.copy(selectedResolution = resolution)
    }

    // ===== ERROR HANDLING =====

    /**
     * Pulisce gli errori dell'UI state.
     */
    fun clearError() {
        _cameraUiState.value = _cameraUiState.value.copy(error = null)
    }

    /**
     * Reset del flag di successo dopo la cattura.
     */
    fun resetCaptureSuccess() {
        _cameraUiState.value = _cameraUiState.value.copy(captureSuccess = false)
    }

    // ===== CLEANUP =====

    override fun onCleared() {
        super.onCleared()
        cameraController.release()
    }
}