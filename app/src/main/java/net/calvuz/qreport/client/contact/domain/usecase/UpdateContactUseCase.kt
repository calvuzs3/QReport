package net.calvuz.qreport.client.contact.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import net.calvuz.qreport.client.contact.domain.validator.ContactDataValidator
import timber.log.Timber
import javax.inject.Inject

/**
 * Updates an existing contact, handling primary contact changes inline.
 *
 * Business rules:
 * - clientId must not change.
 * - Email/phone uniqueness is checked only if the value changed.
 * - Becoming primary: [ContactRepository.setPrimaryContact] is called so the DAO
 *   transaction atomically clears the previous primary and sets [updated_at] on it.
 * - Losing primary: the next alphabetical active contact is promoted via
 *   [ContactRepository.setPrimaryContact] before persisting the update.
 *   If no active contacts remain, the operation is rejected.
 *
 * Primary contact logic is handled inline — no separate handler use case needed.
 */
class UpdateContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val checkContactExists: CheckContactExistsUseCase,
    private val checkEmailUniqueness: CheckEmailUniquenessUseCase,
    private val checkPhoneUniqueness: CheckPhoneUniquenessUseCase,
    private val validateContactData: ContactDataValidator
) {
    suspend operator fun invoke(contact: Contact): QrResult<Contact, QrError> {
        return try {
            Timber.d("Updating contact: ${contact.id}")

            // 1. Verify contact exists
            val original = when (val r = checkContactExists(contact.id)) {
                is QrResult.Success -> r.data
                is QrResult.Error -> return QrResult.Error(r.error)
            }

            // 2. clientId must not change
            if (original.clientId != contact.clientId) {
                Timber.w("Cannot change clientId: ${original.clientId} → ${contact.clientId}")
                return QrResult.Error(QrError.ValidationError.InvalidOperation(
                    QrError.ContactsError.CannotChangeClientAssociation()))
            }

            // 3. Validate updated fields
            when (val v = validateContactData(contact)) {
                is QrResult.Error -> return QrResult.Error(v.error)
                is QrResult.Success -> Unit
            }

            // 4. Email uniqueness (only if changed)
            contact.email?.takeIf { it.isNotBlank() && it != original.email }?.let { email ->
                when (val e = checkEmailUniqueness(email, contact.id)) {
                    is QrResult.Error -> return QrResult.Error(e.error)
                    is QrResult.Success -> Unit
                }
            }

            // 5. Phone uniqueness (only if changed)
            contact.phone?.takeIf { it.isNotBlank() && it != original.phone }?.let { phone ->
                when (val p = checkPhoneUniqueness(phone, contact.id)) {
                    is QrResult.Error -> return QrResult.Error(p.error)
                    is QrResult.Success -> Unit
                }
            }

            // 6. Mobile uniqueness (only if changed)
            contact.mobilePhone?.takeIf { it.isNotBlank() && it != original.mobilePhone }?.let { mobile ->
                when (val m = checkPhoneUniqueness(mobile, contact.id)) {
                    is QrResult.Error -> return QrResult.Error(m.error)
                    is QrResult.Success -> Unit
                }
            }

            // 7. Primary contact logic (inline)
            val primaryChanged = original.isPrimary != contact.isPrimary
            if (primaryChanged) {
                when (val r = handlePrimaryChange(original, contact)) {
                    is QrResult.Error -> return QrResult.Error(r.error)
                    is QrResult.Success -> Unit
                }
            }

            // 8. Persist with refreshed updatedAt
            val toSave = contact.copy(updatedAt = Clock.System.now())
            when (val r = contactRepository.updateContact(toSave)) {
                is QrResult.Success -> {
                    Timber.d("Contact updated: ${r.data.id}")
                    QrResult.Success(r.data)
                }
                is QrResult.Error -> {
                    Timber.e("Failed to update contact ${contact.id}: ${r.error}")
                    QrResult.Error(r.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, contact.id)
            QrResult.Error(QrError.SystemError.UnknownError())
        }
    }

    /**
     * Handles the two primary change scenarios.
     *
     * Becoming primary: calls [ContactRepository.setPrimaryContact] so the DAO
     * transaction atomically clears the old primary's flag and updates its [updated_at].
     *
     * Losing primary: finds the next alphabetical active candidate, calls
     * [ContactRepository.setPrimaryContact] to promote it. Rejects if no
     * active candidate exists.
     */
    private suspend fun handlePrimaryChange(
        original: Contact,
        updated: Contact
    ): QrResult<Unit, QrError> {

        return when {
            // Case 1: becoming primary
            updated.isPrimary && !original.isPrimary -> {
                Timber.d("Contact becoming primary: ${updated.id}")
                when (val r = contactRepository.setPrimaryContact(original.clientId, updated.id)) {
                    is QrResult.Success -> {
                        Timber.d("Primary set: ${updated.id}")
                        QrResult.Success(Unit)
                    }
                    is QrResult.Error -> {
                        Timber.e("Failed to set primary ${updated.id}: ${r.error}")
                        QrResult.Error(r.error)
                    }
                }
            }

            // Case 2: losing primary — must promote another contact first
            !updated.isPrimary && original.isPrimary -> {
                Timber.d("Contact losing primary: ${updated.id}")

                val contacts = when (val r = contactRepository.getContactsByClient(original.clientId)) {
                    is QrResult.Success -> r.data
                    is QrResult.Error -> return QrResult.Error(r.error)
                }

                val candidate = contacts
                    .filter { it.id != updated.id && it.isActive }
                    .sortedWith(
                        compareBy<Contact> { it.firstName.lowercase() }
                            .thenBy { it.lastName?.lowercase() ?: "" }
                    )
                    .firstOrNull()
                    ?: run {
                        Timber.w("No active candidate to become primary — cannot remove flag: ${updated.id}")
                        return QrResult.Error(QrError.ValidationError.InvalidOperation(
                            QrError.ContactsError.CannotRemovePrimaryFlag()))
                    }

                when (val r = contactRepository.setPrimaryContact(original.clientId, candidate.id)) {
                    is QrResult.Success -> {
                        Timber.d("Primary reassigned to ${candidate.id}, ${updated.id} loses flag")
                        QrResult.Success(Unit)
                    }
                    is QrResult.Error -> {
                        Timber.e("Failed to reassign primary to ${candidate.id}: ${r.error}")
                        QrResult.Error(r.error)
                    }
                }
            }

            else -> QrResult.Success(Unit)
        }
    }
}