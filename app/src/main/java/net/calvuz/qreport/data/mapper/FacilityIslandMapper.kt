package net.calvuz.qreport.data.mapper

import net.calvuz.qreport.data.local.entity.FacilityIslandEntity
import net.calvuz.qreport.domain.model.client.FacilityIsland
import kotlinx.datetime.Instant
import net.calvuz.qreport.domain.model.island.IslandType
import javax.inject.Inject

/**
 * Mapper per convertire tra FacilityIslandEntity (data layer) e FacilityIsland (domain layer)
 * Gestisce la conversione delle date Instant ↔ Long e IslandType enum
 */
class FacilityIslandMapper @Inject constructor() {

    /**
     * Converte da FacilityIslandEntity a FacilityIsland domain model
     */
    fun toDomain(entity: FacilityIslandEntity): FacilityIsland {
        return FacilityIsland(
            id = entity.id,
            facilityId = entity.facilityId,
            islandType = parseIslandType(entity.islandType),
            serialNumber = entity.serialNumber,
            model = entity.model,
            installationDate = entity.installationDate?.let {
                Instant.fromEpochMilliseconds(it)
            },
            warrantyExpiration = entity.warrantyExpiration?.let {
                Instant.fromEpochMilliseconds(it)
            },
            isActive = entity.isActive,
            operatingHours = entity.operatingHours,
            cycleCount = entity.cycleCount,
            lastMaintenanceDate = entity.lastMaintenanceDate?.let {
                Instant.fromEpochMilliseconds(it)
            },
            nextScheduledMaintenance = entity.nextScheduledMaintenance?.let {
                Instant.fromEpochMilliseconds(it)
            },
            customName = entity.customName,
            location = entity.location,
            notes = entity.notes,
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
        )
    }

    /**
     * Converte da FacilityIsland domain model a FacilityIslandEntity
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
     * Converte lista di FacilityIslandEntity a lista di FacilityIsland domain models
     */
    fun toDomainList(entities: List<FacilityIslandEntity>): List<FacilityIsland> {
        return entities.map { toDomain(it) }
    }

    /**
     * Converte lista di FacilityIsland domain models a lista di FacilityIslandEntity
     */
    fun toEntityList(domains: List<FacilityIsland>): List<FacilityIslandEntity> {
        return domains.map { toEntity(it) }
    }

    /**
     * Filtra islands per tipo
     */
    fun filterByType(entities: List<FacilityIslandEntity>, islandType: IslandType): List<FacilityIsland> {
        return entities
            .filter { it.islandType == islandType.name }
            .map { toDomain(it) }
    }

    /**
     * Filtra islands attive
     */
    fun getActiveIslands(entities: List<FacilityIslandEntity>): List<FacilityIsland> {
        return entities
            .filter { it.isActive }
            .map { toDomain(it) }
    }

    /**
     * Islands che richiedono manutenzione
     */
    fun getIslandsRequiringMaintenance(
        entities: List<FacilityIslandEntity>,
        currentTime: Long = System.currentTimeMillis()
    ): List<FacilityIsland> {
        return entities
            .filter { entity ->
                entity.isActive &&
                        entity.nextScheduledMaintenance != null &&
                        entity.nextScheduledMaintenance <= currentTime
            }
            .map { toDomain(it) }
    }

    /**
     * Parse IslandType da stringa con fallback
     */
    private fun parseIslandType(typeString: String): IslandType {
        return try {
            IslandType.valueOf(typeString)
        } catch (e: IllegalArgumentException) {
            //
            // Fallback per dati legacy o typos
            //
            when (typeString.uppercase()) {
                "MOVE" -> IslandType.POLY_MOVE
                "CAST" -> IslandType.POLY_CAST
                "EBT" -> IslandType.POLY_EBT
                "TAG_BLE" -> IslandType.POLY_TAG_BLE
                "TAG_FC" -> IslandType.POLY_TAG_FC
                "TAG_V" -> IslandType.POLY_TAG_V
                "SAMPLE" -> IslandType.POLY_SAMPLE
                else -> IslandType.POLY_MOVE // Fallback di default
            }
        }
    }
}

/**
 * Extension functions per conversioni dirette
 */

/**
 * Converte FacilityIslandEntity a FacilityIsland
 */
fun FacilityIslandEntity.toDomain(): FacilityIsland {
    return FacilityIslandMapper().toDomain(this)
}

/**
 * Converte FacilityIsland a FacilityIslandEntity
 */
fun FacilityIsland.toEntity(): FacilityIslandEntity {
    return FacilityIslandMapper().toEntity(this)
}

/**
 * Extension per nome display dell'isola
 */
fun FacilityIsland.displayName(): String {
    return customName?.takeIf { it.isNotBlank() }
        ?: "${islandType.name} - $serialNumber"
}

/**
 * Extension per verifica se l'isola è sotto garanzia
 */
fun FacilityIsland.isUnderWarranty(): Boolean {
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    return warrantyExpiration?.let { it > now } ?: false
}

/**
 * Extension per verifica se richiede manutenzione
 */
fun FacilityIsland.requiresMaintenance(): Boolean {
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    return nextScheduledMaintenance?.let { it <= now } ?: false
}

/**
 * Extension per calcolo giorni dalla last manutenzione
 */
fun FacilityIsland.daysSinceLastMaintenance(): Long? {
    return lastMaintenanceDate?.let { lastMaintenance ->
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val diffMillis = now.toEpochMilliseconds() - lastMaintenance.toEpochMilliseconds()
        diffMillis / (1000 * 60 * 60 * 24) // Convert to days
    }
}