package net.calvuz.qreport.backup.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class CheckUpMaintenanceLogAssociationBackup(
    val id: String,
    val checkupId: String,
    val maintenanceLogId: String,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false
)
