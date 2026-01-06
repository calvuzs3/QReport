package net.calvuz.qreport.app.file.domain.model

/**
 * File filtering options
 */
data class FileFilter(
    val extensions: Set<String>? = null,        // Filter by extension
    val namePattern: String? = null,            // Regex pattern for name
    val minSize: Long? = null,                  // Min file size
    val maxSize: Long? = null,                  // Max file size
    val olderThan: Long? = null,               // Files older than timestamp
    val newerThan: Long? = null                // Files newer than timestamp
)