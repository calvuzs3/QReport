package net.calvuz.qreport.domain.model

import kotlinx.serialization.Serializable


/**
 * Configurazioni camera
 */
@Serializable
data class CameraConfig(
    val autoFocus: Boolean = true,
    val flashMode: String = "auto",
    val preferredResolution: PhotoResolution = PhotoResolution.HIGH,
    val jpegQuality: Int = 90,
    val enableLocationTags: Boolean = false,
    val enableGridLines: Boolean = true
)