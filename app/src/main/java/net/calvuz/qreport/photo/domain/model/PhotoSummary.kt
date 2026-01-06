package net.calvuz.qreport.photo.domain.model

import kotlinx.datetime.Instant

/**
 * Informazioni di riassunto per le foto
 */
data class PhotoSummary(
    val totalPhotos: Int,
    val totalSize: Long,
    val photosWithCaption: Int,
    val averageFileSize: Long,
    val oldestPhoto: Instant?,
    val newestPhoto: Instant?,
    val perspectiveDistribution: Map<PhotoPerspective, Int> = emptyMap(),
    val resolutionDistribution: Map<PhotoResolution, Int> = emptyMap(),
    val averageResolution: Float?,
    val photosWithValidDimensions: Int?,
    val landscapePhotos: Int?,
    val portraitPhotos: Int?,
    val squarePhotos: Int?
)