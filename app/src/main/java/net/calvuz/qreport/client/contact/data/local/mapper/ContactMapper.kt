package net.calvuz.qreport.client.contact.data.local.mapper

import kotlinx.datetime.Instant
import net.calvuz.qreport.client.contact.data.local.entity.ContactEntity
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.model.ContactMethod
import timber.log.Timber
import javax.inject.Inject

/**
 * Maps between [ContactEntity] (data layer) and [Contact] (domain layer).
 * Handles Instant ↔ Long conversion for timestamps.
 */
class ContactMapper @Inject constructor() {

    companion object {
        private const val NULL_STRING = "null"
    }

    /** Maps a [ContactEntity] to a [Contact] domain model. */
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
            preferredContactMethod = entity.preferredContactMethod?.let { raw ->
                // Guard against "null" stored as a string — legacy DB issue
                if (raw.isBlank() || raw.equals(NULL_STRING, ignoreCase = true)) {
                    null
                } else {
                    try {
                        ContactMethod.valueOf(raw)
                    } catch (_: IllegalArgumentException) {
                        Timber.w("Unknown ContactMethod value in DB: '$raw' — falling back to null")
                        null
                    }
                }
            },
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
        )
    }

    /** Maps a [Contact] domain model to a [ContactEntity]. */
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
            // Store the enum name, never the string "null"
            preferredContactMethod = domain.preferredContactMethod?.name,
            createdAt = domain.createdAt.toEpochMilliseconds(),
            updatedAt = domain.updatedAt.toEpochMilliseconds()
        )
    }

    /** Maps a list of [ContactEntity] to a list of [Contact] domain models. */
    fun toDomainList(entities: List<ContactEntity>): List<Contact> = entities.map { toDomain(it) }

    /** Maps a list of [Contact] domain models to a list of [ContactEntity]. */
    fun toEntityList(domains: List<Contact>): List<ContactEntity> = domains.map { toEntity(it) }

    /**
     * Maps a [ContactEntity] optionally enriched with client info.
     * [clientName] is ignored for now — [Contact] domain model does not carry it.
     */
    fun toDomainWithClientInfo(entity: ContactEntity, clientName: String? = null): Contact =
        toDomain(entity)

    /** Returns active primary contacts mapped to domain models. */
    fun getPrimaryContacts(entities: List<ContactEntity>): List<Contact> =
        entities.filter { it.isPrimary && it.isActive }.map { toDomain(it) }

    /** Returns all active contacts mapped to domain models. */
    fun getActiveContacts(entities: List<ContactEntity>): List<Contact> =
        entities.filter { it.isActive }.map { toDomain(it) }
}

// =============================================================================
// Extension functions
// =============================================================================

/** Convenience extension — maps [ContactEntity] to [Contact] without an injected mapper. */
fun ContactEntity.toDomain(): Contact = ContactMapper().toDomain(this)

/** Convenience extension — maps [Contact] to [ContactEntity] without an injected mapper. */
fun Contact.toEntity(): ContactEntity = ContactMapper().toEntity(this)

/**
 * Returns the contact's full name.
 * If [Contact.lastName] is blank, only [Contact.firstName] is returned.
 */
fun Contact.fullName(): String =
    if (lastName.isNullOrBlank()) firstName else "$firstName $lastName"

/** Returns true if the contact has at least one reachable contact method. */
fun Contact.hasValidContactInfo(): Boolean =
    !phone.isNullOrBlank() || !email.isNullOrBlank()

/**
 * Returns a compact technical display string for logging and debug use only.
 * Do NOT use in the UI — build localised strings in the composable layer instead.
 */
fun Contact.toDisplayString(): String {
    val name = fullName()
    val role = role?.let { " - $it" } ?: ""
    val contact = when {
        !phone.isNullOrBlank() && !email.isNullOrBlank() -> " ($phone, $email)"
        !phone.isNullOrBlank() -> " ($phone)"
        !email.isNullOrBlank() -> " ($email)"
        else -> ""
    }
    return "$name$role$contact"
}