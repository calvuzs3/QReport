package net.calvuz.qreport.photo.domain.model

import kotlinx.serialization.Serializable

/**
 * Configuration per le foto
 */
@Serializable
data class PhotoConfig(
    val maxPhotosPerItem: Int = 10,
    val maxFileSize: Long = 10_000_000L, // 10MB
    val compressionQuality: Int = 85,
    val defaultResolution: PhotoResolution = PhotoResolution.HIGH,
    val enableWatermark: Boolean = true,
    val watermarkText: String = "QReport",
    val enableGpsTagging: Boolean = false,
    val defaultPerspective: PhotoPerspective = PhotoPerspective.OVERVIEW
)