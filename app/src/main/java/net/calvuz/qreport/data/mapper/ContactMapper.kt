package net.calvuz.qreport.data.mapper

import net.calvuz.qreport.data.local.entity.ContactEntity
import net.calvuz.qreport.domain.model.client.Contact
import kotlinx.datetime.Instant
import net.calvuz.qreport.domain.model.client.ContactMethod
import javax.inject.Inject

/**
 * Mapper per convertire tra ContactEntity (data layer) e Contact (domain layer)
 * Gestisce la conversione delle date Instant ↔ Long
 */
class ContactMapper @Inject constructor() {

    /**
     * Converte da ContactEntity a Contact domain model
     */
    fun toDomain(entity: ContactEntity): Contact {
        return Contact(
            id = entity.id,
            clientId = entity.clientId,
            firstName = entity.firstName,
            lastName = entity.lastName,
            role = entity.role,
            department = entity.department,
            phone = entity.phone,
            mobilePhone = entity.mobilePhone,
            email = entity.email,
            alternativeEmail = entity.alternativeEmail,
            isPrimary = entity.isPrimary,
            isActive = entity.isActive,
            preferredContactMethod = entity.preferredContactMethod?.let {
                ContactMethod.valueOf(it)
            },
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
        )
    }

    /**
     * Converte da Contact domain model a ContactEntity
     */
    fun toEntity(domain: Contact): ContactEntity {
        return ContactEntity(
            id = domain.id,
            clientId = domain.clientId,
            firstName = domain.firstName,
            lastName = domain.lastName,
            role = domain.role,
            department = domain.department,
            phone = domain.phone,
            mobilePhone = domain.mobilePhone,
            email = domain.email,
            alternativeEmail = domain.alternativeEmail,
            isPrimary = domain.isPrimary,
            isActive = domain.isActive,
            preferredContactMethod = domain.preferredContactMethod.toString(),
            createdAt = domain.createdAt.toEpochMilliseconds(),
            updatedAt = domain.updatedAt.toEpochMilliseconds()
        )
    }

    /**
     * Converte lista di ContactEntity a lista di Contact domain models
     */
    fun toDomainList(entities: List<ContactEntity>): List<Contact> {
        return entities.map { toDomain(it) }
    }

    /**
     * Converte lista di Contact domain models a lista di ContactEntity
     */
    fun toEntityList(domains: List<Contact>): List<ContactEntity> {
        return domains.map { toEntity(it) }
    }

    /**
     * Converte ContactEntity con informazioni sul client
     * Utility per query complesse che includono client name
     */
    fun toDomainWithClientInfo(
        entity: ContactEntity,
        clientName: String? = null
    ): Contact {
        // Al momento Contact domain non include clientName
        // Ma potremmo estendere in futuro
        return toDomain(entity)
    }

    /**
     * Filtra contacts per ruolo primario
     */
    fun getPrimaryContacts(entities: List<ContactEntity>): List<Contact> {
        return entities
            .filter { it.isPrimary && it.isActive }
            .map { toDomain(it) }
    }

    /**
     * Filtra contacts attivi
     */
    fun getActiveContacts(entities: List<ContactEntity>): List<Contact> {
        return entities
            .filter { it.isActive }
            .map { toDomain(it) }
    }
}

/**
 * Extension functions per conversioni dirette
 */

/**
 * Converte ContactEntity a Contact
 */
fun ContactEntity.toDomain(): Contact {
    return ContactMapper().toDomain(this)
}

/**
 * Converte Contact a ContactEntity
 */
fun Contact.toEntity(): ContactEntity {
    return ContactMapper().toEntity(this)
}

/**
 * Extension per nome completo contact
 */
fun Contact.fullName(): String {
    return if (lastName.isNullOrBlank()) {
        firstName
    } else {
        "$firstName $lastName"
    }
}

/**
 * Extension per verifica se ha informazioni di contatto valide
 */
fun Contact.hasValidContactInfo(): Boolean {
    return !phone.isNullOrBlank() || !email.isNullOrBlank()
}

/**
 * Extension per formattazione display
 */
fun Contact.toDisplayString(): String {
    val name = fullName()
    val roleInfo = role?.let { " - $it" } ?: ""
    val contactInfo = when {
        !phone.isNullOrBlank() && !email.isNullOrBlank() -> " ($phone, $email)"
        !phone.isNullOrBlank() -> " ($phone)"
        !email.isNullOrBlank() -> " ($email)"
        else -> ""
    }
    return "$name$roleInfo$contactInfo"
}