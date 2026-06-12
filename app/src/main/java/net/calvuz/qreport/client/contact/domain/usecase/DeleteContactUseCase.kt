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
 *   will have no contacts afterward.
 *
 * Primary contact logic is handled inline — no separate handler use case needed.
 */
class DeleteContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val checkContactExists: CheckContactExistsUseCase
) {

    /**
     * @param contactId ID of the contact to delete
     * @param force     if true, ignores business rules and deletes the contact
     */
    suspend operator fun invoke(
        contactId: String,
        force: Boolean = false
    ): QrResult<Unit, QrError.ContactsError> {

        if (contactId.isBlank()) return QrResult.Error(QrError.ContactsError.NotFound())

        Timber.v("Deleting contact $contactId")

        // 1. Verify contact exists and is active
        val contact = when (val r = checkContactExists(contactId)) {
            is QrResult.Success -> r.data
            is QrResult.Error -> return QrResult.Error(QrError.ContactsError.NotFound())
        }

        // 2. Business rule: if primary, reassign before deleting

            if (contact.isPrimary) {
                when (val r = reassignPrimaryBeforeDeletion(contact)) {
                    is QrResult.Error -> {
                        if (!force) {
                            Timber.e("Failed to reassign primary {force=false}: ${r.error}")
                            return QrResult.Error(QrError.ContactsError.CannotRemovePrimaryFlag())
                        } else {
                            Timber.d("Forcing deletion of primary contact $contactId")
                        }
                    }
                    is QrResult.Success -> Unit
                }
            }


        // 3. Deactivate
        return contactRepository.deactivateContact(contactId).fold(onSuccess = {
            Timber.d("Successfully deactivated contact $contactId")
            QrResult.Success(Unit)
        }, onFailure = {
            Timber.e("Failed to deactivate contact $contactId: ${it.message}")
            QrResult.Error(QrError.ContactsError.DeleteError(it.message))
        })
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

        val candidate = contacts.filter { it.id != primary.id && it.isActive }
            .sortedWith(compareBy<Contact> { it.firstName.lowercase() }.thenBy {
                    it.lastName?.lowercase() ?: ""
                }).firstOrNull()

        if (candidate == null) {
            Timber.d("No other active contacts found")
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