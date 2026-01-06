package net.calvuz.qreport.checkup.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class per i template dei check items
 */
@Entity(tableName = "check_item_templates")
data class CheckItemTemplateEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "module_type")
    val moduleType: String,

    val category: String,

    val description: String,

    val criticality: String,

    @ColumnInfo(name = "order_index")
    val orderIndex: Int,

    @ColumnInfo(name = "island_types")
    val islandTypes: String // JSON string dei tipi isola
)