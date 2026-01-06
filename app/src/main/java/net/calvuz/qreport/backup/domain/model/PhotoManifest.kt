package net.calvuz.qreport.backup.domain.model

import kotlinx.serialization.Serializable

/**
 * PhotoManifest - Manifesto delle foto nel backup
 */
@Serializable
data class PhotoManifest(
    val totalPhotos: Int,
    val totalSize: Long,
    val photos: List<PhotoBackupInfo>,
    val includesThumbnails: Boolean
) {
    companion object {
        fun empty(): PhotoManifest {
            return PhotoManifest(
                totalPhotos = 0,
                totalSize = 0L,
                photos = emptyList(),
                includesThumbnails = false
            )
        }
    }
}