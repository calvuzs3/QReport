package net.calvuz.qreport.checkup.status.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Master data row for a checkup workflow status. Local-only: no synced_at/is_deleted,
 * this table never syncs with the server (see
 * [net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster]).
 */
@Entity(
    tableName = "checkup_statuses",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["is_active"]),
        Index(value = ["sort_order"])
    ]
)
data class CheckUpStatusEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "color_hex")
    val colorHex: String,

    @ColumnInfo(name = "icon_emoji")
    val iconEmoji: String? = null,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "blocks_deletion")
    val blocksDeletion: Boolean = false,

    @ColumnInfo(name = "marks_completion")
    val marksCompletion: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
