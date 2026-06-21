package net.calvuz.qreport.checkup.modules.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Many-to-many link: which island types a [net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster]
 * applies to — drives which check-item-template modules get pulled into a new
 * checkup's checklist for a given island type. No enforced Room
 * [androidx.room.ForeignKey], same soft-reference convention as the rest of this
 * master-data layer.
 */
@Entity(
    tableName = "module_type_island_types",
    primaryKeys = ["island_type_id", "module_type_id"],
    indices = [
        Index(value = ["module_type_id"])
    ]
)
data class ModuleTypeIslandTypeCrossRef(
    @ColumnInfo(name = "island_type_id")
    val islandTypeId: String,

    @ColumnInfo(name = "module_type_id")
    val moduleTypeId: String
)
