package net.calvuz.qreport.data.backup.model

/**
 * Progress estrazione foto
 */
sealed class ExtractionProgress {
    data class InProgress(
        val extractedFiles: Int,
        val totalFiles: Int,
        val currentFile: String,
        val progress: Float
    ) : ExtractionProgress()

    data class Completed(
        val outputDir: String,
        val extractedFiles: Int
    ) : ExtractionProgress()

    data class Error(val message: String, val throwable: Throwable? = null) : ExtractionProgress()
}