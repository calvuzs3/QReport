package net.calvuz.qreport.backup.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class MechanicalUnitBackup(
    val id: String,
    val islandId: String,
    val unitType: String,
    val name: String,
    val serialNumber: String?,
    val model: String?,
    val notes: String?,
    val isActive: Boolean = true,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant
)