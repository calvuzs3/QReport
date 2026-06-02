package net.calvuz.qreport.client.island.data.local.mapper

import kotlinx.datetime.Instant
import net.calvuz.qreport.client.island.data.local.entity.IslandEntity
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandType
import timber.log.Timber
import javax.inject.Inject

/**
 * Mapper per conversioni tra IslandEntity (data layer) e Island (domain layer)
 *
 * ✅ Gestisce conversioni type-safe tra layers
 * ✅ Supporta enum IslandType con fallback
 * ✅ Conversioni Instant ↔ Long automatiche
 */
class IslandMapper @Inject constructor() {

    /**
     * Converte IslandEntity in Island domain model
     */
    fun toDomain(entity: IslandEntity): Island {
        return Island(
            id = entity.id,
            facilityId = entity.facilityId,
            commissioningNumber = entity.commissioningNumber,
            islandType = IslandType.parseIslandType(entity.islandType),
            serialNumber = entity.serialNumber,
            modelNumber = entity.modelNumber,
            installationDate = entity.installationDate?.let { Instant.fromEpochMilliseconds(it) },
            warrantyExpiration = entity.warrantyExpiration?.let { Instant.fromEpochMilliseconds(it) },
            operatingHours = entity.operatingHours,
            cycleCount = entity.cycleCount,
            lastMaintenanceDate = entity.lastMaintenanceDate?.let { Instant.fromEpochMilliseconds(it) },
            nextScheduledMaintenance = entity.nextScheduledMaintenance?.let {
                Instant.fromEpochMilliseconds(
                    it
                )
            },
            customName = entity.customName,
            location = entity.location,
            notes = entity.notes,
            isActive = entity.isActive,
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt),
        )
    }

    /**
     * Converte Island domain model in IslandEntity
     */
    fun toEntity(domain: Island): IslandEntity {
        return IslandEntity(
            id = domain.id,
            facilityId = domain.facilityId,
            commissioningNumber = domain.commissioningNumber,
            islandType = domain.islandType.name,
            serialNumber = domain.serialNumber,
            modelNumber = domain.modelNumber,
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

    /**
     * Converte lista di IslandEntity in lista di Island domain models
     */
    fun toDomainList(entities: List<IslandEntity>): List<Island> {
        return entities.map { toDomain(it) }
    }

    /**
     * Converte lista di Island domain models in lista di IslandEntity
     */
    fun toEntityList(domains: List<Island>): List<IslandEntity> {
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
             Timber.w("UnknownError island type: $islandTypeString, using POLY_MOVE as fallback")

            // Prova parsing case-insensitive
            IslandType.entries.find {
                it.name.equals(islandTypeString, ignoreCase = true)
            } ?: IslandType.POLY_MOVE // Default fallback
        }
    }
}

/*
 * Parsing of an Island Type
 *
 * Fallback: PolyMove
 */
fun IslandType.Companion.parseIslandType(islandType: String): IslandType {
    return try {
        IslandType.valueOf(islandType)
    } catch (_: IllegalArgumentException) {
        // Log the error and use a fallback
        Timber.w("UnknownError island type: $islandType, using POLY_MOVE as fallback")

        // Try case-insensitive parsing
        IslandType.entries.find {
            it.name.equals(islandType, ignoreCase = true)
        } ?: IslandType.POLY_MOVE // Default fallback
    }
}

/**
 * Extension functions per convenience
 */
fun IslandEntity.toDomain(mapper: IslandMapper): Island =
    mapper.toDomain(this)

fun Island.toEntity(mapper: IslandMapper): IslandEntity =
    mapper.toEntity(this)

fun List<IslandEntity>.toDomain(mapper: IslandMapper): List<Island> =
    mapper.toDomainList(this)

fun List<Island>.toEntity(mapper: IslandMapper): List<IslandEntity> =
    mapper.toEntityList(this)