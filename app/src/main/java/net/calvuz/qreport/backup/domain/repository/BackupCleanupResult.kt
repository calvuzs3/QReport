package net.calvuz.qreport.feature.backup.domain.repository

data class BackupCleanupResult(
    val deletedBackups: Int,
    val freedSpace: Long,
    val preservedBackups: List<String>,
    val errors: List<String> = emptyList()
)