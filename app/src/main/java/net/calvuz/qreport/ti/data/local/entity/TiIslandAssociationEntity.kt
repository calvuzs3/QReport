package net.calvuz.qreport.ti.data.local.entity

import androidx.room.*
import net.calvuz.qreport.client.island.data.local.entity.IslandEntity

@Entity(
    tableName = "ti_island_associations",
    foreignKeys = [
        ForeignKey(
            entity = TechnicalInterventionEntity::class,
            parentColumns = ["id"],
            childColumns = ["intervention_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = IslandEntity::class,
            parentColumns = ["id"],
            childColumns = ["island_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["intervention_id"]),
        Index(value = ["island_id"]),
        Index(value = ["association_type"]),
        Index(value = ["is_deleted"]),
        Index(value = ["intervention_id", "island_id"], unique = true)
    ]
)
data class TiIslandAssociationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "intervention_id") val interventionId: String,
    @ColumnInfo(name = "island_id") val islandId: String,
    @ColumnInfo(name = "association_type") val associationType: String,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false
)
