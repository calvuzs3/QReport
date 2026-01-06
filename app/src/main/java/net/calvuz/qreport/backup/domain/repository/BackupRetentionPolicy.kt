package net.calvuz.qreport.feature.backup.domain.repository

data class BackupRetentionPolicy(
    val maxBackups: Int? = null,
    val maxAgeDays: Int? = null,
    val maxTotalSize: Long? = null,
    val preserveDaily: Int? = null,
    val preserveWeekly: Int? = null,
    val preserveMonthly: Int? = null
)