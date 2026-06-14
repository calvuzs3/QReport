package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Manages explicit primary contact changes triggered by the user from the UI
 * (e.g. tapping "Set as primary" in the contact list).
 *
 * All mutations go through [ContactRepository.setPrimaryContact], which delegates
 * to the DAO transaction that atomically:
 *   1. Clears [isPrimary] and bumps [updatedAt] on the previous primary.
 *   2. Sets [isPrimary] and bumps [updatedAt] on the new primary.
 *
 * This is the single authoritative entry point for explicit primary changes.
 * CRUD use cases (Create/Update/Delete) contain their own inline logic for
 * implicit primary changes that occur as a side effect of those operations.
 */
class SetPrimaryContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val checkContactExists: CheckContactExistsUseCase
) {

    /**
     * Sets a contact as primary for its client.
     * Looks up the clientId from the contact record — use [setPrimaryContactForClient]
     * when the clientId is already known to avoid the extra lookup.
     */
    suspend operator fun invoke(contactId: String): QrResult<Unit, QrError> {
        if (contactId.isBlank()) return QrResult.Error(QrError.ContactsError.NotFound())

        val contact = when (val r = checkContactExists(contactId)) {
            is QrResult.Success -> r.data
            is QrResult.Error -> return QrResult.Error(r.error)
        }

        if (contact.isPrimary) {
            Timber.d("Contact is already primary: $contactId")
            return QrResult.Success(Unit)
        }

        return setPrimary(contact.clientId, contactId)
    }

    /**
     * Sets a contact as primary for a specific client.
     * Validates that the contact belongs to [clientId].
     */
    suspend fun setPrimaryContactForClient(
        clientId: String,
        contactId: String
    ): QrResult<Unit, QrError> {
        if (clientId.isBlank()) return QrResult.Error(QrError.ContactsError.MissingClientId())
        if (contactId.isBlank()) return QrResult.Error(QrError.ContactsError.NotFound())

        val contact = when (val r = checkContactExists(contactId)) {
            is QrResult.Success -> r.data
            is QrResult.Error -> return QrResult.Error(r.error)
        }

        if (contact.clientId != clientId) {
            Timber.w("Contact $contactId does not belong to client $clientId")
            return QrResult.Error(QrError.ValidationError.InvalidOperation(
                QrError.ContactsError.ContactDoesNotBelongToClient()))
        }

        if (contact.isPrimary) {
            Timber.d("Contact is already primary: $contactId")
            return QrResult.Success(Unit)
        }

        return setPrimary(clientId, contactId)
    }

    /**
     * Removes the primary flag from a contact and promotes the next alphabetical
     * active contact.
     *
     * Fails if there are no other active contacts — a client must always have
     * a primary contact as long as it has at least one active contact.
     */
    suspend fun removePrimaryStatus(contactId: String): QrResult<Unit, QrError> {
        if (contactId.isBlank()) return QrResult.Error(QrError.ContactsError.NotFound())

        val contact = when (val r = checkContactExists(contactId)) {
            is QrResult.Success -> r.data
            is QrResult.Error -> return QrResult.Error(r.error)
        }

        if (!contact.isPrimary) {
            Timber.w("Contact is not primary: $contactId")
            return QrResult.Error(QrError.ValidationError.InvalidOperation(
                QrError.ValidationError.IsNotPrimary()))
        }

        val clientContacts = when (val r = contactRepository.getContactsByClient(contact.clientId)) {
            is QrResult.Success -> r.data
            is QrResult.Error -> return QrResult.Error(r.error)
        }

        // Find next candidate alphabetically
        val candidate = clientContacts
            .filter { it.id != contact.id && it.isActive }
            .sortedWith(
                compareBy<Contact> { it.firstName.lowercase() }
                    .thenBy { it.lastName?.lowercase() ?: "" }
            )
            .firstOrNull()
            ?: run {
                Timber.w("No other active contacts — cannot remove primary flag: $contactId")
                return QrResult.Error(QrError.ValidationError.InvalidOperation(
                    QrError.ContactsError.CannotRemovePrimaryFlag()))
            }

        // Promote candidate — the DAO transaction clears the old flag and sets the new one
        return setPrimary(contact.clientId, candidate.id)
    }

    /** Returns the current primary contact for a client, or null if none. */
    suspend fun getCurrentPrimaryContact(clientId: String): QrResult<Contact?, QrError> {
        if (clientId.isBlank()) return QrResult.Error(QrError.ContactsError.MissingClientId())
        return try {
            contactRepository.getPrimaryContact(clientId)
        } catch (e: Exception) {
            Timber.e(e, clientId)
            QrResult.Error(QrError.SystemError.UnknownError())
        }
    }

    /** Returns true if the client has a primary contact. */
    suspend fun hasPrimaryContact(clientId: String): QrResult<Boolean, QrError> {
        if (clientId.isBlank()) return QrResult.Error(QrError.ContactsError.MissingClientId())
        return try {
            contactRepository.hasPrimaryContact(clientId)
        } catch (e: Exception) {
            Timber.e(e, clientId)
            QrResult.Error(QrError.SystemError.UnknownError())
        }
    }

    /**
     * Suggests the best primary candidate using a scoring heuristic:
     * email presence (3 pts) > phone (1 pt) > mobile (1 pt), then alphabetical.
     */
    suspend fun suggestBestPrimaryCandidate(clientId: String): QrResult<Contact?, QrError> {
        if (clientId.isBlank()) return QrResult.Error(QrError.ContactsError.MissingClientId())
        return try {
            val contacts = when (val r = contactRepository.getContactsByClient(clientId)) {
                is QrResult.Success -> r.data
                is QrResult.Error -> return QrResult.Error(r.error)
            }

            val candidate = contacts
                .filter { it.isActive && !it.isPrimary }
                .sortedWith(
                    compareByDescending<Contact> { c ->
                        var score = 0
                        if (c.email?.isNotBlank() == true) score += 3
                        if (c.phone?.isNotBlank() == true) score += 1
                        if (c.mobilePhone?.isNotBlank() == true) score += 1
                        score
                    }.thenBy { it.firstName.lowercase() }
                        .thenBy { it.lastName?.lowercase() ?: "" }
                )
                .firstOrNull()

            Timber.d("Best primary candidate for client $clientId: ${candidate?.id ?: "none"}")
            QrResult.Success(candidate)
        } catch (e: Exception) {
            Timber.e(e, clientId)
            QrResult.Error(QrError.SystemError.UnknownError())
        }
    }

    /**
     * Assigns a primary contact automatically if the client has none.
     * Uses [suggestBestPrimaryCandidate] to select the candidate.
     * Returns the promoted contact, or null if no contacts exist.
     */
    suspend fun autoAssignPrimaryIfMissing(clientId: String): QrResult<Contact?, QrError> {
        if (clientId.isBlank()) return QrResult.Error(QrError.ContactsError.MissingClientId())
        return try {
            val hasPrimary = when (val r = contactRepository.hasPrimaryContact(clientId)) {
                is QrResult.Success -> r.data
                is QrResult.Error -> return QrResult.Error(r.error)
            }

            if (hasPrimary) {
                Timber.d("Client $clientId already has primary — returning existing")
                return contactRepository.getPrimaryContact(clientId)
            }

            val candidate = when (val r = suggestBestPrimaryCandidate(clientId)) {
                is QrResult.Success -> r.data ?: run {
                    Timber.d("No candidate for auto-assignment for client $clientId")
                    return QrResult.Success(null)
                }
                is QrResult.Error -> return QrResult.Error(r.error)
            }

            when (val r = setPrimary(clientId, candidate.id)) {
                is QrResult.Error -> return QrResult.Error(r.error)
                is QrResult.Success -> Unit
            }

            // Return the updated contact
            when (val r = contactRepository.getContactById(candidate.id)) {
                is QrResult.Success -> {
                    Timber.d("Auto-assigned primary: ${candidate.id} for client $clientId")
                    QrResult.Success(r.data)
                }
                is QrResult.Error -> QrResult.Error(r.error)
            }
        } catch (e: Exception) {
            Timber.e(e, clientId)
            QrResult.Error(QrError.SystemError.UnknownError())
        }
    }

    // =========================================================================
    // PRIVATE — single call site for all primary assignments
    // =========================================================================

    /**
     * Single internal method that calls [ContactRepository.setPrimaryContact].
     * All public methods funnel through here so the DAO transaction is
     * always invoked consistently.
     */
    private suspend fun setPrimary(clientId: String, contactId: String): QrResult<Unit, QrError> {
        return when (val r = contactRepository.setPrimaryContact(clientId, contactId)) {
            is QrResult.Success -> {
                Timber.d("Primary set: $contactId for client $clientId")
                QrResult.Success(Unit)
            }
            is QrResult.Error -> {
                Timber.e("Failed to set primary $contactId for client $clientId: ${r.error}")
                QrResult.Error(r.error)
            }
        }
    }
}