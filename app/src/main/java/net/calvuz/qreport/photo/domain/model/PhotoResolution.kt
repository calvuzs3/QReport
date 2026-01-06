package net.calvuz.qreport.photo.domain.model

import kotlinx.serialization.Serializable

/**
 * Enum per risoluzione foto (se serve)
 */
@Serializable
enum class PhotoResolution(val displayName: String, val width: Int, val height: Int) {
    LOW("Bassa", 640, 480),
    MEDIUM("Media", 1280, 720),
    HIGH("Alta", 1920, 1080),
    VERY_HIGH("Molto Alta", 3840, 2160);

    fun getAspectRatio(): Float = width.toFloat() / height.toFloat()
    fun getMegapixels(): Float = (width * height) / 1_000_000f
}