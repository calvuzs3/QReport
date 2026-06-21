package net.calvuz.qreport.checkup.modules.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Master data row for a checklist module type (e.g. "Sicurezza", "Meccanico").
 * Local-only: no synced_at/is_deleted, this table never syncs with the server
 * (see [net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster]).
 */
@Entity(
    tableName = "module_types",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["is_active"]),
        Index(value = ["sort_order"])
    ]
)
data class ModuleTypeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "icon_name")
    val iconName: String? = null,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
