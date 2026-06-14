package net.calvuz.qreport.checkup.data.local.mapper

import net.calvuz.qreport.app.app.domain.model.CriticalityLevel
import net.calvuz.qreport.checkup.data.local.entity.CheckItemEntity
import net.calvuz.qreport.checkup.data.local.entity.CheckItemWithPhotos
import net.calvuz.qreport.checkup.domain.model.CheckItem
import net.calvuz.qreport.checkup.domain.model.CheckItemStatus
import net.calvuz.qreport.checkup.domain.model.module.ModuleType
import net.calvuz.qreport.photo.data.local.mapper.toDomain

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