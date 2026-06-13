@file:Suppress("HardCodedStringLiteral")
package net.calvuz.qreport.client.document.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the island_documents table.
 *
 * Scope FK design:
 *  - [islandId], [facilityId], [clientId] are all nullable.
 *  - No DB-level FK constraints (same pattern as MaintenanceLogEntity.mechanicalUnitId):
 *    GLOBAL documents have no FK, and documents must survive parent
 *    deactivation / deletion without orphan constraint violations.
 *  - Referential integrity is enforced at the use case level.
 *
 * Lifecycle fields follow the project-wide two-stage soft-delete convention:
 *  - [isActive]   false = document logically deactivated (first delete stage)
 *  - [isDeleted]  true  = document marked for purge / server sync (second stage)
 *  - [updatedAt]  updated on every write; used for sync conflict resolution
 *
 * All normal queries filter WHERE is_deleted = 0.
 *
 * The file at [filePath] must be deleted by the delete use case when [isDeleted]
 * transitions to true — DB record and filesystem are always kept in sync.
 */
@Entity(
    tableName = "island_documents",
    indices = [
        Index(value = ["scope"]),
        Index(value = ["island_id"]),
        Index(value = ["facility_id"]),
        Index(value = ["client_id"]),
        Index(value = ["category"]),
        Index(value = ["is_active"]),
        Index(value = ["is_deleted"]),
        Index(value = ["updated_at"])
    ]
    // No foreignKeys — see class KDoc
)
data class DocumentEntity(

    @PrimaryKey
    val id: String,

    // ===== SCOPE =====

    @ColumnInfo(name = "scope")
    val scope: String,                          // DocumentScope.name

    @ColumnInfo(name = "island_id")
    val islandId: String? = null,               // non-null when scope == ISLAND

    @ColumnInfo(name = "facility_id")
    val facilityId: String? = null,             // non-null when scope == FACILITY

    @ColumnInfo(name = "client_id")
    val clientId: String? = null,               // non-null when scope == CLIENT
    // all null when scope == GLOBAL

    // ===== FILE =====

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "file_size")
    val fileSize: Long,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name="file_hash")
    val fileHash: String? = null,              // null until first sync; computed at import

    // ===== METADATA =====

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "category")
    val category: String,                       // DocumentCategory.name

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    // ===== LIFECYCLE =====

    @ColumnInfo(name = "created_at")
    val createdAt: Long,                        // epoch milliseconds

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,                        // epoch milliseconds

    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Boolean = true,

    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,

    // ===== SYNC =====

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null                  // null = never synced
)