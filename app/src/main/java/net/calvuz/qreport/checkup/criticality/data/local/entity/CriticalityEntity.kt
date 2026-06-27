package net.calvuz.qreport.checkup.criticality.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Master data row for a checklist criticality level (e.g. "Critico", "Routine"). */
@Entity(
    tableName = "criticality_levels",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["is_active"]),
        Index(value = ["sort_order"])
    ]
)
data class CriticalityEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "priority")
    val priority: Int,

    @ColumnInfo(name = "color_hex")
    val colorHex: String,

    @ColumnInfo(name = "icon_emoji")
    val iconEmoji: String? = null,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)
