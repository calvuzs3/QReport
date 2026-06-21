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
        // find+fallback instead of valueOf(): a module/criticality code not in the
        // legacy enum must not crash the checkup screen, same convention as
        // IslandType.Companion.parse().
        moduleType = ModuleType.entries.find { it.name == this.moduleType } ?: ModuleType.MECHANICAL,
        moduleTypeId = this.moduleTypeId,
        itemCode = this.itemCode,
        description = this.description,
        status = CheckItemStatus.valueOf(this.status),
        criticality = CriticalityLevel.entries.find { it.name == this.criticality } ?: CriticalityLevel.ROUTINE,
        criticalityId = this.criticalityId,
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
        moduleTypeId = this.moduleTypeId,
        itemCode = this.itemCode,
        description = this.description,
        status = this.status.name,
        criticality = this.criticality.name, // AGGIORNATO
        criticalityId = this.criticalityId,
        notes = this.notes,
        checkedAt = this.checkedAt,
        orderIndex = this.orderIndex
    )
}
