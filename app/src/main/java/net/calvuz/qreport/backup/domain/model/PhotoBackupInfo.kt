package net.calvuz.qreport.backup.domain.model

import kotlinx.serialization.Serializable

/**
 * PhotoBackupInfo - Informazioni per backup foto singola
 */
@Serializable
data class PhotoBackupInfo(
    val checkItemId: String,
    val fileName: String,
    val relativePath: String,
    val sizeBytes: Long,
    val sha256Hash: String,
    val hasThumbnail: Boolean
)