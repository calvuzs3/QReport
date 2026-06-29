package net.calvuz.qreport.checkup.items.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Master data row for a checklist template item (the master replacement for the
 * hardcoded `CheckItemModules` object). [moduleTypeId]/[criticalityId] are soft
 * references to `module_types`/`criticality_levels` — no enforced Room
 * [androidx.room.ForeignKey], same convention as `island_type_id` on
 * [net.calvuz.qreport.client.island.data.local.entity.IslandEntity]. Which island
 * types a template surfaces in is decided by its module's island-type links, see
 * [net.calvuz.qreport.checkup.modules.data.local.entity.ModuleTypeIslandTypeCrossRef].
 */
@Entity(
    tableName = "check_item_templates",
    indices = [
        Index(value = ["module_type_id"]),
        Index(value = ["criticality_id"]),
        Index(value = ["is_active"]),
        Index(value = ["order_index"])
    ]
)
data class CheckItemTemplateEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "module_type_id")
    val moduleTypeId: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "criticality_id")
    val criticalityId: String,

    @ColumnInfo(name = "order_index")
    val orderIndex: Int,

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
