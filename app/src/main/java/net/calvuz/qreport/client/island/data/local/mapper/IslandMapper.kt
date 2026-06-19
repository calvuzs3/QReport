package net.calvuz.qreport.client.island.data.local.mapper

import kotlinx.datetime.Instant
import net.calvuz.qreport.client.island.data.local.entity.IslandEntity
import net.calvuz.qreport.client.island.domain.model.Island
import javax.inject.Inject

/**
 * Maps between [IslandEntity] (data layer) and [Island] (domain layer).
 *
 * Handles [Instant] ↔ epoch-milliseconds [Long] conversion.
 */
class IslandMapper @Inject constructor() {

    fun toDomain(entity: IslandEntity): Island {
        return Island(
            id = entity.id,
            facilityId = entity.facilityId,
            commissioningNumber = entity.commissioningNumber,
            islandType = entity.islandType,
            islandTypeId = entity.islandTypeId,
            serialNumber = entity.serialNumber,
            installationDate = entity.installationDate?.let { Instant.fromEpochMilliseconds(it) },
            warrantyExpiration = entity.warrantyExpiration?.let { Instant.fromEpochMilliseconds(it) },
            operatingHours = entity.operatingHours,
            cycleCount = entity.cycleCount,
            lastMaintenanceDate = entity.lastMaintenanceDate?.let { Instant.fromEpochMilliseconds(it) },
            nextScheduledMaintenance = entity.nextScheduledMaintenance?.let { Instant.fromEpochMilliseconds(it) },
            customName = entity.customName,
            location = entity.location,
            notes = entity.notes,
            isActive = entity.isActive,
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt),
        )
    }

    fun toEntity(domain: Island): IslandEntity {
        return IslandEntity(
            id = domain.id,
            facilityId = domain.facilityId,
            commissioningNumber = domain.commissioningNumber,
            islandType = domain.islandType,
            islandTypeId = domain.islandTypeId,
            serialNumber = domain.serialNumber,
            installationDate = domain.installationDate?.toEpochMilliseconds(),
            warrantyExpiration = domain.warrantyExpiration?.toEpochMilliseconds(),
            operatingHours = domain.operatingHours,
            cycleCount = domain.cycleCount,
            lastMaintenanceDate = domain.lastMaintenanceDate?.toEpochMilliseconds(),
            nextScheduledMaintenance = domain.nextScheduledMaintenance?.toEpochMilliseconds(),
            customName = domain.customName,
            location = domain.location,
            notes = domain.notes,
            isActive = domain.isActive,
            createdAt = domain.createdAt.toEpochMilliseconds(),
            updatedAt = domain.updatedAt.toEpochMilliseconds()
        )
    }

    fun toDomainList(entities: List<IslandEntity>): List<Island> = entities.map { toDomain(it) }

    fun toEntityList(domains: List<Island>): List<IslandEntity> = domains.map { toEntity(it) }
}

// Convenience extensions

fun IslandEntity.toDomain(mapper: IslandMapper): Island = mapper.toDomain(this)

fun Island.toEntity(mapper: IslandMapper): IslandEntity = mapper.toEntity(this)

fun List<IslandEntity>.toDomain(mapper: IslandMapper): List<Island> = mapper.toDomainList(this)

fun List<Island>.toEntity(mapper: IslandMapper): List<IslandEntity> = mapper.toEntityList(this)