package net.calvuz.qreport.data.backup.model

data class BackupSummary(
    val totalBackups: Int,
    val totalSizeMB: Long,
    val lastBackupTimestamp: kotlinx.datetime.Instant?,
    val hasPhotoBackups: Boolean,
    val oldestBackupTimestamp: kotlinx.datetime.Instant?
) {
    companion object {
        fun empty() = BackupSummary(
            totalBackups = 0,
            totalSizeMB = 0L,
            lastBackupTimestamp = null,
            hasPhotoBackups = false,
            oldestBackupTimestamp = null
        )
    }
}
