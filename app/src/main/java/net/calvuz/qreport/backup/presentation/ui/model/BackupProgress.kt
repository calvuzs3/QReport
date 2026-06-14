package net.calvuz.qreport.backup.presentation.ui.model

import net.calvuz.qreport.app.error.presentation.UiText
import kotlin.time.Duration

/**
 * BackupProgress - Progress stato backup
 */
sealed class BackupProgress {
    object Idle : BackupProgress()

    data class InProgress(
        val step: UiText,
        val progress: Float,
        val currentTable: UiText? = null,
        val processedRecords: Int = 0,
        val totalRecords: Int = 0
    ) : BackupProgress()

    data class Completed(
        val backupId: String,
        val backupPath: String,
        val totalSize: Long,
        val duration: Duration,
        val tablesBackedUp: Int = 14
    ) : BackupProgress()

    data class Error(
        val message: UiText,
        val throwable: Throwable? = null
    ) : BackupProgress()
}