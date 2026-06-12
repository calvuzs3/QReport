package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Restores a contact and — if its parent client is inactive — restores the
 * client as well. Both operations run inside a single repository transaction.
 *
 * No cascade to child islands or units: only the contact itself (and its
 * parent when needed) is reactivated.
 */
class RestoreContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val getContactById: GetContactByIdUseCase
) {
    suspend operator fun invoke(contactId: String): QrResult<Unit, QrError> {

        if (contactId.isBlank()) return QrResult.Error(QrError.ContactsError.NotFound())

        // Load contact
        when (val r = getContactById(contactId)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        // Restore contact — and parents if inactive
        return contactRepository.restoreContact(
            contactId = contactId,
        ).fold(
            onSuccess = {
                Timber.d("Contact $contactId restored")
                QrResult.Success(Unit)
            },
            onFailure = {
                Timber.e(it, "Failed to restore contact $contactId")
                QrResult.Error(QrError.ContactsError.DeleteError(it.message))
            }
        )
    }
}