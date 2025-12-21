package net.calvuz.qreport.domain.model.backup

/**
 * BackupProgress - Progress stato backup
 */
sealed class BackupProgress {
    object Idle : BackupProgress()

    data class InProgress(
        val step: String,
        val progress: Float,
        val currentTable: String? = null,
        val processedRecords: Int = 0,
        val totalRecords: Int = 0
    ) : BackupProgress()

    data class Completed(
        val backupId: String,
        val backupPath: String,
        val totalSize: Long,
        val duration: Long,
        val tablesBackedUp: Int = 9
    ) : BackupProgress()

    data class Error(val message: String, val throwable: Throwable? = null) : BackupProgress()
}