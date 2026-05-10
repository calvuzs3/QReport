package net.calvuz.qreport.client.unit.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import net.calvuz.qreport.client.island.data.local.entity.IslandEntity

@Entity(
    tableName = "mechanical_units",
    foreignKeys = [
        ForeignKey(
            entity = IslandEntity::class,
            parentColumns = ["id"],
            childColumns = ["island_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["island_id"]),
        Index(value = ["is_active"])
    ]
)
data class MechanicalUnitEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "island_id")
    val islandId: String,

    @ColumnInfo(name = "unit_type")
    val unitType: String,                   // UnitType.name()

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "serial_number")
    val serialNumber: String?,

    @ColumnInfo(name = "model")
    val model: String?,

    @ColumnInfo(name = "notes")
    val notes: String?,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
