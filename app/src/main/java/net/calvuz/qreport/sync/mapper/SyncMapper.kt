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
import net.calvuz.qreport.checkup.criticality.data.local.entity.CriticalityEntity
import net.calvuz.qreport.checkup.items.data.local.entity.CheckItemTemplateEntity
import net.calvuz.qreport.checkup.modules.data.local.entity.ModuleTypeEntity
import net.calvuz.qreport.checkup.status.data.local.entity.CheckUpStatusEntity
import net.calvuz.qreport.client.island.data.local.entity.IslandTypeEntity
import net.calvuz.qreport.checkup.checkup.data.local.entity.CheckUpEntity
import net.calvuz.qreport.checkup.checkup.data.local.entity.CheckUpIslandAssociationEntity
import net.calvuz.qreport.sync.data.remote.dto.CheckItemTemplateDto
import net.calvuz.qreport.sync.data.remote.dto.CheckUpIslandAssociationDto
import net.calvuz.qreport.sync.data.remote.dto.CheckUpRecordDto
import net.calvuz.qreport.sync.data.remote.dto.CheckUpStatusDto
import kotlinx.datetime.Instant
import net.calvuz.qreport.sync.data.remote.dto.ClientDto
import net.calvuz.qreport.sync.data.remote.dto.ContactDto
import net.calvuz.qreport.sync.data.remote.dto.ContractDto
import net.calvuz.qreport.sync.data.remote.dto.CriticalityLevelDto
import net.calvuz.qreport.sync.data.remote.dto.FacilityDto
import net.calvuz.qreport.sync.data.remote.dto.FacilityIslandDto
import net.calvuz.qreport.sync.data.remote.dto.IslandDocumentDto
import net.calvuz.qreport.sync.data.remote.dto.IslandTypeDto
import net.calvuz.qreport.sync.data.remote.dto.MaintenanceLogDto
import net.calvuz.qreport.sync.data.remote.dto.MechanicalUnitDto
import net.calvuz.qreport.sync.data.remote.dto.ModuleTypeDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps Room entities to remote DTOs (for push) and remote DTOs to Room entities (for pull).
 */
@Singleton
class SyncMapper @Inject constructor() {

    // ===== ISLAND TYPE =====

    fun islandTypeToDto(entity: IslandTypeEntity) = IslandTypeDto(
        id = entity.id,
        code = entity.code,
        label = entity.label,
        description = entity.description,
        iconName = entity.iconName,
        maintenanceIntervalDays = entity.maintenanceIntervalDays,
        sortOrder = entity.sortOrder,
        isActive = entity.isActive,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        syncedAt = entity.syncedAt,
        isDeleted = entity.isDeleted
    )

    fun islandTypeToEntity(dto: IslandTypeDto) = IslandTypeEntity(
        id = dto.id,
        code = dto.code,
        label = dto.label,
        description = dto.description,
        iconName = dto.iconName,
        maintenanceIntervalDays = dto.maintenanceIntervalDays,
        sortOrder = dto.sortOrder,
        isActive = dto.isActive,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        syncedAt = dto.syncedAt,
        isDeleted = dto.isDeleted
    )

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
        islandTypeId = entity.islandTypeId,
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
        islandTypeId = dto.islandTypeId,
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

    // ===== CHECKUP MASTER DATA =====

    fun moduleTypeToDto(entity: ModuleTypeEntity) = ModuleTypeDto(
        id = entity.id,
        code = entity.code,
        label = entity.label,
        description = entity.description,
        iconName = entity.iconName,
        sortOrder = entity.sortOrder,
        isActive = entity.isActive,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        syncedAt = entity.syncedAt,
        isDeleted = entity.isDeleted
    )

    fun moduleTypeToEntity(dto: ModuleTypeDto) = ModuleTypeEntity(
        id = dto.id,
        code = dto.code,
        label = dto.label,
        description = dto.description,
        iconName = dto.iconName,
        sortOrder = dto.sortOrder,
        isActive = dto.isActive,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        syncedAt = dto.syncedAt,
        isDeleted = dto.isDeleted
    )

    fun criticalityLevelToDto(entity: CriticalityEntity) = CriticalityLevelDto(
        id = entity.id,
        code = entity.code,
        label = entity.label,
        priority = entity.priority,
        colorHex = entity.colorHex,
        iconEmoji = entity.iconEmoji,
        sortOrder = entity.sortOrder,
        isActive = entity.isActive,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        syncedAt = entity.syncedAt,
        isDeleted = entity.isDeleted
    )

    fun criticalityLevelToEntity(dto: CriticalityLevelDto) = CriticalityEntity(
        id = dto.id,
        code = dto.code,
        label = dto.label,
        priority = dto.priority,
        colorHex = dto.colorHex,
        iconEmoji = dto.iconEmoji,
        sortOrder = dto.sortOrder,
        isActive = dto.isActive,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        syncedAt = dto.syncedAt,
        isDeleted = dto.isDeleted
    )

    fun checkUpStatusToDto(entity: CheckUpStatusEntity) = CheckUpStatusDto(
        id = entity.id,
        code = entity.code,
        label = entity.label,
        colorHex = entity.colorHex,
        iconEmoji = entity.iconEmoji,
        sortOrder = entity.sortOrder,
        isActive = entity.isActive,
        blocksDeletion = entity.blocksDeletion,
        marksCompletion = entity.marksCompletion,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        syncedAt = entity.syncedAt,
        isDeleted = entity.isDeleted
    )

    fun checkUpStatusToEntity(dto: CheckUpStatusDto) = CheckUpStatusEntity(
        id = dto.id,
        code = dto.code,
        label = dto.label,
        colorHex = dto.colorHex,
        iconEmoji = dto.iconEmoji,
        sortOrder = dto.sortOrder,
        isActive = dto.isActive,
        blocksDeletion = dto.blocksDeletion,
        marksCompletion = dto.marksCompletion,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        syncedAt = dto.syncedAt,
        isDeleted = dto.isDeleted
    )

    fun checkItemTemplateToDto(entity: CheckItemTemplateEntity) = CheckItemTemplateDto(
        id = entity.id,
        moduleTypeId = entity.moduleTypeId,
        category = entity.category,
        description = entity.description,
        criticalityId = entity.criticalityId,
        orderIndex = entity.orderIndex,
        isActive = entity.isActive,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        syncedAt = entity.syncedAt,
        isDeleted = entity.isDeleted
    )

    fun checkUpToDto(entity: CheckUpEntity) = CheckUpRecordDto(
        id = entity.id,
        clientCompanyName = entity.clientCompanyName,
        clientContactPerson = entity.clientContactPerson,
        clientSite = entity.clientSite,
        clientAddress = entity.clientAddress,
        clientPhone = entity.clientPhone,
        clientEmail = entity.clientEmail,
        islandSerialNumber = entity.islandSerialNumber,
        islandModel = entity.islandModel,
        islandInstallationDate = entity.islandInstallationDate,
        islandLastMaintenanceDate = entity.islandLastMaintenanceDate,
        islandOperatingHours = entity.islandOperatingHours,
        islandCycleCount = entity.islandCycleCount,
        technicianName = entity.technicianName,
        technicianCompany = entity.technicianCompany,
        technicianCertification = entity.technicianCertification,
        technicianPhone = entity.technicianPhone,
        technicianEmail = entity.technicianEmail,
        checkupDate = entity.checkUpDate.toEpochMilliseconds(),
        headerNotes = entity.headerNotes,
        islandType = entity.islandType,
        islandTypeId = entity.islandTypeId,
        status = entity.status,
        createdAt = entity.createdAt.toEpochMilliseconds(),
        updatedAt = entity.updatedAt.toEpochMilliseconds(),
        completedAt = entity.completedAt?.toEpochMilliseconds(),
        syncedAt = entity.syncedAt,
        isDeleted = entity.isDeleted
    )

    fun checkUpToEntity(dto: CheckUpRecordDto) = CheckUpEntity(
        id = dto.id,
        clientCompanyName = dto.clientCompanyName,
        clientContactPerson = dto.clientContactPerson,
        clientSite = dto.clientSite,
        clientAddress = dto.clientAddress,
        clientPhone = dto.clientPhone,
        clientEmail = dto.clientEmail,
        islandSerialNumber = dto.islandSerialNumber,
        islandModel = dto.islandModel,
        islandInstallationDate = dto.islandInstallationDate,
        islandLastMaintenanceDate = dto.islandLastMaintenanceDate,
        islandOperatingHours = dto.islandOperatingHours,
        islandCycleCount = dto.islandCycleCount,
        technicianName = dto.technicianName,
        technicianCompany = dto.technicianCompany,
        technicianCertification = dto.technicianCertification,
        technicianPhone = dto.technicianPhone,
        technicianEmail = dto.technicianEmail,
        checkUpDate = Instant.fromEpochMilliseconds(dto.checkupDate),
        headerNotes = dto.headerNotes,
        islandType = dto.islandType,
        islandTypeId = dto.islandTypeId,
        status = dto.status,
        createdAt = Instant.fromEpochMilliseconds(dto.createdAt),
        updatedAt = Instant.fromEpochMilliseconds(dto.updatedAt),
        completedAt = dto.completedAt?.let { Instant.fromEpochMilliseconds(it) },
        syncedAt = dto.syncedAt,
        isDeleted = dto.isDeleted
    )

    fun checkUpIslandAssociationToDto(entity: CheckUpIslandAssociationEntity) = CheckUpIslandAssociationDto(
        id = entity.id,
        checkupId = entity.checkupId,
        islandId = entity.islandId,
        associationType = entity.associationType,
        notes = entity.notes,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        syncedAt = entity.syncedAt
    )

    fun checkUpIslandAssociationToEntity(dto: CheckUpIslandAssociationDto) = CheckUpIslandAssociationEntity(
        id = dto.id,
        checkupId = dto.checkupId,
        islandId = dto.islandId,
        associationType = dto.associationType,
        notes = dto.notes,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        syncedAt = dto.syncedAt
    )

    fun checkItemTemplateToEntity(dto: CheckItemTemplateDto) = CheckItemTemplateEntity(
        id = dto.id,
        moduleTypeId = dto.moduleTypeId,
        category = dto.category,
        description = dto.description,
        criticalityId = dto.criticalityId,
        orderIndex = dto.orderIndex,
        isActive = dto.isActive,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        syncedAt = dto.syncedAt,
        isDeleted = dto.isDeleted
    )
}