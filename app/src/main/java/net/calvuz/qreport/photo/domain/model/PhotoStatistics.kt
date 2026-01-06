package net.calvuz.qreport.photo.domain.model


/**
 * Data class per le statistiche delle foto.
 */
data class PhotoStatistics(
    val totalCount: Int,
    val totalSize: Long,
    val averageSize: Long,
    val oldestTimestamp: Long,
    val newestTimestamp: Long,
    val photosWithCaption: Int,
    val averageResolution: Float,
    val photosWithValidDimensions: Int,
    val landscapePhotos: Int,
    val portraitPhotos: Int,
    val squarePhotos: Int
)