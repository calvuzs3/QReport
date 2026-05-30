package net.calvuz.qreport.client.client.data.local.mapper

import kotlinx.datetime.Instant
import net.calvuz.qreport.app.app.data.converter.AddressConverter
import net.calvuz.qreport.client.client.data.local.entity.ClientEntity
import net.calvuz.qreport.client.client.domain.model.Client
import javax.inject.Inject

/**
 * Maps between [ClientEntity] (data layer) and [Client] (domain layer).
 *
 * Address JSON serialization is delegated to [AddressConverter], consistent
 * with the rest of the data layer.
 *
 * [ClientEntity.isDeleted] is a data-layer concern: the repository filters
 * deleted rows before calling this mapper, so [Client] never sees them.
 */
class ClientMapper @Inject constructor(
    private val addressConverter: AddressConverter
) {

    fun toDomain(entity: ClientEntity): Client = Client(
        id = entity.id,
        companyName = entity.companyName,
        notes = entity.notes,
        headquarters = addressConverter.toAddress(entity.headquartersJson),
        isActive = entity.isActive,
        createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
        updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
    )

    /**
     * [isDeleted] is always false here; soft-delete is managed by the repository,
     * never written directly from a domain model.
     */
    fun toEntity(domain: Client): ClientEntity = ClientEntity(
        id = domain.id,
        companyName = domain.companyName,
        notes = domain.notes,
        headquartersJson = addressConverter.fromAddress(domain.headquarters),
        isActive = domain.isActive,
        createdAt = domain.createdAt.toEpochMilliseconds(),
        updatedAt = domain.updatedAt.toEpochMilliseconds()
    )

    fun toDomainList(entities: List<ClientEntity>): List<Client> =
        entities.map { toDomain(it) }

    fun toEntityList(domains: List<Client>): List<ClientEntity> =
        domains.map { toEntity(it) }
}

// ─── Extension functions ──────────────────────────────────────────────────────

fun ClientEntity.toDomain(addressConverter: AddressConverter): Client =
    ClientMapper(addressConverter).toDomain(this)

fun Client.toEntity(addressConverter: AddressConverter): ClientEntity =
    ClientMapper(addressConverter).toEntity(this)