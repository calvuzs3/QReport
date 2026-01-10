package net.calvuz.qreport.client.contract.data.local

import kotlinx.datetime.Clock
import net.calvuz.qreport.client.contract.domain.model.Contract
import kotlinx.datetime.Instant
import javax.inject.Inject

class ContractMapper @Inject constructor() {

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

    fun toDomainList(entities: List<ContractEntity>): List<Contract> {
        return entities.map { toDomain(it) }
    }

    fun toEntityList(domains: List<Contract>): List<ContractEntity> {
        return domains.map { toEntity(it) }
    }

    /**
     * Converte ContractEntity with customer info
     * Complex query utility with customer name
     * Contract domain does not include clientName
     */
    fun toDomainWithClientInfo(
        entity: ContractEntity,
        clientName: String? = null
    ): Contract {
        return toDomain(entity)
    }

}

fun ContractEntity.toDomain(): Contract {
    return ContractMapper().toDomain(this)
}

fun Contract.toEntity(): ContractEntity {
    return ContractMapper().toEntity(this)
}

fun Contract.isValid(): Boolean {
    val now = Clock.System.now()
    return (startDate <= now) && (endDate >= now)
}

fun Contract.toDisplayString(): String {

    return "$name (${isValid()})"
}