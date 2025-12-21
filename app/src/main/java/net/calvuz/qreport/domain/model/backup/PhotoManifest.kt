package net.calvuz.qreport.domain.model.backup

import kotlinx.serialization.Serializable

/**
 * PhotoManifest - Manifesto delle foto nel backup
 */
@Serializable
data class PhotoManifest(
    val totalPhotos: Int,
    val totalSizeMB: Double,
    val photos: List<PhotoBackupInfo>,
    val includesThumbnails: Boolean
) {
    companion object {
        fun empty(): PhotoManifest {
            return PhotoManifest(
                totalPhotos = 0,
                totalSizeMB = 0.0,
                photos = emptyList(),
                includesThumbnails = false
            )
        }
    }
}