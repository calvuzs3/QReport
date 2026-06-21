package net.calvuz.qreport.checkup.checkup.data.local.mapper

import net.calvuz.qreport.checkup.checkup.data.local.entity.CheckUpEntity
import net.calvuz.qreport.checkup.checkup.data.local.entity.CheckUpWithDetails
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUp
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUpHeader
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUpStatus
import net.calvuz.qreport.checkup.checkup.domain.model.ClientInfo
import net.calvuz.qreport.client.island.domain.model.IslandInfo
import net.calvuz.qreport.settings.domain.model.TechnicianInfo
import net.calvuz.qreport.checkup.items.data.local.mapper.toDomain

// ===============================
// CheckUp Mappers
// ===============================

fun CheckUpEntity.toDomain(): CheckUp {
    return CheckUp(
        id = this.id,
        header = CheckUpHeader(
            clientInfo = ClientInfo(
                companyName = this.clientCompanyName,
                contactPerson = this.clientContactPerson,
                site = this.clientSite,
                address = this.clientAddress,
                phone = this.clientPhone,
                email = this.clientEmail
            ),
            islandInfo = IslandInfo(
                serialNumber = this.islandSerialNumber,
                model = this.islandModel,
                installationDate = this.islandInstallationDate,
                lastMaintenanceDate = this.islandLastMaintenanceDate,
                operatingHours = this.islandOperatingHours,
                cycleCount = this.islandCycleCount
            ),
            technicianInfo = TechnicianInfo(
                name = this.technicianName,
                company = this.technicianCompany,
                certification = this.technicianCertification,
                phone = this.technicianPhone,
                email = this.technicianEmail
            ),
            checkUpDate = this.checkUpDate,
            notes = this.headerNotes
        ),
        islandType = this.islandType,
        islandTypeId = this.islandTypeId,
        status = CheckUpStatus.valueOf(this.status),
        checkItems = emptyList(), // Populated separately when needed
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        completedAt = this.completedAt
    )
}

fun CheckUp.toEntity(): CheckUpEntity {
    return CheckUpEntity(
        id = this.id,
        // Client info
        clientCompanyName = this.header.clientInfo.companyName,
        clientContactPerson = this.header.clientInfo.contactPerson,
        clientSite = this.header.clientInfo.site,
        clientAddress = this.header.clientInfo.address,
        clientPhone = this.header.clientInfo.phone,
        clientEmail = this.header.clientInfo.email,
        // Island info
        islandSerialNumber = this.header.islandInfo.serialNumber,
        islandModel = this.header.islandInfo.model,
        islandInstallationDate = this.header.islandInfo.installationDate,
        islandLastMaintenanceDate = this.header.islandInfo.lastMaintenanceDate,
        islandOperatingHours = this.header.islandInfo.operatingHours,
        islandCycleCount = this.header.islandInfo.cycleCount,
        // Technician info
        technicianName = this.header.technicianInfo.name,
        technicianCompany = this.header.technicianInfo.company,
        technicianCertification = this.header.technicianInfo.certification,
        technicianPhone = this.header.technicianInfo.phone,
        technicianEmail = this.header.technicianInfo.email,
        // CheckUp data
        checkUpDate = this.header.checkUpDate,
        headerNotes = this.header.notes,
        islandType = this.islandType,
        islandTypeId = this.islandTypeId,
        status = this.status.name,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        completedAt = this.completedAt
    )
}

fun CheckUpWithDetails.toDomain(): CheckUp {
    return checkUp.toDomain().copy(
        checkItems = checkItems.map { it.toDomain() }
    )
}