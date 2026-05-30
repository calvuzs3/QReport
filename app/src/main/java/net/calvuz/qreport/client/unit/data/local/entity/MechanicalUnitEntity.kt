package net.calvuz.qreport.client.unit.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import net.calvuz.qreport.client.island.data.local.entity.IslandEntity

/**
 * Mechanical Unit Entity Room 
 *
 * Sync fields:
 *  - [updatedAt]  updated on every local write (create / edit / soft-delete)
 *  - [syncedAt]   set to [updatedAt] value after a successful push to the server;
 *                 null means the record has never been synced
 *  - [isDeleted]  soft-delete flag; the row is excluded from all normal queries
 *                 and pushed to the server so other devices can mirror the deletion
 */
@Entity(
    tableName = "mechanical_units",
    foreignKeys = [
        ForeignKey(
            entity = IslandEntity::class,
            parentColumns = ["id"],
            childColumns = ["island_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["island_id"]),
        Index(value = ["is_active"]),
        Index(value = ["is_deleted"]),   // speeds up the WHERE is_deleted = 0 filter
        Index(value = ["updated_at"]),   // speeds up the delta query (updated_at > synced_at)
    ]
)
data class MechanicalUnitEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "island_id")
    val islandId: String,

    @ColumnInfo(name = "unit_type")
    val unitType: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "serial_number")
    val serialNumber: String?,

    @ColumnInfo(name = "model")
    val model: String?,

    @ColumnInfo(name = "notes")
    val notes: String?,

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
