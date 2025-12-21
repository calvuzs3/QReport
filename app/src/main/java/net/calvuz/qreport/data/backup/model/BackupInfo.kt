package net.calvuz.qreport.data.backup.model

import kotlinx.datetime.Instant

/**
 * Informazioni backup per lista
 */
data class BackupInfo(
    val id: String,
    val timestamp: Instant,
    val description: String?,
    val totalSizeMB: Double,
    val includesPhotos: Boolean,
    val dirPath: String,
    val filePath: String,
    val appVersion: String,
    val recordCount: Int? = null,
    val photoCount: Int? = null
)