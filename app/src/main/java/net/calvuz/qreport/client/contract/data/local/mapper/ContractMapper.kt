package net.calvuz.qreport.client.contract.data.local.mapper

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.client.contract.data.local.entity.ContractEntity
import net.calvuz.qreport.client.contract.domain.model.Contract
import javax.inject.Inject

/**
 * Maps between [ContractEntity] (data layer) and [Contract] (domain layer).
 * Handles Instant ↔ Long conversion for timestamps.
 */
class ContractMapper @Inject constructor() {

    /** Maps a [ContractEntity] to a [Contract] domain model. */
    fun toDomain(entity: ContractEntity): Contract {
        return Contract(
            id = entity.id,
            clientId = entity.clientId,
            name = entity.name,
            description = entity.description,
            startDate = Instant.fromEpochMilliseconds(entity.startDate),
            endDate = Instant.fromEpochMilliseconds(entity.endDate),
            hasPriority = entity.hasPriority,
            hasRemoteAssistance = entity.hasRemoteAssistance,
            hasMaintenance = entity.hasMaintenance,
            notes = entity.notes,
            isActive = entity.isActive,
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
        )
    }

    /** Maps a [Contract] domain model to a [ContractEntity]. */
    fun toEntity(domain: Contract): ContractEntity {
        return ContractEntity(
            id = domain.id,
            clientId = domain.clientId,
            name = domain.name,
            description = domain.description,
            startDate = domain.startDate.toEpochMilliseconds(),
            endDate = domain.endDate.toEpochMilliseconds(),
            hasPriority = domain.hasPriority,
            hasRemoteAssistance = domain.hasRemoteAssistance,
            hasMaintenance = domain.hasMaintenance,
            notes = domain.notes,
            isActive = domain.isActive,
            createdAt = domain.createdAt.toEpochMilliseconds(),
            updatedAt = domain.updatedAt.toEpochMilliseconds()
        )
    }

    /** Maps a list of [ContractEntity] to a list of [Contract] domain models. */
    fun toDomainList(entities: List<ContractEntity>): List<Contract> = entities.map { toDomain(it) }

    /** Maps a list of [Contract] domain models to a list of [ContractEntity]. */
    fun toEntityList(domains: List<Contract>): List<ContractEntity> = domains.map { toEntity(it) }

    /**
     * Maps a [ContractEntity] optionally enriched with client info.
     * [clientName] is ignored for now — [Contract] domain model does not carry it.
     */
    fun toDomainWithClientInfo(entity: ContractEntity, clientName: String? = null): Contract =
        toDomain(entity)
}

// =============================================================================
// Extension functions
// =============================================================================

/** Convenience extension — maps [ContractEntity] to [Contract] without an injected mapper. */
fun ContractEntity.toDomain(): Contract = ContractMapper().toDomain(this)

/** Convenience extension — maps [Contract] to [ContractEntity] without an injected mapper. */
fun Contract.toEntity(): ContractEntity = ContractMapper().toEntity(this)

/**
 * Returns true if the contract is currently active by date —
 * i.e. [startDate] is in the past and [endDate] is in the future.
 */
fun Contract.isValid(): Boolean {
    val now = Clock.System.now()
    return startDate <= now && endDate >= now
}