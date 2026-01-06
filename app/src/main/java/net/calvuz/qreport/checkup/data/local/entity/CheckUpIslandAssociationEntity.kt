package net.calvuz.qreport.checkup.data.local.entity

import androidx.room.*
import net.calvuz.qreport.client.island.data.local.entity.IslandEntity

/**
 * Entity per associare CheckUp alle Isole Robotizzate
 * Supporta associazioni multiple e tipologie diverse
 */
@Entity(
    tableName = "checkup_island_associations",
    foreignKeys = [
        ForeignKey(
            entity = CheckUpEntity::class,
            parentColumns = ["id"],
            childColumns = ["checkup_id"],
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
        Index(value = ["checkup_id"]),
        Index(value = ["island_id"]),
        Index(value = ["association_type"]),
        Index(value = ["checkup_id", "island_id"], unique = true) // Evita duplicati
    ]
)
data class CheckUpIslandAssociationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "checkup_id") val checkupId: String,
    @ColumnInfo(name = "island_id") val islandId: String,
    @ColumnInfo(name = "association_type") val associationType: String, // AssociationType.name
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)