package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Get a contact by Id
 */
class GetContactByIdUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {

    suspend operator fun invoke(contactId: String): QrResult<Contact, QrError> {
        return try {
            
            Timber.d("Get a contact by id")
            
            // Check input
            if (contactId.isBlank()) {
                Timber.w("ContactId is blank")
                return QrResult.Error(QrError.ContactsError.MissingContactId())
            }

            // Get
            when (val result = contactRepository.getContactById(contactId)) {
                is QrResult.Success -> {
                    val contact = result.data
                    if (contact != null) {
                        Timber.d("Contact found successfully: $contactId")
                        QrResult.Success(contact)
                    } else {
                        Timber.w("Contact not found: $contactId")
                        QrResult.Error(QrError.ContactsError.NotFound())
                    }
                }

                is QrResult.Error -> {
                    Timber.e("Repository error for contactId $contactId: ${result.error}")
                    QrResult.Error(result.error)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, contactId)
            QrResult.Error(QrError.SystemError.UnknownError(e))
        }
    }
}