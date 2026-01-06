package net.calvuz.qreport.backup.presentation.ui.model

/**
 * RestoreProgress - Progress stato ripristino
 */
sealed class RestoreProgress {

    object Idle : RestoreProgress()

    data class InProgress(
        val step: String,
        val progress: Float,
        val currentTable: String? = null,
        val processedRecords: Int = 0,
        val totalRecords: Int = 0
    ) : RestoreProgress()

    data class Completed(val backupId: String, val processedRecords: Int? = 0) : RestoreProgress()

    data class Error(val message: String, val throwable: Throwable? = null) : RestoreProgress()
}