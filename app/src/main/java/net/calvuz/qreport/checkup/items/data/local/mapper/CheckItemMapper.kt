package net.calvuz.qreport.checkup.items.data.local.mapper

import net.calvuz.qreport.checkup.criticality.domain.model.CriticalityLevel
import net.calvuz.qreport.checkup.items.data.local.entity.CheckItemEntity
import net.calvuz.qreport.checkup.items.domain.model.CheckItem
import net.calvuz.qreport.checkup.items.domain.model.CheckItemStatus
import net.calvuz.qreport.checkup.modules.domain.model.ModuleType

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
