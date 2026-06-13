@file:Suppress("HardCodedStringLiteral")
package net.calvuz.qreport.sync.mapper

import net.calvuz.qreport.client.client.data.local.entity.ClientEntity
import net.calvuz.qreport.client.contact.data.local.entity.ContactEntity
import net.calvuz.qreport.client.contract.data.local.entity.ContractEntity
import net.calvuz.qreport.client.document.data.local.entity.DocumentEntity
import net.calvuz.qreport.client.facility.data.local.entity.FacilityEntity
import net.calvuz.qreport.client.island.data.local.entity.IslandEntity
import net.calvuz.qreport.client.island.maintenance.data.local.entity.MaintenanceLogEntity
import net.calvuz.qreport.client.unit.data.local.entity.MechanicalUnitEntity
import net.calvuz.qreport.sync.data.remote.dto.ClientDto
import net.calvuz.qreport.sync.data.remote.dto.ContactDto
import net.calvuz.qreport.sync.data.remote.dto.ContractDto
import net.calvuz.qreport.sync.data.remote.dto.FacilityDto
import net.calvuz.qreport.sync.data.remote.dto.FacilityIslandDto
import net.calvuz.qreport.sync.data.remote.dto.IslandDocumentDto
import net.calvuz.qreport.sync.data.remote.dto.MaintenanceLogDto
import net.calvuz.qreport.sync.data.remote.dto.MechanicalUnitDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps Room entities to remote DTOs (for push) and remote DTOs to Room entities (for pull).
 */
@Singleton
class SyncMapper @Inject constructor() {

    // ===== CLIENT =====

    fun clientToDto(entity: ClientEntity) = ClientDto(
        id = entity.id,
        companyName = entity.companyName,
        notes = entity.notes,
        headquartersJson = entity.headquartersJson,
        isActive = entity.isActive,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        syncedAt = entity.syncedAt,
        isDeleted = entity.isDeleted
    )

    fun clientToEntity(dto: ClientDto) = ClientEntity(
        id = dto.id,
        companyName = dto.companyName,
        notes = dto.notes,
        headquartersJson = dto.headquartersJson,
        isActive = dto.isActive,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        syncedAt = dto.syncedAt,
        isDeleted = dto.isDeleted
    )

    // ===== CONTACT =====

    fun contactToDto(entity: ContactEntity) = ContactDto(
        id = entity.id,
        clientId = entity.clientId,
        firstName = entity.firstName,
        lastName = entity.lastName,
        title = entity.title,
        role = entity.role,
        department = entity.department,
        phone = entity.phone,
        mobilePhone = entity.mobilePhone,
        email = entity.email,
        alternativeEmail = entity.alternativeEmail,
        isPrimary = entity.isPrimary,
        preferredContactMethod = entity.preferredContactMethod,
        notes = entity.notes,
        isActive = entity.isActive,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        syncedAt = entity.syncedAt,
        isDeleted = entity.isDeleted
    )

    fun contactToEntity(dto: ContactDto) = ContactEntity(
        id = dto.id,
        clientId = dto.clientId,
        firstName = dto.firstName,
        lastName = dto.lastName,
        title = dto.title,
        role = dto.role,
        department = dto.department,
        phone = dto.phone,
        mobilePhone = dto.mobilePhone,
        email = dto.email,
        alternativeEmail = dto.alternativeEmail,
        isPrimary = dto.isPrimary,
        preferredContactMethod = dto.preferredContactMethod,
        notes = dto.notes,
        isActive = dto.isActive,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        syncedAt = dto.syncedAt,
        isDeleted = dto.isDeleted
    )

    // ===== CONTRACT =====

    fun contractToDto(entity: ContractEntity) = ContractDto(
        id = entity.id,
        clientId = entity.clientId,
        name = entity.name,
        description = entity.description,
        startDate = entity.startDate,
        endDate = entity.endDate,
        hasPriority = entity.hasPriority,
        hasRemoteAssistance = entity.hasRemoteAssistance,
        hasMaintenance = entity.hasMaintenance,
        notes = entity.notes,
        isActive = entity.isActive,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        syncedAt = entity.syncedAt,
        isDeleted = entity.isDeleted
    )

    fun contractToEntity(dto: ContractDto) = ContractEntity(
        id = dto.id,
        clientId = dto.clientId,
        name = dto.name,
        description = dto.description,
        startDate = dto.startDate,
        endDate = dto.endDate,
        hasPriority = dto.hasPriority,
        hasRemoteAssistance = dto.hasRemoteAssistance,
        hasMaintenance = dto.hasMaintenance,
        notes = dto.notes,
        isActive = dto.isActive,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        syncedAt = dto.syncedAt,
        isDeleted = dto.isDeleted
    )

    // ===== FACILITY =====

    fun facilityToDto(entity: FacilityEntity) = FacilityDto(
        id = entity.id,
        clientId = entity.clientId,
        name = entity.name,
        code = entity.code,
        notes = entity.notes,
        facilityType = entity.facilityType,
        addressJson = entity.addressJson,
        isPrimary = entity.isPrimary,
        isActive = entity.isActive,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        syncedAt = entity.syncedAt,
        isDeleted = entity.isDeleted
    )

    fun facilityToEntity(dto: FacilityDto) = FacilityEntity(
        id = dto.id,
        clientId = dto.clientId,
        name = dto.name,
        code = dto.code,
        notes = dto.notes,
        facilityType = dto.facilityType,
        addressJson = dto.addressJson,
        isPrimary = dto.isPrimary,
        isActive = dto.isActive,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        syncedAt = dto.syncedAt,
        isDeleted = dto.isDeleted
    )

    // ===== FACILITY ISLAND =====

    fun facilityIslandToDto(entity: IslandEntity) = FacilityIslandDto(
        id = entity.id,
        facilityId = entity.facilityId,
        commissioningNumber = entity.commissioningNumber,
        islandType = entity.islandType,
        serialNumber = entity.serialNumber,
        modelNumber = entity.modelNumber,
        model = entity.model,
        installationDate = entity.installationDate,
        warrantyExpiration = entity.warrantyExpiration,
        operatingHours = entity.operatingHours,   // Int in entity, Int in DTO
        cycleCount = entity.cycleCount,
        lastMaintenanceDate = entity.lastMaintenanceDate,
        nextScheduledMaintenance = entity.nextScheduledMaintenance,
        customName = entity.customName,
        location = entity.location,
        notes = entity.notes,
        isActive = entity.isActive,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        syncedAt = entity.syncedAt,
        isDeleted = entity.isDeleted
    )

    fun facilityIslandToEntity(dto: FacilityIslandDto) = IslandEntity(
        id = dto.id,
        facilityId = dto.facilityId,
        commissioningNumber = dto.commissioningNumber,
        islandType = dto.islandType,
        serialNumber = dto.serialNumber,
        modelNumber = dto.modelNumber,
        model = dto.model,
        installationDate = dto.installationDate,
        warrantyExpiration = dto.warrantyExpiration,
        operatingHours = dto.operatingHours,
        cycleCount = dto.cycleCount,
        lastMaintenanceDate = dto.lastMaintenanceDate,
        nextScheduledMaintenance = dto.nextScheduledMaintenance,
        customName = dto.customName,
        location = dto.location,
        notes = dto.notes,
        isActive = dto.isActive,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        syncedAt = dto.syncedAt,
        isDeleted = dto.isDeleted
    )

    // ===== MECHANICAL UNIT =====

    fun mechanicalUnitToDto(entity: MechanicalUnitEntity) = MechanicalUnitDto(
        id = entity.id,
        islandId = entity.islandId,
        unitType = entity.unitType,
        name = entity.name,
        serialNumber = entity.serialNumber,
        model = entity.model,
        notes = entity.notes,
        isActive = entity.isActive,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        syncedAt = entity.syncedAt,
        isDeleted = entity.isDeleted
    )

    fun mechanicalUnitToEntity(dto: MechanicalUnitDto) = MechanicalUnitEntity(
        id = dto.id,
        islandId = dto.islandId,
        unitType = dto.unitType,
        name = dto.name,
        serialNumber = dto.serialNumber,
        model = dto.model,
        notes = dto.notes,
        isActive = dto.isActive,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        syncedAt = dto.syncedAt,
        isDeleted = dto.isDeleted
    )

    // ===== MAINTENANCE LOG =====

    fun maintenanceLogToDto(entity: MaintenanceLogEntity) = MaintenanceLogDto(
        id = entity.id,
        islandId = entity.islandId,
        operationType = entity.operationType,
        customOperationLabel = entity.customOperationLabel,
        mechanicalUnitId = entity.mechanicalUnitId,
        componentLabel = entity.componentLabel,
        description = entity.description,
        technicianName = entity.technicianName,
        technicianCompany = entity.technicianCompany,
        operatingHoursAtEvent = entity.operatingHoursAtEvent,
        cycleCountAtEvent = entity.cycleCountAtEvent,
        outcome = entity.outcome,
        durationMinutes = entity.durationMinutes,
        notes = entity.notes,
        performedAt = entity.performedAt,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        syncedAt = entity.syncedAt,
        isActive = entity.isActive,
        isDeleted = entity.isDeleted
    )

    fun maintenanceLogToEntity(dto: MaintenanceLogDto) = MaintenanceLogEntity(
        id = dto.id,
        islandId = dto.islandId,
        operationType = dto.operationType,
        customOperationLabel = dto.customOperationLabel,
        mechanicalUnitId = dto.mechanicalUnitId,
        componentLabel = dto.componentLabel,
        description = dto.description,
        technicianName = dto.technicianName,
        technicianCompany = dto.technicianCompany,
        operatingHoursAtEvent = dto.operatingHoursAtEvent,
        cycleCountAtEvent = dto.cycleCountAtEvent,
        outcome = dto.outcome,
        durationMinutes = dto.durationMinutes,
        notes = dto.notes,
        performedAt = dto.performedAt,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        syncedAt = dto.syncedAt,
        isActive = dto.isActive,
        isDeleted = dto.isDeleted
    )

    // ===== END =====

    @Suppress("unused")
    fun islandDocumentToDto(entity: DocumentEntity) = IslandDocumentDto(
        id = entity.id,
        scope = entity.scope,
        islandId = entity.islandId,
        facilityId = entity.facilityId,
        clientId = entity.clientId,
        fileName = entity.fileName,
        filePath = entity.filePath,
        fileSize = entity.fileSize,
        mimeType = entity.mimeType,
        fileHash = entity.fileHash,
        title = entity.title,
        category = entity.category,
        notes = entity.notes,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        syncedAt = entity.syncedAt,
        isActive = entity.isActive,
        isDeleted = entity.isDeleted
    )
    
    @Suppress("unused")
    fun islandDocumentToEntity(dto: IslandDocumentDto) = DocumentEntity(
        id = dto.id,
        scope = dto.scope,
        islandId = dto.islandId,
        facilityId = dto.facilityId,
        clientId = dto.clientId,
        fileName = dto.fileName,
        filePath = dto.filePath,
        fileSize = dto.fileSize,
        mimeType = dto.mimeType,
        fileHash = dto.fileHash,
        title = dto.title,
        category = dto.category,
        notes = dto.notes,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        syncedAt = dto.syncedAt,
        isActive = dto.isActive,
        isDeleted = dto.isDeleted
    )
}