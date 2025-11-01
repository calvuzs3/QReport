package net.calvuz.qreport.presentation.screen.photo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.domain.model.camera.CameraSettings
import net.calvuz.qreport.domain.model.photo.Photo
import net.calvuz.qreport.domain.model.photo.PhotoPerspective
import net.calvuz.qreport.domain.model.photo.PhotoResolution
import net.calvuz.qreport.domain.model.photo.PhotoResult
import net.calvuz.qreport.domain.usecase.photo.CapturePhotoUseCase
import net.calvuz.qreport.domain.usecase.photo.DeletePhotoUseCase
import net.calvuz.qreport.domain.usecase.photo.GetCheckItemPhotosUseCase
import net.calvuz.qreport.domain.usecase.photo.UpdatePhotoUseCase
import net.calvuz.qreport.presentation.camera.CameraController
import net.calvuz.qreport.presentation.camera.CaptureResult
import net.calvuz.qreport.presentation.model.camera.CameraUiState
import net.calvuz.qreport.presentation.screen.photo.model.PhotoGalleryUiState
import net.calvuz.qreport.presentation.model.photo.PhotoPreviewUiState
import javax.inject.Inject

/**
 * ViewModel per gestire tutte le operazioni relative alle foto.
 * Aggiornato per supportare perspective, resolution, orderIndex e altre nuove funzionalità.
 */
@HiltViewModel
class PhotoViewModel @Inject constructor(
    private val cameraController: CameraController,
    private val capturePhotoUseCase: CapturePhotoUseCase,
    private val getCheckItemPhotosUseCase: GetCheckItemPhotosUseCase,
    private val updatePhotoUseCase: UpdatePhotoUseCase,
    private val deletePhotoUseCase: DeletePhotoUseCase
) : ViewModel() {

    // ===== CAMERA STATE =====

    private val _cameraUiState = MutableStateFlow(CameraUiState())
    val cameraUiState: StateFlow<CameraUiState> = _cameraUiState.asStateFlow()

    // ===== GALLERY STATE =====

    private val _galleryUiState = MutableStateFlow(PhotoGalleryUiState())
    val galleryUiState: StateFlow<PhotoGalleryUiState> = _galleryUiState.asStateFlow()

    // ===== PREVIEW STATE =====

    private val _previewUiState = MutableStateFlow(PhotoPreviewUiState())
    val previewUiState: StateFlow<PhotoPreviewUiState> = _previewUiState.asStateFlow()

    // ===== CURRENT CHECK ITEM =====

    private val _currentCheckItemId = MutableStateFlow<String?>(null)
    val currentCheckItemId: StateFlow<String?> = _currentCheckItemId.asStateFlow()

    init {
        // Osserva lo stato della camera dal controller
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

    // ===== CAMERA FUNCTIONS =====

    /**
     * Cattura una foto e la salva per il check item corrente.
     */
    fun capturePhoto(checkItemId: String, caption: String = "") {
        viewModelScope.launch {
            try {
                val result = cameraController.capturePhoto()

                when (result) {
                    is CaptureResult.Success -> {
                        // Crea le capture settings con perspective e resolution selezionate
                        val captureSettings = CameraSettings(
                            perspective = _cameraUiState.value.selectedPerspective,
                            resolution = _cameraUiState.value.selectedResolution
                        )

                        // Salva la foto nel repository
                        savePhoto(checkItemId, result.imageUri, caption, captureSettings)
                    }
                    is CaptureResult.Error -> {
                        _cameraUiState.value = _cameraUiState.value.copy(
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _cameraUiState.value = _cameraUiState.value.copy(
                    error = "Errore durante la cattura: ${e.message}"
                )
            }
        }
    }

    /**
     * Salva una foto catturata nel repository.
     */
    private suspend fun savePhoto(
        checkItemId: String,
        imageUri: Uri,
        caption: String,
        cameraSettings: CameraSettings
    ) {
        val result = capturePhotoUseCase(
            checkItemId = checkItemId,
            imageUri = imageUri,
            caption = caption,
            cameraSettings = cameraSettings
        )

        when (result) {
            is PhotoResult.Success -> {
                _cameraUiState.value = _cameraUiState.value.copy(
                    captureSuccess = true,
                    error = null
                )
                // Ricarica le foto per aggiornare la gallery
                loadPhotos(checkItemId)
            }
            is PhotoResult.Error -> {
                _cameraUiState.value = _cameraUiState.value.copy(
                    error = "Errore nel salvataggio: ${result.exception.message}"
                )
            }
            is PhotoResult.Loading -> {
                // Handle loading state if needed
            }
        }
    }

    /**
     * Cambia la modalità flash.
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
     * Focus su un punto specifico.
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

    // ===== GALLERY FUNCTIONS =====

    /**
     * Carica le foto per un check item specifico.
     */
    fun loadPhotos(checkItemId: String) {
        _currentCheckItemId.value = checkItemId

        viewModelScope.launch {
            _galleryUiState.value = _galleryUiState.value.copy(isLoading = true)

            getCheckItemPhotosUseCase(checkItemId).collect { result ->
                when (result) {
                    is PhotoResult.Success -> {
                        val filteredPhotos = applyFilters(result.data)
                        _galleryUiState.value = _galleryUiState.value.copy(
                            photos = filteredPhotos,
                            isLoading = false,
                            error = null
                        )
                    }
                    is PhotoResult.Error -> {
                        _galleryUiState.value = _galleryUiState.value.copy(
                            isLoading = false,
                            error = result.exception.message
                        )
                    }
                    is PhotoResult.Loading -> {
                        _galleryUiState.value = _galleryUiState.value.copy(isLoading = true)
                    }
                }
            }
        }
    }

    /**
     * Applica i filtri alle foto.
     */
    private fun applyFilters(photos: List<Photo>): List<Photo> {
        var filtered = photos

        // Filtro per perspective
        _galleryUiState.value.selectedPerspective?.let { perspective ->
            filtered = filtered.filter { it.metadata.perspective == perspective }
        }

        // Filtro per resolution
        _galleryUiState.value.selectedResolution?.let { resolution ->
            filtered = filtered.filter { it.metadata.resolution == resolution }
        }

        // Filtro per search query
        if (_galleryUiState.value.searchQuery.isNotBlank()) {
            val query = _galleryUiState.value.searchQuery.lowercase()
            filtered = filtered.filter {
                it.caption.lowercase().contains(query) ||
                        it.metadata.perspective?.displayName?.lowercase()?.contains(query) == true
            }
        }

        return filtered
    }

    /**
     * Filtra le foto per perspective.
     */
    fun filterByPerspective(perspective: PhotoPerspective?) {
        _galleryUiState.value = _galleryUiState.value.copy(selectedPerspective = perspective)
        _currentCheckItemId.value?.let { loadPhotos(it) }
    }

    /**
     * Filtra le foto per resolution.
     */
    fun filterByResolution(resolution: PhotoResolution?) {
        _galleryUiState.value = _galleryUiState.value.copy(selectedResolution = resolution)
        _currentCheckItemId.value?.let { loadPhotos(it) }
    }

    /**
     * Cerca foto per caption o perspective.
     */
    fun searchPhotos(query: String) {
        _galleryUiState.value = _galleryUiState.value.copy(searchQuery = query)
        _currentCheckItemId.value?.let { loadPhotos(it) }
    }

    /**
     * Seleziona una foto per la preview.
     */
    fun selectPhoto(photo: Photo?) {
        _galleryUiState.value = _galleryUiState.value.copy(selectedPhoto = photo)

        if (photo != null) {
            _previewUiState.value = _previewUiState.value.copy(
                photo = photo,
                tempCaption = photo.caption,
                tempPerspective = photo.metadata.perspective
            )
        }
    }

    /**
     * Mostra/nasconde la preview fullscreen.
     */
    fun showFullscreen(show: Boolean) {
        _galleryUiState.value = _galleryUiState.value.copy(showFullscreen = show)
    }

    /**
     * Toggle modalità riordino.
     */
    fun toggleReorderMode() {
        _galleryUiState.value = _galleryUiState.value.copy(
            isReorderMode = !_galleryUiState.value.isReorderMode
        )
    }

    /**
     * Riordina le foto.
     */
    fun reorderPhotos(photoOrderUpdates: List<Pair<String, Int>>) {
        viewModelScope.launch {
            val result = updatePhotoUseCase.reorderPhotos(photoOrderUpdates)

            when (result) {
                is PhotoResult.Success -> {
                    _galleryUiState.value = _galleryUiState.value.copy(isReorderMode = false)
                    _currentCheckItemId.value?.let { loadPhotos(it) }
                }
                is PhotoResult.Error -> {
                    _galleryUiState.value = _galleryUiState.value.copy(
                        error = result.exception.message
                    )
                }
                is PhotoResult.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    // ===== PHOTO EDITING FUNCTIONS =====

    /**
     * Inizia l'editing della caption.
     */
    fun startEditingCaption(photo: Photo) {
        _previewUiState.value = _previewUiState.value.copy(
            photo = photo,
            isEditingCaption = true,
            tempCaption = photo.caption
        )
    }

    /**
     * Aggiorna la caption temporanea durante l'editing.
     */
    fun updateTempCaption(caption: String) {
        _previewUiState.value = _previewUiState.value.copy(tempCaption = caption)
    }

    /**
     * Salva la caption modificata.
     */
    fun saveCaption(photoId: String, caption: String) {
        viewModelScope.launch {
            _previewUiState.value = _previewUiState.value.copy(isUpdating = true)

            val result = updatePhotoUseCase.updateCaption(photoId, caption)

            when (result) {
                is PhotoResult.Success -> {
                    _previewUiState.value = _previewUiState.value.copy(
                        photo = result.data,
                        isEditingCaption = false,
                        isUpdating = false,
                        tempCaption = result.data.caption
                    )
                    _currentCheckItemId.value?.let { loadPhotos(it) }
                }
                is PhotoResult.Error -> {
                    _previewUiState.value = _previewUiState.value.copy(
                        isUpdating = false,
                        error = result.exception.message
                    )
                }
                is PhotoResult.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    /**
     * Annulla l'editing della caption.
     */
    fun cancelEditingCaption() {
        _previewUiState.value = _previewUiState.value.copy(
            isEditingCaption = false,
            tempCaption = _previewUiState.value.photo?.caption ?: ""
        )
    }

    /**
     * Inizia l'editing della perspective.
     */
    fun startEditingPerspective(photo: Photo) {
        _previewUiState.value = _previewUiState.value.copy(
            photo = photo,
            isEditingPerspective = true,
            tempPerspective = photo.metadata.perspective
        )
    }

    /**
     * Aggiorna la perspective temporanea.
     */
    fun updateTempPerspective(perspective: PhotoPerspective?) {
        _previewUiState.value = _previewUiState.value.copy(tempPerspective = perspective)
    }

    /**
     * Salva la perspective modificata.
     */
    fun savePerspective(photoId: String, perspective: PhotoPerspective?) {
        viewModelScope.launch {
            _previewUiState.value = _previewUiState.value.copy(isUpdating = true)

            val result = updatePhotoUseCase.updatePerspective(photoId, perspective)

            when (result) {
                is PhotoResult.Success -> {
                    _previewUiState.value = _previewUiState.value.copy(
                        photo = result.data,
                        isEditingPerspective = false,
                        isUpdating = false,
                        tempPerspective = result.data.metadata.perspective
                    )
                    _currentCheckItemId.value?.let { loadPhotos(it) }
                }
                is PhotoResult.Error -> {
                    _previewUiState.value = _previewUiState.value.copy(
                        isUpdating = false,
                        error = result.exception.message
                    )
                }
                is PhotoResult.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    /**
     * Annulla l'editing della perspective.
     */
    fun cancelEditingPerspective() {
        _previewUiState.value = _previewUiState.value.copy(
            isEditingPerspective = false,
            tempPerspective = _previewUiState.value.photo?.metadata?.perspective
        )
    }

    // ===== PHOTO MANAGEMENT FUNCTIONS =====

    /**
     * Elimina una foto.
     */
    fun deletePhoto(photoId: String) {
        viewModelScope.launch {
            val result = deletePhotoUseCase(photoId, deleteFile = true)

            when (result) {
                is PhotoResult.Success -> {
                    _currentCheckItemId.value?.let { loadPhotos(it) }
                    _previewUiState.value = _previewUiState.value.copy(
                        showDeleteConfirmation = false
                    )
                }
                is PhotoResult.Error -> {
                    _previewUiState.value = _previewUiState.value.copy(
                        error = result.exception.message
                    )
                }
                is PhotoResult.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    /**
     * Mostra/nasconde la conferma di eliminazione.
     */
    fun showDeleteConfirmation(show: Boolean) {
        _previewUiState.value = _previewUiState.value.copy(showDeleteConfirmation = show)
    }

    // ===== ERROR HANDLING =====

    /**
     * Pulisce gli errori dall'UI state.
     */
    fun clearError() {
        _cameraUiState.value = _cameraUiState.value.copy(error = null)
        _galleryUiState.value = _galleryUiState.value.copy(error = null)
        _previewUiState.value = _previewUiState.value.copy(error = null)
    }

    /**
     * Reset dello stato della camera dopo successo.
     */
    fun resetCaptureSuccess() {
        _cameraUiState.value = _cameraUiState.value.copy(captureSuccess = false)
    }

    // ===== PERMISSION HANDLING =====

    /**
     * Aggiorna lo stato dei permessi camera.
     */
    fun updateCameraPermission(granted: Boolean) {
        _cameraUiState.value = _cameraUiState.value.copy(permissionGranted = granted)
    }

    /**
     * Mostra/nasconde il rationale per i permessi.
     */
    fun showPermissionRationale(show: Boolean) {
        _cameraUiState.value = _cameraUiState.value.copy(showPermissionRationale = show)
    }

    // ===== CLEANUP =====

    override fun onCleared() {
        super.onCleared()
        cameraController.release()
    }
}