package net.calvuz.qreport.client.client.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Client Entity Room
 *
 * Sync fields:
 *  - [updatedAt]  updated on every local write (create / edit / soft-delete)
 *  - [syncedAt]   set to [updatedAt] value after a successful push to the server;
 *                 null means the record has never been synced
 *  - [isDeleted]  soft-delete flag; the row is excluded from all normal queries
 *                 and pushed to the server so other devices can mirror the deletion
 */
@Entity(
    tableName = "clients",
    indices = [
        Index(value = ["company_name"]),
        Index(value = ["is_active"]),
        Index(value = ["is_deleted"]),   // speeds up the WHERE is_deleted = 0 filter
        Index(value = ["updated_at"]),   // speeds up the delta query (updated_at > synced_at)
    ]
)
data class ClientEntity(
    @PrimaryKey val id: String,

    // ===== DATA =====
    @ColumnInfo(name = "company_name") val companyName: String, val notes: String?,

    // ===== HEADQUARTERS JSON =====
    @ColumnInfo(name = "headquarters_json") val headquartersJson: String?, // Serialized JSON of Address object

    // ===== METADATA =====
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,

    @ColumnInfo(name = "created_at") val createdAt: Long, // Timestamp in milliseconds

    @ColumnInfo(name = "updated_at") val updatedAt: Long, // Timestamp in milliseconds — updated on every local write

    // ===== SYNC =====
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null, // null = never synced; set after successful server push

    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false // Soft-delete: row hidden in UI, pushed to server
)