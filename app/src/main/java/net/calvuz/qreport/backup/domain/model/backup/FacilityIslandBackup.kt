package net.calvuz.qreport.backup.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * FacilityIslandBackup - Backup delle isole robotiche
 */
@Serializable
data class FacilityIslandBackup(
    val id: String,
    val facilityId: String,
    val islandType: String,
    val serialNumber: String,
    val model: String?,
    @Contextual val installationDate: Instant?,
    @Contextual val warrantyExpiration: Instant?,
    val isActive: Boolean,
    val operatingHours: Int,
    val cycleCount: Long,
    @Contextual val lastMaintenanceDate: Instant?,
    @Contextual val nextScheduledMaintenance: Instant?,
    val customName: String?,
    val location: String?,
    val notes: String?,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant
)