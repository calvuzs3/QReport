package net.calvuz.qreport.ti.data.local.entity

import androidx.room.*
import net.calvuz.qreport.client.island.maintenance.data.local.entity.MaintenanceLogEntity

@Entity(
    tableName = "ti_maintenance_log_associations",
    foreignKeys = [
        ForeignKey(
            entity = TechnicalInterventionEntity::class,
            parentColumns = ["id"],
            childColumns = ["intervention_id"],
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
        Index(value = ["intervention_id"]),
        Index(value = ["maintenance_log_id"]),
        Index(value = ["is_deleted"]),
        Index(value = ["intervention_id", "maintenance_log_id"], unique = true)
    ]
)
data class TiMaintenanceLogAssociationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "intervention_id") val interventionId: String,
    @ColumnInfo(name = "maintenance_log_id") val maintenanceLogId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false
)
