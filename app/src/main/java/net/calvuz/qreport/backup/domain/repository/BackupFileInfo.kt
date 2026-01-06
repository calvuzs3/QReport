package net.calvuz.qreport.feature.backup.domain.repository

data class BackupFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long
)