package net.calvuz.qreport.domain.model.camera

import kotlinx.serialization.Serializable
import net.calvuz.qreport.domain.model.photo.PhotoConfig
import net.calvuz.qreport.domain.model.photo.PhotoPerspective
import net.calvuz.qreport.domain.model.photo.PhotoResolution

/**
 * Impostazioni camera utilizzate per lo scatto
 */
@Serializable
data class CameraSettings(
    val compressionQuality: Int = 85,
    val resolution: PhotoResolution = PhotoResolution.HIGH,
    val perspective: PhotoPerspective = PhotoPerspective.OVERVIEW,
    val generateThumbnail: Boolean = true,
    val thumbnailSize: Int = 200,
    val saveMetadata: Boolean = true,
    val autoCorrectOrientation: Boolean = true,
    val enableGpsTagging: Boolean = false,
    val watermarkEnabled: Boolean = true,
    // ✅ NUOVO: Integrazione con CameraConfig
    val cameraConfig: CameraConfig = CameraConfig.default()
) {
    companion object {
        fun default() = CameraSettings()

        fun highQuality() = CameraSettings(
            compressionQuality = 95,
            resolution = PhotoResolution.VERY_HIGH,
            cameraConfig = CameraConfig.highQuality()
        )

        fun lowStorage() = CameraSettings(
            compressionQuality = 70,
            resolution = PhotoResolution.MEDIUM,
            cameraConfig = CameraConfig.lowStorage()
        )

        fun fromConfig(config: PhotoConfig) = CameraSettings(
            compressionQuality = config.compressionQuality,
            resolution = config.defaultResolution,
            perspective = config.defaultPerspective,
            enableGpsTagging = config.enableGpsTagging,
            watermarkEnabled = config.enableWatermark
        )

        // ✅ NUOVO: Crea da CameraConfig
        fun fromCameraConfig(cameraConfig: CameraConfig) = CameraSettings(
            compressionQuality = cameraConfig.jpegQuality,
            resolution = cameraConfig.preferredResolution,
            enableGpsTagging = cameraConfig.enableLocationTags,
            autoCorrectOrientation = true, // Sempre attivo per il fix
            cameraConfig = cameraConfig
        )
    }

    // ✅ COMPUTED PROPERTIES: Facilita accesso ai valori
    val autoFocus: Boolean get() = cameraConfig.autoFocus
    val flashMode: String get() = cameraConfig.flashMode
    val enableLocationTags: Boolean get() = cameraConfig.enableLocationTags
    val enableGridLines: Boolean get() = cameraConfig.enableGridLines

}