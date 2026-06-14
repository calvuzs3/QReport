package net.calvuz.qreport.backup.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * MaintenanceLogBackup - Backup of maintenance log entries
 *
 * Mirrors MaintenanceLogEntity. Logs are content-immutable after creation,
 * but lifecycle fields (isActive/isDeleted) and syncedAt are preserved.
 */
@Serializable
data class MaintenanceLogBackup(
    val id: String,
    val islandId: String,
    val operationType: String,
    val customOperationLabel: String?,
    val mechanicalUnitId: String?,
    val componentLabel: String?,
    val description: String,
    val technicianName: String,
    val technicianCompany: String?,
    val operatingHoursAtEvent: Int?,
    val cycleCountAtEvent: Long?,
    val outcome: String,
    val durationMinutes: Int?,
    val notes: String?,
    @Contextual val performedAt: Instant,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
    val syncedAt: Long? = null,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false
)
