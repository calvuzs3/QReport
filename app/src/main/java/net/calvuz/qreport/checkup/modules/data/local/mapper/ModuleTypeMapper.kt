package net.calvuz.qreport.checkup.modules.data.local.mapper

import kotlinx.datetime.Instant
import net.calvuz.qreport.checkup.modules.data.local.entity.ModuleTypeEntity
import net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster
import javax.inject.Inject

class ModuleTypeMapper @Inject constructor() {

    fun toDomain(entity: ModuleTypeEntity): ModuleTypeMaster = ModuleTypeMaster(
        id = entity.id,
        code = entity.code,
        label = entity.label,
        description = entity.description,
        iconName = entity.iconName,
        sortOrder = entity.sortOrder,
        isActive = entity.isActive,
        createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
        updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
    )

    fun toEntity(domain: ModuleTypeMaster): ModuleTypeEntity = ModuleTypeEntity(
        id = domain.id,
        code = domain.code,
        label = domain.label,
        description = domain.description,
        iconName = domain.iconName,
        sortOrder = domain.sortOrder,
        isActive = domain.isActive,
        createdAt = domain.createdAt.toEpochMilliseconds(),
        updatedAt = domain.updatedAt.toEpochMilliseconds()
    )

    fun toDomainList(entities: List<ModuleTypeEntity>): List<ModuleTypeMaster> = entities.map { toDomain(it) }
}
