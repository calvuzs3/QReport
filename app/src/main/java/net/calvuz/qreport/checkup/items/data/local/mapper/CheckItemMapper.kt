package net.calvuz.qreport.checkup.items.data.local.mapper

import net.calvuz.qreport.checkup.criticality.domain.model.CriticalityLevel
import net.calvuz.qreport.checkup.items.data.local.entity.CheckItemEntity
import net.calvuz.qreport.checkup.items.domain.model.CheckItem
import net.calvuz.qreport.checkup.items.domain.model.CheckItemStatus

fun CheckItemEntity.toDomain(): CheckItem {
    return CheckItem(
        id = this.id,
        checkUpId = this.checkUpId,
        // module_type_id is canonical; fall back to legacy module_type column for rows
        // written before the ModuleTypeMaster migration (their enum name == master id).
        moduleTypeId = this.moduleTypeId ?: this.moduleType,
        itemCode = this.itemCode,
        description = this.description,
        status = CheckItemStatus.valueOf(this.status),
        criticality = CriticalityLevel.entries.find { it.name == this.criticality } ?: CriticalityLevel.ROUTINE,
        criticalityId = this.criticalityId,
        notes = this.notes,
        photos = emptyList(),
        checkedAt = this.checkedAt,
        orderIndex = this.orderIndex
    )
}

fun CheckItem.toEntity(): CheckItemEntity {
    return CheckItemEntity(
        id = this.id,
        checkUpId = this.checkUpId,
        moduleType = this.moduleTypeId,   // keep legacy column in sync for old readers
        moduleTypeId = this.moduleTypeId,
        itemCode = this.itemCode,
        description = this.description,
        status = this.status.name,
        criticality = this.criticality.name,
        criticalityId = this.criticalityId,
        notes = this.notes,
        checkedAt = this.checkedAt,
        orderIndex = this.orderIndex
    )
}
