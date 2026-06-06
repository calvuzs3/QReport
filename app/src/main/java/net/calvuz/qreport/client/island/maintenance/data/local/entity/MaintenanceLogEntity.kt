package net.calvuz.qreport.client.island.maintenance.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import net.calvuz.qreport.client.island.data.local.entity.IslandEntity

/**
 * Room entity for the maintenance_logs table.
 *
 * Lifecycle fields follow the project-wide two-stage delete convention:
 *  - [isActive]   false = log is logically deactivated (first stage)
 *  - [isDeleted]  true  = log is marked for server sync / purge (second stage)
 *  - [updatedAt]  updated on every write; used for sync conflict resolution
 *
 * All normal DAO queries filter WHERE is_deleted = 0.
 *
 * Component targeting:
 *  - [mechanicalUnitId] references mechanical_units.id — nullable, no FK constraint
 *    at DB level to avoid orphan violations if a unit is deactivated after the log
 *    was created. Referential integrity is enforced in the use case layer.
 *  - [componentLabel] is used when the component is not yet catalogued as a MechanicalUnit.
 *
 * [customOperationLabel] is only stored when operation_type = 'OTHER'.
 *
 * Island cascade: logs are deleted when the parent island is deleted (CASCADE).
 */
@Entity(
    tableName = "maintenance_logs",
    foreignKeys = [
        ForeignKey(
            entity = IslandEntity::class,
            parentColumns = ["id"],
            childColumns = ["island_id"],
            onDelete = ForeignKey.Companion.CASCADE
        )
        // mechanical_units FK intentionally omitted — see class KDoc
    ],
    indices = [
        Index(value = ["island_id"]),
        Index(value = ["mechanical_unit_id"]),
        Index(value = ["operation_type"]),
        Index(value = ["outcome"]),
        Index(value = ["performed_at"]),
        Index(value = ["is_deleted"]),
    ]
)
data class MaintenanceLogEntity(

    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "island_id")
    val islandId: String,

    // ===== OPERATION =====
    @ColumnInfo(name = "operation_type")
    val operationType: String,                      // MaintenanceOperationType.name

    @ColumnInfo(name = "custom_operation_label")
    val customOperationLabel: String? = null,

    // ===== COMPONENT TARGET =====
    @ColumnInfo(name = "mechanical_unit_id")
    val mechanicalUnitId: String? = null,

    @ColumnInfo(name = "component_label")
    val componentLabel: String? = null,

    // ===== DESCRIPTION =====
    @ColumnInfo(name = "description")
    val description: String,

    // ===== TECHNICIAN SNAPSHOT =====
    @ColumnInfo(name = "technician_name")
    val technicianName: String,

    @ColumnInfo(name = "technician_company")
    val technicianCompany: String? = null,

    // ===== MACHINE STATE SNAPSHOT =====
    @ColumnInfo(name = "operating_hours_at_event")
    val operatingHoursAtEvent: Int? = null,

    @ColumnInfo(name = "cycle_count_at_event")
    val cycleCountAtEvent: Long? = null,

    // ===== OUTCOME =====
    @ColumnInfo(name = "outcome")
    val outcome: String,                            // MaintenanceOutcome.name

    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    // ===== META =====
    @ColumnInfo(name = "performed_at")
    val performedAt: Long,                          // epoch milliseconds

    @ColumnInfo(name = "created_at")
    val createdAt: Long,                            // epoch milliseconds

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,                            // epoch milliseconds

    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Boolean = true,

    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false
)