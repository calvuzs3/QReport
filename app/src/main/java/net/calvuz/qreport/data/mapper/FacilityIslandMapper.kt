package net.calvuz.qreport.data.mapper

import kotlinx.datetime.Instant
import net.calvuz.qreport.data.local.entity.FacilityIslandEntity
import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.model.island.IslandType
import timber.log.Timber
import javax.inject.Inject

/**
 * Mapper per conversioni tra FacilityIslandEntity (data layer) e FacilityIsland (domain layer)
 *
 * ✅ Gestisce conversioni type-safe tra layers
 * ✅ Supporta enum IslandType con fallback
 * ✅ Conversioni Instant ↔ Long automatiche
 */
class FacilityIslandMapper @Inject constructor() {

    /**
     * Converte FacilityIslandEntity in FacilityIsland domain model
     */
    fun toDomain(entity: FacilityIslandEntity): FacilityIsland {
        return FacilityIsland(
            id = entity.id,
            facilityId = entity.facilityId,
            islandType = parseIslandType(entity.islandType),
            serialNumber = entity.serialNumber,
            model = entity.model,
            installationDate = entity.installationDate?.let { Instant.fromEpochMilliseconds(it) },
            warrantyExpiration = entity.warrantyExpiration?.let { Instant.fromEpochMilliseconds(it) },
            isActive = entity.isActive,
            operatingHours = entity.operatingHours,
            cycleCount = entity.cycleCount,
            lastMaintenanceDate = entity.lastMaintenanceDate?.let { Instant.fromEpochMilliseconds(it) },
            nextScheduledMaintenance = entity.nextScheduledMaintenance?.let { Instant.fromEpochMilliseconds(it) },
            customName = entity.customName,
            location = entity.location,
            notes = entity.notes,
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
        )
    }

    /**
     * Converte FacilityIsland domain model in FacilityIslandEntity
     */
    fun toEntity(domain: FacilityIsland): FacilityIslandEntity {
        return FacilityIslandEntity(
            id = domain.id,
            facilityId = domain.facilityId,
            islandType = domain.islandType.name,
            serialNumber = domain.serialNumber,
            model = domain.model,
            installationDate = domain.installationDate?.toEpochMilliseconds(),
            warrantyExpiration = domain.warrantyExpiration?.toEpochMilliseconds(),
            isActive = domain.isActive,
            operatingHours = domain.operatingHours,
            cycleCount = domain.cycleCount,
            lastMaintenanceDate = domain.lastMaintenanceDate?.toEpochMilliseconds(),
            nextScheduledMaintenance = domain.nextScheduledMaintenance?.toEpochMilliseconds(),
            customName = domain.customName,
            location = domain.location,
            notes = domain.notes,
            createdAt = domain.createdAt.toEpochMilliseconds(),
            updatedAt = domain.updatedAt.toEpochMilliseconds()
        )
    }

    /**
     * Converte lista di FacilityIslandEntity in lista di FacilityIsland domain models
     */
    fun toDomainList(entities: List<FacilityIslandEntity>): List<FacilityIsland> {
        return entities.map { toDomain(it) }
    }

    /**
     * Converte lista di FacilityIsland domain models in lista di FacilityIslandEntity
     */
    fun toEntityList(domains: List<FacilityIsland>): List<FacilityIslandEntity> {
        return domains.map { toEntity(it) }
    }

    /**
     * Parsing sicuro di IslandType con fallback
     *
     * Se il tipo non è riconosciuto, usa un default o lancia eccezione
     * controllata per evitare crash runtime
     */
    private fun parseIslandType(islandTypeString: String): IslandType {
        return try {
            IslandType.valueOf(islandTypeString)
        } catch (_: IllegalArgumentException) {
            // Log l'errore e usa un fallback
             Timber.w("Unknown island type: $islandTypeString, using POLY_MOVE as fallback")

            // Prova parsing case-insensitive
            IslandType.entries.find {
                it.name.equals(islandTypeString, ignoreCase = true)
            } ?: IslandType.POLY_MOVE // Default fallback
        }
    }
}

/**
 * Extension functions per convenience
 */
fun FacilityIslandEntity.toDomain(mapper: FacilityIslandMapper): FacilityIsland =
    mapper.toDomain(this)

fun FacilityIsland.toEntity(mapper: FacilityIslandMapper): FacilityIslandEntity =
    mapper.toEntity(this)

fun List<FacilityIslandEntity>.toDomain(mapper: FacilityIslandMapper): List<FacilityIsland> =
    mapper.toDomainList(this)

fun List<FacilityIsland>.toEntity(mapper: FacilityIslandMapper): List<FacilityIslandEntity> =
    mapper.toEntityList(this)