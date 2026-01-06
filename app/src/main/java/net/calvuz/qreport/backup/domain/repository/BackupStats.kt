package net.calvuz.qreport.feature.backup.domain.repository

data class BackupStats(
    val totalSize: Long,
    val fileCount: Int,
    val jsonFiles: List<BackupFileInfo>,
    val photoArchive: BackupFileInfo?,
    val infoFile: BackupFileInfo?,
    val createdAt: Long,
    val lastModified: Long
)