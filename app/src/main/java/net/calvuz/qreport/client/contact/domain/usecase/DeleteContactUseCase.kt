package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Deletes a contact (soft delete), reassigning primary if needed.
 *
 * Business rules:
 * - If the contact is primary and other active contacts exist, the first
 *   alphabetical active contact is promoted to primary before deletion.
 * - If the contact is the only one, deletion is still allowed — the client
 *   will have no contacts afterwards.
 *
 * Primary contact logic is handled inline — no separate handler use case needed.
 */
class DeleteContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val checkContactExists: CheckContactExistsUseCase
) {
    suspend operator fun invoke(contactId: String): QrResult<Unit, QrError> {
        return try {
            if (contactId.isBlank()) return QrResult.Error(QrError.ContactsError.NotFound())

            Timber.d("Deleting contact: $contactId")

            // 1. Verify contact exists and is active
            val contact = when (val r = checkContactExists(contactId)) {
                is QrResult.Success -> r.data
                is QrResult.Error -> return QrResult.Error(r.error)
            }

            // 2. If primary, reassign before deleting
            if (contact.isPrimary) {
                when (val r = reassignPrimaryBeforeDeletion(contact)) {
                    is QrResult.Error -> return QrResult.Error(r.error)
                    is QrResult.Success -> Unit
                }
            }

            // 3. Soft delete
            when (val r = contactRepository.deleteContact(contactId)) {
                is QrResult.Success -> {
                    Timber.d("Contact deleted: $contactId")
                    QrResult.Success(Unit)
                }
                is QrResult.Error -> {
                    Timber.e("Failed to delete contact $contactId: ${r.error}")
                    QrResult.Error(r.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, contactId)
            QrResult.Error(QrError.SystemError.UnknownError())
        }
    }

    /**
     * Finds the next active contact alphabetically and calls [ContactRepository.setPrimaryContact].
     * The DAO transaction updates [is_primary] and [updated_at] atomically on both rows.
     * If no other active contacts exist, returns [QrResult.Success] — deletion is allowed
     * and the client will temporarily have no primary.
     */
    private suspend fun reassignPrimaryBeforeDeletion(primary: Contact): QrResult<Unit, QrError> {
        val contacts = when (val r = contactRepository.getContactsByClient(primary.clientId)) {
            is QrResult.Success -> r.data
            is QrResult.Error -> return QrResult.Error(r.error)
        }

        val candidate = contacts
            .filter { it.id != primary.id && it.isActive }
            .sortedWith(
                compareBy<Contact> { it.firstName.lowercase() }
                    .thenBy { it.lastName?.lowercase() ?: "" }
            )
            .firstOrNull()

        if (candidate == null) {
            Timber.d("No other active contacts — primary left unassigned after deletion: ${primary.id}")
            return QrResult.Success(Unit)
        }

        return when (val r = contactRepository.setPrimaryContact(primary.clientId, candidate.id)) {
            is QrResult.Success -> {
                Timber.d("Primary reassigned to ${candidate.id} before deleting ${primary.id}")
                QrResult.Success(Unit)
            }
            is QrResult.Error -> {
                Timber.e("Failed to reassign primary to ${candidate.id}: ${r.error}")
                QrResult.Error(r.error)
            }
        }
    }
}