package net.calvuz.qreport.checkup.criticality.data.local.mapper

import kotlinx.datetime.Instant
import net.calvuz.qreport.checkup.criticality.data.local.entity.CriticalityEntity
import net.calvuz.qreport.checkup.criticality.domain.model.CriticalityMaster
import javax.inject.Inject

class CriticalityMapper @Inject constructor() {

    fun toDomain(entity: CriticalityEntity): CriticalityMaster = CriticalityMaster(
        id = entity.id,
        code = entity.code,
        label = entity.label,
        priority = entity.priority,
        colorHex = entity.colorHex,
        iconEmoji = entity.iconEmoji,
        sortOrder = entity.sortOrder,
        isActive = entity.isActive,
        createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
        updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
    )

    fun toEntity(domain: CriticalityMaster): CriticalityEntity = CriticalityEntity(
        id = domain.id,
        code = domain.code,
        label = domain.label,
        priority = domain.priority,
        colorHex = domain.colorHex,
        iconEmoji = domain.iconEmoji,
        sortOrder = domain.sortOrder,
        isActive = domain.isActive,
        createdAt = domain.createdAt.toEpochMilliseconds(),
        updatedAt = domain.updatedAt.toEpochMilliseconds()
    )

    fun toDomainList(entities: List<CriticalityEntity>): List<CriticalityMaster> = entities.map { toDomain(it) }
}
