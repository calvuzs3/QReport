package net.calvuz.qreport.client.island.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import net.calvuz.qreport.client.facility.data.local.entity.FacilityEntity

/**
 * Facility Island Entity Room
 *
 * Sync fields:
 *  - [updatedAt]  updated on every local write (create / edit / soft-delete)
 *  - [syncedAt]   set to [updatedAt] value after a successful push to the server;
 *                 null means the record has never been synced
 *  - [isDeleted]  soft-delete flag; the row is excluded from all normal queries
 *                 and pushed to the server so other devices can mirror the deletion
 */
@Entity(
    tableName = "facility_islands",
    foreignKeys = [
        ForeignKey(
            entity = FacilityEntity::class,
            parentColumns = ["id"],
            childColumns = ["facility_id"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [
        Index(value = ["facility_id"]),
        Index(value = ["serial_number"], unique = true),
        Index(value = ["island_type"]),
        Index(value = ["is_active"]),
        Index(value = ["next_scheduled_maintenance"]),
        Index(value = ["is_deleted"]),   // speeds up the WHERE is_deleted = 0 filter
        Index(value = ["updated_at"]),   // speeds up the delta query (updated_at > synced_at)
    ]
)
data class IslandEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "facility_id")
    val facilityId: String,

    // ===== COMMISSIONING =====

    @ColumnInfo("commissioning_number")
    val commissioningNumber: String? = null,

    // ===== ISLAND TYPE =====

    @ColumnInfo(name = "island_type")
    val islandType: String, // frozen display label (kept for backward compat)

    @ColumnInfo(name = "island_type_id")
    val islandTypeId: String? = null, // FK → island_types.id (nullable during Expand phase)

    // ===== TECHNICAL DETAILS =====

    @ColumnInfo(name = "serial_number")
    val serialNumber: String,

    @ColumnInfo("model_number")
    val modelNumber: String? = null,

    @ColumnInfo(name = "model")
    val model: String? = null,

    @ColumnInfo(name = "installation_date")
    val installationDate: Long? = null,

    @ColumnInfo(name = "warranty_expiration")
    val warrantyExpiration: Long? = null,

    // ===== STATUS =====

    @ColumnInfo(name = "operating_hours")
    val operatingHours: Int = 0,

    @ColumnInfo(name = "cycle_count")
    val cycleCount: Long = 0L,

    @ColumnInfo(name = "last_maintenance_date")
    val lastMaintenanceDate: Long? = null,

    @ColumnInfo(name = "next_scheduled_maintenance")
    val nextScheduledMaintenance: Long? = null,

    // ===== CONFIGURATION =====

    @ColumnInfo(name = "custom_name")
    val customName: String? = null,

    @ColumnInfo(name = "location")
    val location: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    // ===== META =====

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    // ===== SYNC =====
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null, // null = never synced; set after successful server push

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false // Soft-delete: row hidden in UI, pushed to server
)