package net.calvuz.qreport.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index
import kotlinx.datetime.Instant

@Entity(
    tableName = "checkups",
    indices = [
        Index(value = ["status"]),
        Index(value = ["island_type"]),
        Index(value = ["created_at"]),
        Index(value = ["client_company_name"])
    ]
)
data class CheckUpEntity(
    @PrimaryKey val id: String,

    // Header info - embedded per semplicità
    @ColumnInfo(name = "client_company_name") val clientCompanyName: String,
    @ColumnInfo(name = "client_contact_person") val clientContactPerson: String,
    @ColumnInfo(name = "client_site") val clientSite: String,
    @ColumnInfo(name = "client_address") val clientAddress: String,
    @ColumnInfo(name = "client_phone") val clientPhone: String,
    @ColumnInfo(name = "client_email") val clientEmail: String,

    @ColumnInfo(name = "island_serial_number") val islandSerialNumber: String,
    @ColumnInfo(name = "island_model") val islandModel: String,
    @ColumnInfo(name = "island_installation_date") val islandInstallationDate: String,
    @ColumnInfo(name = "island_last_maintenance_date") val islandLastMaintenanceDate: String,
    @ColumnInfo(name = "island_operating_hours") val islandOperatingHours: Int,
    @ColumnInfo(name = "island_cycle_count") val islandCycleCount: Long,

    @ColumnInfo(name = "technician_name") val technicianName: String,
    @ColumnInfo(name = "technician_company") val technicianCompany: String,
    @ColumnInfo(name = "technician_certification") val technicianCertification: String,
    @ColumnInfo(name = "technician_phone") val technicianPhone: String,
    @ColumnInfo(name = "technician_email") val technicianEmail: String,

    @ColumnInfo(name = "checkup_date") val checkUpDate: Instant,
    @ColumnInfo(name = "header_notes") val headerNotes: String,

    // CheckUp data
    @ColumnInfo(name = "island_type") val islandType: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
    @ColumnInfo(name = "completed_at") val completedAt: Instant?
)

