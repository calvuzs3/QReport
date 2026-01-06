package net.calvuz.qreport.app.file.domain.model

/**
 * Generic file information
 */
data class CoreFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val extension: String? = null
)