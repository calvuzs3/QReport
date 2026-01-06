package net.calvuz.qreport.backup.domain.model

import kotlinx.datetime.Instant

/**
 * Informazioni backup per lista
 */
data class BackupInfo(
    val id: String,
    val createdAt: Instant,
    val description: String?,
    val totalSize: Long,
    val includesPhotos: Boolean,
    val dirPath: String,
    val filePath: String,
    val appVersion: String,
    val recordCount: Int? = null,
    val photoCount: Int? = null
)