package net.calvuz.qreport.presentation.feature.photo.model

import net.calvuz.qreport.domain.model.photo.Photo
import net.calvuz.qreport.domain.model.photo.PhotoPerspective

/**
 * Modello per l'anteprima rapida delle foto in un check item.
 */
data class PhotoPreviewModel(
    val photosCount: Int,
    val firstPhotoThumbnail: String?,
    val hasMultiplePhotos: Boolean,
    val perspectiveDistribution: Map<PhotoPerspective, Int> = emptyMap()
) {
    companion object {
        fun fromPhotos(photos: List<Photo>): PhotoPreviewModel {
            return PhotoPreviewModel(
                photosCount = photos.size,
                firstPhotoThumbnail = photos.firstOrNull()?.filePath,
                hasMultiplePhotos = photos.size > 1,
                perspectiveDistribution = photos
                    .mapNotNull { it.metadata.perspective }
                    .groupingBy { it }
                    .eachCount()
            )
        }

        val empty = PhotoPreviewModel(0, null, false)
    }
}