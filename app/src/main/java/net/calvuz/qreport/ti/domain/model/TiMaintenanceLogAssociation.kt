package net.calvuz.qreport.ti.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class TiMaintenanceLogAssociation(
    val id: String,
    val interventionId: String,
    val maintenanceLogId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncedAt: Instant? = null,
    val isDeleted: Boolean = false
)
