package net.calvuz.qreport.client.island.data.local.mapper

import kotlinx.datetime.Instant
import net.calvuz.qreport.client.island.data.local.entity.IslandTypeEntity
import net.calvuz.qreport.client.island.domain.model.IslandTypeMaster
import javax.inject.Inject

/**
 * Maps between [IslandTypeEntity] (data layer) and [IslandTypeMaster] (domain layer).
 *
 * [IslandTypeEntity.isDeleted] and [IslandTypeEntity.syncedAt] are data-layer concerns:
 * [toEntity] never sets them, leaving the entity defaults (null/false), exactly like
 * [net.calvuz.qreport.client.client.data.local.mapper.ClientMapper] — any edit is left
 * pending re-sync.
 */
class IslandTypeMapper @Inject constructor() {

    fun toDomain(entity: IslandTypeEntity): IslandTypeMaster = IslandTypeMaster(
        id = entity.id,
        code = entity.code,
        label = entity.label,
        description = entity.description,
        iconName = entity.iconName,
        maintenanceIntervalDays = entity.maintenanceIntervalDays,
        sortOrder = entity.sortOrder,
        isActive = entity.isActive,
        createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
        updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
    )

    fun toEntity(domain: IslandTypeMaster): IslandTypeEntity = IslandTypeEntity(
        id = domain.id,
        code = domain.code,
        label = domain.label,
        description = domain.description,
        iconName = domain.iconName,
        maintenanceIntervalDays = domain.maintenanceIntervalDays,
        sortOrder = domain.sortOrder,
        isActive = domain.isActive,
        createdAt = domain.createdAt.toEpochMilliseconds(),
        updatedAt = domain.updatedAt.toEpochMilliseconds()
    )

    fun toDomainList(entities: List<IslandTypeEntity>): List<IslandTypeMaster> = entities.map { toDomain(it) }

    fun toEntityList(domains: List<IslandTypeMaster>): List<IslandTypeEntity> = domains.map { toEntity(it) }
}
