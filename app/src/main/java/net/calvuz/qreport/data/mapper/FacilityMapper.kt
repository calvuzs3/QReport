package net.calvuz.qreport.data.mapper

import net.calvuz.qreport.data.local.converters.AddressConverter
import net.calvuz.qreport.data.local.entity.FacilityEntity
import net.calvuz.qreport.domain.model.client.Facility
import net.calvuz.qreport.domain.model.client.FacilityType
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * Mapper per convertire tra FacilityEntity (data layer) e Facility (domain layer)
 * Gestisce la conversione di Address tramite JSON serialization e FacilityType enum
 */
class FacilityMapper @Inject constructor(
    private val addressConverter: AddressConverter
) {

    /**
     * Converte da FacilityEntity a Facility domain model
     */
    fun toDomain(entity: FacilityEntity): Facility {
        return Facility(
            id = entity.id,
            clientId = entity.clientId,
            name = entity.name,
            code = entity.code,
            description = entity.description,
            facilityType = parseFacilityType(entity.facilityType),
            address = addressConverter.toAddress(entity.addressJson)
                ?: throw IllegalStateException("Facility must have a valid address"),
            isPrimary = entity.isPrimary,
            isActive = entity.isActive,
            islands = emptyList(), // Le islands vengono caricate separatamente
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
        )
    }

    /**
     * Converte da Facility domain model a FacilityEntity
     */
    fun toEntity(domain: Facility): FacilityEntity {
        return FacilityEntity(
            id = domain.id,
            clientId = domain.clientId,
            name = domain.name,
            code = domain.code,
            description = domain.description,
            facilityType = domain.facilityType.name,
            addressJson = addressConverter.fromAddress(domain.address)
                ?: throw IllegalStateException("Cannot serialize facility address"),
            isPrimary = domain.isPrimary,
            isActive = domain.isActive,
            createdAt = domain.createdAt.toEpochMilliseconds(),
            updatedAt = domain.updatedAt.toEpochMilliseconds()
        )
    }

    /**
     * Converte lista di FacilityEntity a lista di Facility domain models
     */
    fun toDomainList(entities: List<FacilityEntity>): List<Facility> {
        return entities.map { toDomain(it) }
    }

    /**
     * Converte lista di Facility domain models a lista di FacilityEntity
     */
    fun toEntityList(domains: List<Facility>): List<FacilityEntity> {
        return domains.map { toEntity(it) }
    }

    /**
     * Converte FacilityEntity con IDs delle islands associate
     * Utility per query complesse con JOIN
     */
    fun toDomainWithIslands(
        entity: FacilityEntity,
        islandIds: List<String> = emptyList()
    ): Facility {
        return Facility(
            id = entity.id,
            clientId = entity.clientId,
            name = entity.name,
            code = entity.code,
            description = entity.description,
            facilityType = parseFacilityType(entity.facilityType),
            address = addressConverter.toAddress(entity.addressJson)
                ?: throw IllegalStateException("Facility must have a valid address"),
            isPrimary = entity.isPrimary,
            isActive = entity.isActive,
            islands = islandIds, // IDs delle islands associate
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
        )
    }

    /**
     * Parse FacilityType da stringa con fallback
     */
    private fun parseFacilityType(typeString: String): FacilityType {
        return try {
            FacilityType.valueOf(typeString)
        } catch (e: IllegalArgumentException) {
            FacilityType.OTHER // Fallback per dati legacy
        }
    }
}

/**
 * Extension functions per conversioni dirette
 */

/**
 * Converte FacilityEntity a Facility
 */
fun FacilityEntity.toDomain(addressConverter: AddressConverter): Facility {
    return FacilityMapper(addressConverter).toDomain(this)
}

/**
 * Converte Facility a FacilityEntity
 */
fun Facility.toEntity(addressConverter: AddressConverter): FacilityEntity {
    return FacilityMapper(addressConverter).toEntity(this)
}