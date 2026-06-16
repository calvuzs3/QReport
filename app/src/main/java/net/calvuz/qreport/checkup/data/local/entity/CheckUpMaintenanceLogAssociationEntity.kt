package net.calvuz.qreport.checkup.data.local.entity

import androidx.room.*
import net.calvuz.qreport.client.island.maintenance.data.local.entity.MaintenanceLogEntity

@Entity(
    tableName = "checkup_maintenance_log_associations",
    foreignKeys = [
        ForeignKey(
            entity = CheckUpEntity::class,
            parentColumns = ["id"],
            childColumns = ["checkup_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MaintenanceLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["maintenance_log_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["checkup_id"]),
        Index(value = ["maintenance_log_id"]),
        Index(value = ["is_deleted"]),
        Index(value = ["checkup_id", "maintenance_log_id"], unique = true)
    ]
)
data class CheckUpMaintenanceLogAssociationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "checkup_id") val checkupId: String,
    @ColumnInfo(name = "maintenance_log_id") val maintenanceLogId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false
)
