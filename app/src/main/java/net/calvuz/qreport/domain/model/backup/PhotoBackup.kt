package net.calvuz.qreport.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.calvuz.qreport.domain.model.camera.CameraSettings
import net.calvuz.qreport.domain.model.photo.PhotoLocation
import net.calvuz.qreport.domain.model.photo.PhotoPerspective
import net.calvuz.qreport.domain.model.photo.PhotoResolution

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