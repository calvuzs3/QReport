package net.calvuz.qreport.domain.model.photo

import kotlinx.datetime.Instant

/**
 * Filtri per la ricerca delle foto
 */
data class PhotoFilter(
    val checkItemId: String? = null,
    val dateRange: Pair<Instant, Instant>? = null,
    val minFileSize: Long? = null,
    val maxFileSize: Long? = null,
    val hasCaption: Boolean? = null,
    val perspective: PhotoPerspective? = null,
    val resolution: PhotoResolution? = null,
    val minWidth: Int? = null,
    val maxWidth: Int? = null,
    val minHeight: Int? = null,
    val maxHeight: Int? = null,
    val aspectRatioCategory: String? = null
)

