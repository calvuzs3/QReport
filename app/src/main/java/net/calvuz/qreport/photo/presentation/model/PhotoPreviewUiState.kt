package net.calvuz.qreport.photo.presentation.model

import net.calvuz.qreport.photo.domain.model.Photo
import net.calvuz.qreport.photo.domain.model.PhotoPerspective

/**
 * Stato UI per la preview di una singola foto.
 */
data class PhotoPreviewUiState(
    val photo: Photo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditingCaption: Boolean = false,
    val tempCaption: String = "",
    val isUpdating: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val isEditingPerspective: Boolean = false,
    val tempPerspective: PhotoPerspective? = null
) {
    val hasPhoto: Boolean get() = photo != null
    val caption: String get() = photo?.caption ?: ""
    val perspective: PhotoPerspective? get() = photo?.metadata?.perspective
}