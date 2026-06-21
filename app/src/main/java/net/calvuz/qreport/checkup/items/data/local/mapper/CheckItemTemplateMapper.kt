package net.calvuz.qreport.checkup.items.data.local.mapper

import kotlinx.datetime.Instant
import net.calvuz.qreport.checkup.items.data.local.entity.CheckItemTemplateEntity
import net.calvuz.qreport.checkup.items.domain.model.CheckItemTemplateMaster
import javax.inject.Inject

class CheckItemTemplateMapper @Inject constructor() {

    fun toDomain(entity: CheckItemTemplateEntity): CheckItemTemplateMaster =
        CheckItemTemplateMaster(
            id = entity.id,
            moduleTypeId = entity.moduleTypeId,
            category = entity.category,
            description = entity.description,
            criticalityId = entity.criticalityId,
            orderIndex = entity.orderIndex,
            isActive = entity.isActive,
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
        )

    fun toEntity(domain: CheckItemTemplateMaster): CheckItemTemplateEntity = CheckItemTemplateEntity(
        id = domain.id,
        moduleTypeId = domain.moduleTypeId,
        category = domain.category,
        description = domain.description,
        criticalityId = domain.criticalityId,
        orderIndex = domain.orderIndex,
        isActive = domain.isActive,
        createdAt = domain.createdAt.toEpochMilliseconds(),
        updatedAt = domain.updatedAt.toEpochMilliseconds()
    )
}
