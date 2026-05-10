package net.calvuz.qreport.client.unit.data.local.mapper

import kotlinx.datetime.Instant
import net.calvuz.qreport.client.unit.data.local.entity.MechanicalUnitEntity
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.model.UnitType
import javax.inject.Inject


/**
 * Mapper between [MechanicalUnitEntity] (data layer) and [MechanicalUnit] (domain layer).
 */
class MechanicalUnitMapper @Inject constructor() {

    fun toDomain(entity: MechanicalUnitEntity): MechanicalUnit = MechanicalUnit(
        id           = entity.id,
        islandId     = entity.islandId,
        unitType     = UnitType.valueOf(entity.unitType),
        name         = entity.name,
        serialNumber = entity.serialNumber,
        model        = entity.model,
        notes        = entity.notes,
        isActive     = entity.isActive,
        createdAt    = Instant.fromEpochMilliseconds(entity.createdAt),
        updatedAt    = Instant.fromEpochMilliseconds(entity.updatedAt)
    )

    fun toEntity(domain: MechanicalUnit): MechanicalUnitEntity = MechanicalUnitEntity(
        id           = domain.id,
        islandId     = domain.islandId,
        unitType     = domain.unitType.name,
        name         = domain.name,
        serialNumber = domain.serialNumber,
        model        = domain.model,
        notes        = domain.notes,
        isActive     = domain.isActive,
        createdAt    = domain.createdAt.toEpochMilliseconds(),
        updatedAt    = domain.updatedAt.toEpochMilliseconds()
    )

    fun toDomainList(entities: List<MechanicalUnitEntity>): List<MechanicalUnit> =
        entities.map { toDomain(it) }
}

