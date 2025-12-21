package net.calvuz.qreport.domain.model.backup

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * BackupMetadata - Informazioni sul backup
 */
@Serializable
data class BackupMetadata(
    val id: String,
    @Contextual val timestamp: Instant,
    val appVersion: String,
    val databaseVersion: Int,
    val deviceInfo: DeviceInfo,
    val backupType: BackupType,
    val totalSize: Long,
    val checksum: String,
    val description: String? = null
) {
    companion object {
        fun create(
            appVersion: String,
            databaseVersion: Int,
            deviceInfo: DeviceInfo,
            backupType: BackupType = BackupType.FULL,
            totalSize: Long = 0L,
            description: String? = null
        ): BackupMetadata {
            return BackupMetadata(
                id = UUID.randomUUID().toString(),
                timestamp = Clock.System.now(),
                appVersion = appVersion,
                databaseVersion = databaseVersion,
                deviceInfo = deviceInfo,
                backupType = backupType,
                totalSize = totalSize,
                checksum = "", // Calculated after creation
                description = description
            )
        }
    }
}