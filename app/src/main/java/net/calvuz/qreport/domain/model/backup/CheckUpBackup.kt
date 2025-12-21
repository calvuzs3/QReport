package net.calvuz.qreport.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * CheckUpBackup - Backup dei CheckUp
 */
@Serializable
data class CheckUpBackup(
    val id: String,

    val clientCompanyName: String,
    val clientContactPerson: String,
    val clientSite: String,
    val clientAddress: String,
    val clientPhone: String,
    val clientEmail: String,

    val islandSerialNumber: String,
    val islandModel: String,
     val islandInstallationDate: String,
     val islandLastMaintenanceDate: String,
    val islandOperatingHours: Int,
    val islandCycleCount: Long,

    val technicianName: String,
    val technicianCompany: String,
    val technicianCertification: String,
    val technicianPhone: String,
    val technicianEmail: String,

    @Contextual val checkUpDate: Instant,
    val headerNotes: String,

    val islandType: String,
    val status: String,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
    @Contextual val completedAt: Instant?
)