package net.calvuz.qreport.backup.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class TiMaintenanceLogAssociationBackup(
    val id: String,
    val interventionId: String,
    val maintenanceLogId: String,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false
)
