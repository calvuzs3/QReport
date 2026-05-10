package net.calvuz.qreport.client.client.data.local.mapper

import net.calvuz.qreport.app.app.data.converter.AddressConverter
import net.calvuz.qreport.client.client.data.local.entity.ClientEntity
import net.calvuz.qreport.client.client.domain.model.Client
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * Mapper to convert between ClientEntity (data layer) and Client (domain layer)
 */
class ClientMapper @Inject constructor(
    private val addressConverter: AddressConverter
) {

    fun toDomain(entity: ClientEntity): Client {
        return Client(
            id = entity.id,
            companyName = entity.companyName,

            notes = entity.notes,
            headquarters = addressConverter.toAddress(entity.headquartersJson),
            facilities = emptyList(), // Separately loaded
            contacts = emptyList(),   // Separately loaded
            contracts = emptyList(),  // Separately loaded
            isActive = entity.isActive,
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
        )
    }

    fun toEntity(domain: Client): ClientEntity {
        return ClientEntity(
            id = domain.id,
            companyName = domain.companyName,
            notes = domain.notes,
            headquartersJson = addressConverter.fromAddress(domain.headquarters),
            isActive = domain.isActive,
            createdAt = domain.createdAt.toEpochMilliseconds(),
            updatedAt = domain.updatedAt.toEpochMilliseconds()
        )
    }

    fun toDomainList(entities: List<ClientEntity>): List<Client> {
        return entities.map { toDomain(it) }
    }

    fun toEntityList(domains: List<Client>): List<ClientEntity> {
        return domains.map { toEntity(it) }
    }

    /**
     * Complex queries  Helper method
     * Convert ClientEntity with relational data
     */
    fun toDomainWithRelations(
        entity: ClientEntity,
        facilityIds: List<String> = emptyList(),
        contactIds: List<String> = emptyList(),
        contractIds: List<String> = emptyList()
    ): Client {
        return Client(
            id = entity.id,
            companyName = entity.companyName,
            notes = entity.notes,
            headquarters = addressConverter.toAddress(entity.headquartersJson),
            facilities = facilityIds,  // Facility IDs
            contacts = contactIds,     // Contacts IDs
            contracts = contractIds,   // Contract IDs
            isActive = entity.isActive,
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
        )
    }
}

/**
 * Extension functions
 */

fun ClientEntity.toDomain(addressConverter: AddressConverter): Client {
    return ClientMapper(addressConverter).toDomain(this)
}

fun Client.toEntity(addressConverter: AddressConverter): ClientEntity {
    return ClientMapper(addressConverter).toEntity(this)
}