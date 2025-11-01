/**
 * ✅ STEP 2: CameraConfig espanso con opzioni orientamento e processing
 */

package net.calvuz.qreport.domain.model.camera

import kotlinx.serialization.Serializable
import net.calvuz.qreport.domain.model.photo.PhotoResolution

/**
 * ✅ AGGIORNATO: Configurazioni camera complete con gestione orientamento
 */
@Serializable
data class CameraConfig(
    // ===== CAMERA HARDWARE =====
    val autoFocus: Boolean = true,
    val flashMode: String = "auto", // "auto", "on", "off", "torch"
    val preferredResolution: PhotoResolution = PhotoResolution.HIGH,
    val enableGridLines: Boolean = true,
    val enableLocationTags: Boolean = false,

    // ===== IMAGE PROCESSING =====
    val jpegQuality: Int = 90,
    val autoCorrectOrientation: Boolean = true,     // ✅ CRUCIALE per fix orientamento
    val applyOrientationPhysically: Boolean = true, // ✅ NUOVO: Rotazione fisica vs solo EXIF
    val preserveOriginalExif: Boolean = true,       // ✅ NUOVO: Preserva metadati camera
    val generateThumbnails: Boolean = true,
    val thumbnailSize: Int = 150,

    // ===== WATERMARK & ENHANCEMENT =====
    val enableWatermark: Boolean = false,
    val watermarkText: String = "QReport",
    val watermarkPosition: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    val enableImageEnhancement: Boolean = false,    // ✅ NUOVO: Auto-enhancement

    // ===== STORAGE =====
    val maxImageDimension: Int = 1920,             // ✅ NUOVO: Limite dimensioni
    val enableBackup: Boolean = false,             // ✅ NUOVO: Backup originali
    val compressionLevel: CompressionLevel = CompressionLevel.MEDIUM
) {
    companion object {
        fun default() = CameraConfig()

        fun highQuality() = CameraConfig(
            jpegQuality = 95,
            preferredResolution = PhotoResolution.VERY_HIGH,
            maxImageDimension = 2560,
            compressionLevel = CompressionLevel.LOW,
            enableImageEnhancement = true
        )

        fun lowStorage() = CameraConfig(
            jpegQuality = 75,
            preferredResolution = PhotoResolution.MEDIUM,
            maxImageDimension = 1280,
            compressionLevel = CompressionLevel.HIGH,
            enableBackup = false
        )

        fun debugMode() = CameraConfig(
            jpegQuality = 85,
            autoCorrectOrientation = true,
            applyOrientationPhysically = true,
            preserveOriginalExif = true,
            enableBackup = true,  // Per debug, mantieni originali
            enableLocationTags = false
        )

        // ✅ NUOVO: Configurazione per fix orientamento
        fun orientationFix() = CameraConfig(
            autoCorrectOrientation = true,
            applyOrientationPhysically = true,
            preserveOriginalExif = true,
            jpegQuality = 90
        )
    }

    // ===== COMPUTED PROPERTIES =====

    /**
     * Qualità JPEG effettiva basata su compression level
     */
    val effectiveJpegQuality: Int
        get() = when (compressionLevel) {
            CompressionLevel.LOW -> maxOf(jpegQuality, 90)
            CompressionLevel.MEDIUM -> jpegQuality
            CompressionLevel.HIGH -> minOf(jpegQuality, 75)
        }

    /**
     * Dimensione thumbnail effettiva
     */
    val effectiveThumbnailSize: Int
        get() = if (generateThumbnails) thumbnailSize else 0

    /**
     * Se deve applicare correzioni orientamento
     */
    val shouldFixOrientation: Boolean
        get() = autoCorrectOrientation && applyOrientationPhysically

    /**
     * Configurazione per ImageProcessor
     */
    fun toImageProcessorConfig() = ImageProcessorConfig(
        maxDimension = maxImageDimension,
        quality = effectiveJpegQuality,
        fixOrientation = shouldFixOrientation,
        preserveExif = preserveOriginalExif,
        generateThumbnail = generateThumbnails,
        thumbnailSize = effectiveThumbnailSize,
        enableWatermark = enableWatermark,
        watermarkText = watermarkText
    )
}

/**
 * ✅ NUOVO: Livelli di compressione
 */
@Serializable
enum class CompressionLevel {
    LOW,    // Alta qualità, file grandi
    MEDIUM, // Bilanciato
    HIGH    // Bassa qualità, file piccoli
}

/**
 * ✅ NUOVO: Posizioni watermark
 */
@Serializable
enum class WatermarkPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER
}

/**
 * ✅ NUOVO: Configurazione per ImageProcessor
 */
data class ImageProcessorConfig(
    val maxDimension: Int,
    val quality: Int,
    val fixOrientation: Boolean,
    val preserveExif: Boolean,
    val generateThumbnail: Boolean,
    val thumbnailSize: Int,
    val enableWatermark: Boolean,
    val watermarkText: String
) {
    companion object {
        fun fromCameraConfig(cameraConfig: CameraConfig) = cameraConfig.toImageProcessorConfig()
    }
}