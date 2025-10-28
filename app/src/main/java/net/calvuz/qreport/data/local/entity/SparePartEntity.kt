package net.calvuz.qreport.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * Entità SparePart
 */
@Entity(
    tableName = "spare_parts",
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
        Index(value = ["urgency"]),
        Index(value = ["category"]),
        Index(value = ["added_at"])
    ]
)
data class SparePartEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "checkup_id") val checkUpId: String,
    @ColumnInfo(name = "part_number") val partNumber: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "quantity") val quantity: Int,
    @ColumnInfo(name = "urgency") val urgency: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "estimated_cost") val estimatedCost: Double?,
    @ColumnInfo(name = "notes") val notes: String,
    @ColumnInfo(name = "supplier_info") val supplierInfo: String,
    @ColumnInfo(name = "added_at") val addedAt: Instant
)