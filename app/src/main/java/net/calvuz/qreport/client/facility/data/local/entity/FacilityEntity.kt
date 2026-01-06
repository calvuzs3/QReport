package net.calvuz.qreport.client.facility.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import net.calvuz.qreport.client.client.data.local.entity.ClientEntity

/**
 * Entity Room per Facility
 * Mapping del domain model Facility per persistenza database
 */
@Entity(
    tableName = "facilities",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [
        Index(value = ["client_id"]),
        Index(value = ["name"]),
        Index(value = ["is_primary", "client_id"]),
        Index(value = ["is_active"])
    ]
)
data class FacilityEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "client_id")
    val clientId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "code")
    val code: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "facility_type")
    val facilityType: String, // FacilityType.name

    @ColumnInfo(name = "address_json")
    val addressJson: String, // JSON serialized Address

    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean = false,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)