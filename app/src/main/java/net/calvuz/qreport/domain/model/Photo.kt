package net.calvuz.qreport.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Foto associata a un check item
 */
@Serializable
data class Photo(
    val id: String,
    val checkItemId: String,
    val fileName: String,
    val filePath: String,
    val caption: String,
    val takenAt: Instant,
    val fileSize: Long,
    val orderIndex: Int,
    val metadata: PhotoMetadata = PhotoMetadata()
)