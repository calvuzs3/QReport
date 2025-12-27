package net.calvuz.qreport.data.mapper

import net.calvuz.qreport.domain.model.*
import net.calvuz.qreport.data.local.entity.*
import net.calvuz.qreport.domain.model.checkup.CheckItem
import net.calvuz.qreport.domain.model.checkup.CheckItemStatus
import net.calvuz.qreport.domain.model.checkup.CheckUp
import net.calvuz.qreport.domain.model.checkup.CheckUpHeader
import net.calvuz.qreport.domain.model.checkup.CheckUpStatus
import net.calvuz.qreport.domain.model.island.IslandInfo
import net.calvuz.qreport.domain.model.island.IslandType
import net.calvuz.qreport.domain.model.module.ModuleType
import net.calvuz.qreport.domain.model.photo.Photo
import net.calvuz.qreport.domain.model.settings.TechnicianInfo
import net.calvuz.qreport.domain.model.spare.SparePart
import net.calvuz.qreport.domain.model.spare.SparePartCategory
import net.calvuz.qreport.domain.model.spare.SparePartUrgency

/**
 * Mappers per conversione tra Domain Models e Database Entities
 *
 * Estensioni per convertire facilmente tra i modelli del dominio
 * e le entità del database Room, mantenendo la separazione dei layer.
 *
 * AGGIORNATO: Usa TypeConverters automatici di Room invece di serializzazione manuale
 */

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
        islandType = IslandType.valueOf(this.islandType),
        status = CheckUpStatus.valueOf(this.status),
        checkItems = emptyList(), // Populated separately when needed
        spareParts = emptyList(), // Populated separately when needed
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
        islandType = this.islandType.name,
        status = this.status.name,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        completedAt = this.completedAt
    )
}

fun CheckUpWithDetails.toDomain(): CheckUp {
    return checkUp.toDomain().copy(
        checkItems = checkItems.map { it.toDomain() },
        spareParts = spareParts.map { it.toDomain() }
    )
}

// ===============================
// CheckItem Mappers
// ===============================

fun CheckItemEntity.toDomain(): CheckItem {
    return CheckItem(
        id = this.id,
        checkUpId = this.checkUpId,
        moduleType = ModuleType.valueOf(this.moduleType),
        itemCode = this.itemCode,
        description = this.description,
        status = CheckItemStatus.valueOf(this.status),
        criticality = CriticalityLevel.valueOf(this.criticality), // AGGIORNATO
        notes = this.notes,
        photos = emptyList(), // Populated separately when needed
        checkedAt = this.checkedAt,
        orderIndex = this.orderIndex
    )
}

fun CheckItem.toEntity(): CheckItemEntity {
    return CheckItemEntity(
        id = this.id,
        checkUpId = this.checkUpId,
        moduleType = this.moduleType.name,
        itemCode = this.itemCode,
        description = this.description,
        status = this.status.name,
        criticality = this.criticality.name, // AGGIORNATO
        notes = this.notes,
        checkedAt = this.checkedAt,
        orderIndex = this.orderIndex
    )
}

fun CheckItemWithPhotos.toDomain(): CheckItem {
    return checkItem.toDomain().copy(
        photos = photos.map { it.toDomain() }
    )
}

// ===============================
// SparePart Mappers
// ===============================

fun SparePartEntity.toDomain(): SparePart {
    return SparePart(
        id = this.id,
        checkUpId = this.checkUpId,
        partNumber = this.partNumber,
        description = this.description,
        category = SparePartCategory.valueOf(this.category),
        quantity = this.quantity,
        urgency = SparePartUrgency.valueOf(this.urgency),
        notes = this.notes,
        estimatedCost = this.estimatedCost,
        supplierInfo = this.supplierInfo,
        addedAt = this.addedAt
    )
}

fun SparePart.toEntity(): SparePartEntity {
    return SparePartEntity(
        id = this.id,
        checkUpId = this.checkUpId,
        partNumber = this.partNumber,
        description = this.description,
        category = this.category.name,
        quantity = this.quantity,
        urgency = this.urgency.name,
        notes = this.notes,
        estimatedCost = this.estimatedCost,
        supplierInfo = this.supplierInfo,
        addedAt = this.addedAt
    )
}


// ===============================
// List Extensions
// ===============================

fun List<CheckUpEntity>.toCheckUpDomainModels(): List<CheckUp> = map { it.toDomain() }
fun List<CheckUp>.toCheckUpEntities(): List<CheckUpEntity> = map { it.toEntity() }

fun List<CheckItemEntity>.toCheckItemDomainModels(): List<CheckItem> = map { it.toDomain() }
fun List<CheckItem>.toCheckItemEntities(): List<CheckItemEntity> = map { it.toEntity() }

fun List<PhotoEntity>.toPhotoDomainModels(): List<Photo> = map { it.toDomain() }
fun List<Photo>.toPhotoEntities(): List<PhotoEntity> = map { it.toEntity() }

fun List<SparePartEntity>.toSparePartDomainModels(): List<SparePart> = map { it.toDomain() }
fun List<SparePart>.toSparePartEntities(): List<SparePartEntity> = map { it.toEntity() }

fun List<CheckUpWithDetails>.toCheckUpWithDetailsDomainModels(): List<CheckUp> = map { it.toDomain() }
fun List<CheckItemWithPhotos>.toCheckItemWithPhotosDomainModels(): List<CheckItem> = map { it.toDomain() }

