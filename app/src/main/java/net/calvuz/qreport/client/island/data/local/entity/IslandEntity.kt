package net.calvuz.qreport.client.island.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import net.calvuz.qreport.client.facility.data.local.entity.FacilityEntity

/**
 * Entity Room per Island
 * Mapping del domain model Island per persistenza database
 */
@Entity(
    tableName = "facility_islands",
    foreignKeys = [
        ForeignKey(
            entity = FacilityEntity::class,
            parentColumns = ["id"],
            childColumns = ["facility_id"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [
        Index(value = ["facility_id"]),
        Index(value = ["serial_number"], unique = true),
        Index(value = ["island_type"]),
        Index(value = ["is_active"]),
        Index(value = ["next_scheduled_maintenance"])
    ]
)
data class IslandEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "facility_id")
    val facilityId: String,

    // TIPO ISOLA
    @ColumnInfo(name = "island_type")
    val islandType: String, // IslandType.name

    // DETTAGLI TECNICI
    @ColumnInfo(name = "serial_number")
    val serialNumber: String,

    @ColumnInfo(name = "model")
    val model: String? = null,

    @ColumnInfo(name = "installation_date")
    val installationDate: Long? = null,

    @ColumnInfo(name = "warranty_expiration")
    val warrantyExpiration: Long? = null,

    // STATO OPERATIVO
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "operating_hours")
    val operatingHours: Int = 0,

    @ColumnInfo(name = "cycle_count")
    val cycleCount: Long = 0L,

    @ColumnInfo(name = "last_maintenance_date")
    val lastMaintenanceDate: Long? = null,

    @ColumnInfo(name = "next_scheduled_maintenance")
    val nextScheduledMaintenance: Long? = null,

    // CONFIGURAZIONE
    @ColumnInfo(name = "custom_name")
    val customName: String? = null,

    @ColumnInfo(name = "location")
    val location: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    // METADATI
    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)