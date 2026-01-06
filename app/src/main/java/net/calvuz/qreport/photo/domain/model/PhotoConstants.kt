package net.calvuz.qreport.photo.domain.model

// domain/model/photo/PhotoConstants.kt
object PhotoConstants {
    const val MAX_CAPTION_LENGTH = 500
    val FORBIDDEN_CHARACTERS = Regex("[<>:\"/\\\\|?*]")
    const val DEFAULT_COMPRESSION_QUALITY = 85
    const val DEFAULT_THUMBNAIL_SIZE = 200
}