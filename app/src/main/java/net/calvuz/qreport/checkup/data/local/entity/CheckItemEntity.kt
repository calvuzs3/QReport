package net.calvuz.qreport.checkup.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * Entità CheckItem
 */
@Entity(
    tableName = "check_items",
    foreignKeys = [
        ForeignKey(
            entity = CheckUpEntity::class,
            parentColumns = ["id"],
            childColumns = ["checkup_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["checkup_id"]),
        Index(value = ["module_type"]),
        Index(value = ["status"]),
        Index(value = ["criticality"]),
        Index(value = ["order_index"])
    ]
)
data class CheckItemEntity (
    @PrimaryKey val id: String,
    @ColumnInfo(name = "checkup_id") val checkUpId: String,
    @ColumnInfo(name = "module_type") val moduleType: String,
    @ColumnInfo(name = "item_code") val itemCode: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "criticality") val criticality: String,
    @ColumnInfo(name = "notes") val notes: String,
    @ColumnInfo(name = "checked_at") val checkedAt: Instant?,
    @ColumnInfo(name = "order_index") val orderIndex: Int
)
