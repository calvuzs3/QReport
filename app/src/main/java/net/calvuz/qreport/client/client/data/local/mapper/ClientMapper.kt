package net.calvuz.qreport.client.client.data.local.mapper

import net.calvuz.qreport.app.app.data.converter.AddressConverter
import net.calvuz.qreport.client.client.data.local.entity.ClientEntity
import net.calvuz.qreport.client.client.domain.model.Client
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * Mapper per convertire tra ClientEntity (data layer) e Client (domain layer)
 * Gestisce la conversione di Address tramite JSON serialization
 */
class ClientMapper @Inject constructor(
    private val addressConverter: AddressConverter
) {

    /**
     * Converte da ClientEntity a Client domain model
     */
    fun toDomain(entity: ClientEntity): Client {
        return Client(
            id = entity.id,
            companyName = entity.companyName,
            vatNumber = entity.vatNumber,
            fiscalCode = entity.fiscalCode,
            website = entity.website,
            industry = entity.industry,
            notes = entity.notes,
            headquarters = addressConverter.toAddress(entity.headquartersJson),
            facilities = emptyList(), // Le facilities vengono caricate separatamente
            contacts = emptyList(),   // I contacts vengono caricati separatamente
            isActive = entity.isActive,
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
        )
    }

    /**
     * Converte da Client domain model a ClientEntity
     */
    fun toEntity(domain: Client): ClientEntity {
        return ClientEntity(
            id = domain.id,
            companyName = domain.companyName,
            vatNumber = domain.vatNumber,
            fiscalCode = domain.fiscalCode,
            website = domain.website,
            industry = domain.industry,
            notes = domain.notes,
            headquartersJson = addressConverter.fromAddress(domain.headquarters),
            isActive = domain.isActive,
            createdAt = domain.createdAt.toEpochMilliseconds(),
            updatedAt = domain.updatedAt.toEpochMilliseconds()
        )
    }

    /**
     * Converte lista di ClientEntity a lista di Client domain models
     */
    fun toDomainList(entities: List<ClientEntity>): List<Client> {
        return entities.map { toDomain(it) }
    }

    /**
     * Converte lista di Client domain models a lista di ClientEntity
     */
    fun toEntityList(domains: List<Client>): List<ClientEntity> {
        return domains.map { toEntity(it) }
    }

    /**
     * Converte ClientEntity con dati relazionali completi (facilities + contacts)
     * Utility per query complesse con JOIN
     */
    fun toDomainWithRelations(
        entity: ClientEntity,
        facilityIds: List<String> = emptyList(),
        contactIds: List<String> = emptyList()
    ): Client {
        return Client(
            id = entity.id,
            companyName = entity.companyName,
            vatNumber = entity.vatNumber,
            fiscalCode = entity.fiscalCode,
            website = entity.website,
            industry = entity.industry,
            notes = entity.notes,
            headquarters = addressConverter.toAddress(entity.headquartersJson),
            facilities = facilityIds,  // IDs delle facilities associate
            contacts = contactIds,     // IDs dei contacts associati
            isActive = entity.isActive,
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
        )
    }
}

/**
 * Extension functions per conversioni dirette
 */

/**
 * Converte ClientEntity a Client
 */
fun ClientEntity.toDomain(addressConverter: AddressConverter): Client {
    return ClientMapper(addressConverter).toDomain(this)
}

/**
 * Converte Client a ClientEntity
 */
fun Client.toEntity(addressConverter: AddressConverter): ClientEntity {
    return ClientMapper(addressConverter).toEntity(this)
}