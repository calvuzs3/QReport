package net.calvuz.qreport.presentation.feature.photo.model

import net.calvuz.qreport.domain.model.photo.Photo
import net.calvuz.qreport.domain.model.photo.PhotoPerspective
import net.calvuz.qreport.domain.model.photo.PhotoResolution

/**
 * Evento UI per le azioni delle foto.
 */
sealed class PhotoUiEvent {
    // Camera events
    data object InitializeCamera : PhotoUiEvent()
    data object CapturePhoto : PhotoUiEvent()
    data class SetFlashMode(val flashMode: Int) : PhotoUiEvent()
    data class SetZoomRatio(val zoomRatio: Float) : PhotoUiEvent()
    data class FocusOnPoint(val x: Float, val y: Float) : PhotoUiEvent()
    data class SetPerspective(val perspective: PhotoPerspective) : PhotoUiEvent()
    data class SetResolution(val resolution: PhotoResolution) : PhotoUiEvent()
    data object DismissCameraError : PhotoUiEvent()

    // Gallery events
    data class LoadPhotos(val checkItemId: String) : PhotoUiEvent()
    data class SelectPhoto(val photo: Photo?) : PhotoUiEvent()
    data class ShowFullscreen(val show: Boolean) : PhotoUiEvent()
    data class SearchPhotos(val query: String) : PhotoUiEvent()
    data class FilterByPerspective(val perspective: PhotoPerspective?) : PhotoUiEvent()
    data class FilterByResolution(val resolution: PhotoResolution?) : PhotoUiEvent()
    data object RefreshPhotos : PhotoUiEvent()
    data object ToggleReorderMode : PhotoUiEvent()
    data class ReorderPhotos(val photoOrderUpdates: List<Pair<String, Int>>) : PhotoUiEvent()

    // Photo editing events
    data class StartEditingCaption(val photo: Photo) : PhotoUiEvent()
    data class UpdateCaption(val newCaption: String) : PhotoUiEvent()
    data class SaveCaption(val photoId: String, val caption: String) : PhotoUiEvent()
    data object CancelEditingCaption : PhotoUiEvent()
    data class StartEditingPerspective(val photo: Photo) : PhotoUiEvent()
    data class UpdatePerspective(val perspective: PhotoPerspective?) : PhotoUiEvent()
    data class SavePerspective(val photoId: String, val perspective: PhotoPerspective?) : PhotoUiEvent()
    data object CancelEditingPerspective : PhotoUiEvent()

    // Photo management events
    data class DeletePhoto(val photoId: String) : PhotoUiEvent()
    data class ShowDeleteConfirmation(val show: Boolean) : PhotoUiEvent()
    data class SharePhoto(val photo: Photo) : PhotoUiEvent()

    // Permission events
    data object RequestCameraPermission : PhotoUiEvent()
    data object DismissPermissionRationale : PhotoUiEvent()
}