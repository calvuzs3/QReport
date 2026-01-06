package net.calvuz.qreport.backup.data.model

import kotlinx.datetime.Instant

data class BackupSummary(
    val totalBackups: Int,
    val totalSize: Long,
    val lastBackupTimestamp: Instant?,
    val hasPhotoBackups: Boolean,
    val oldestBackupTimestamp: Instant?
) {
    companion object {
        fun empty() = BackupSummary(
            totalBackups = 0,
            totalSize = 0L,
            lastBackupTimestamp = null,
            hasPhotoBackups = false,
            oldestBackupTimestamp = null
        )
    }
}
