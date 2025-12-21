package net.calvuz.qreport.data.backup.model

/**
 * Progress archivio foto
 */
sealed class ArchiveProgress {
    data class InProgress(
        val processedFiles: Int,
        val totalFiles: Int,
        val currentFile: String,
        val progress: Float
    ) : ArchiveProgress()

    data class Completed(
        val archivePath: String,
        val totalFiles: Int,
        val totalSizeMB: Double
    ) : ArchiveProgress()

    data class Error(val message: String, val throwable: Throwable? = null) : ArchiveProgress()
}