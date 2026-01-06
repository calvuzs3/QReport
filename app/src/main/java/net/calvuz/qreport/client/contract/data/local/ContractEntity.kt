package net.calvuz.qreport.client.contract.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import net.calvuz.qreport.client.client.data.local.entity.ClientEntity

@Entity(
    tableName = "contracts",
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
        Index(value = ["has_maintenance"])
    ]
)
data class ContractEntity(

    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "client_id")
    val clientId: String,

    @ColumnInfo(name = "name")
    val name: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "start_date")
    val startDate: Long,

    @ColumnInfo(name = "end_date")
    val endDate: Long,

    @ColumnInfo(name = "has_priority")
    val hasPriority: Boolean = true,

    @ColumnInfo(name = "has_remote_assistance")
    val hasRemoteAssistance: Boolean = true,

    @ColumnInfo(name = "has_maintenance")
    val hasMaintenance: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)