package net.calvuz.qreport.client.facility.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import net.calvuz.qreport.client.client.data.local.entity.ClientEntity

/**
 * Facility Entity Room
 *
 * Sync fields:
 *  - [updatedAt]  updated on every local write (create / edit / soft-delete)
 *  - [syncedAt]   set to [updatedAt] value after a successful push to the server;
 *                 null means the record has never been synced
 *  - [isDeleted]  soft-delete flag; the row is excluded from all normal queries
 *                 and pushed to the server so other devices can mirror the deletion
*/
@Entity(
    tableName = "facilities",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [
        Index(value = ["client_id"]),
        Index(value = ["name"]),
        Index(value = ["is_primary", "client_id"]),
        Index(value = ["is_active"]),
        Index(value = ["is_deleted"]),   // speeds up the WHERE is_deleted = 0 filter
        Index(value = ["updated_at"]),   // speeds up the delta query (updated_at > synced_at)
    ]
)
data class FacilityEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "client_id")
    val clientId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "code")
    val code: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "facility_type")
    val facilityType: String, // FacilityType.name

    @ColumnInfo(name = "address_json")
    val addressJson: String?, // JSON serialized Address

    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean = false,

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