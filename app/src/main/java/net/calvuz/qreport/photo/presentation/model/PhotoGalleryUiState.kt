package net.calvuz.qreport.photo.presentation.model

import net.calvuz.qreport.photo.domain.model.Photo
import net.calvuz.qreport.photo.domain.model.PhotoPerspective
import net.calvuz.qreport.photo.domain.model.PhotoResolution

/**
 * Stati UI per la gestione delle foto nel presentation layer.
 * Aggiornato per supportare i nuovi domain model con perspective, resolution, orderIndex.
 */

/**
 * Stato UI per la gallery delle foto di un check item.
 */
data class PhotoGalleryUiState(
    val photos: List<Photo> = emptyList(),
    val selectedPhoto: Photo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showFullscreen: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val selectedPerspective: PhotoPerspective? = null,
    val selectedResolution: PhotoResolution? = null,
    val isReorderMode: Boolean = false
) {
    val hasPhotos: Boolean get() = photos.isNotEmpty()
    val photosCount: Int get() = photos.size
    val isEmpty: Boolean get() = photos.isEmpty() && !isLoading

    val availablePerspectives: List<PhotoPerspective>
        get() = photos.mapNotNull { it.metadata.perspective }.distinct()

    val availableResolutions: List<PhotoResolution>
        get() = photos.mapNotNull { it.metadata.resolution }.distinct()
}