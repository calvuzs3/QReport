package net.calvuz.qreport.backup.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.calvuz.qreport.photo.domain.model.CameraSettings
import net.calvuz.qreport.photo.domain.model.PhotoLocation
import net.calvuz.qreport.photo.domain.model.PhotoPerspective
import net.calvuz.qreport.photo.domain.model.PhotoResolution

/**
 * PhotoBackup - Backup delle foto
 */
@Serializable
data class PhotoBackup(
    val id: String,
    val checkItemId: String,
    val fileName: String,
    val filePath: String,
    val thumbnailPath: String?,
    val caption: String,
    @Contextual val takenAt: Instant,
    val fileSize: Long,
    val orderIndex: Int,
    val width: Int,
    val height: Int,
    val gpsLocation: PhotoLocation?,
    val resolution: PhotoResolution?,
    val perspective: PhotoPerspective?,
    val exifData: Map<String, String>,
    val cameraSettings: CameraSettings?
)