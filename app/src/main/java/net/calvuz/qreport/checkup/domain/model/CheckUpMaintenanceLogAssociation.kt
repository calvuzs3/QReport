package net.calvuz.qreport.checkup.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CheckUpMaintenanceLogAssociation(
    val id: String,
    val checkupId: String,
    val maintenanceLogId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncedAt: Instant? = null,
    val isDeleted: Boolean = false
)
