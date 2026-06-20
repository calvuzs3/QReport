package net.calvuz.qreport.checkup.checkup.data.local.mapper

import net.calvuz.qreport.checkup.checkup.data.local.entity.SparePartEntity
import net.calvuz.qreport.checkup.checkup.domain.model.spare.SparePart
import net.calvuz.qreport.checkup.checkup.domain.model.spare.SparePartCategory
import net.calvuz.qreport.checkup.checkup.domain.model.spare.SparePartUrgency

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