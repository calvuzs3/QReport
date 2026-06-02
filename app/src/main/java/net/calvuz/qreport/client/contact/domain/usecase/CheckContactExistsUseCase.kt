package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

class CheckContactExistsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
){

    suspend operator fun invoke(contactId: String): QrResult<Contact, QrError> {
        return try {

            // Check input
            if (contactId.isBlank()) {
                Timber.w("ContactId is blank")
                return QrResult.Error(QrError.ContactsError.MissingClientId())
            }

            // Get contact
            when (val result = contactRepository.getContactById(contactId)) {
                is QrResult.Success -> {
                    val contact = result.data
                    when {
                        contact == null -> {
                            Timber.w("Contact not found: $contactId")
                            QrResult.Error(QrError.ContactsError.NotFound())
                        }

                        !contact.isActive -> {
                            Timber.w("Contact is not active: $contactId")
                            QrResult.Error(QrError.ContactsError.ContactIsNotActive())
                        }

                        else -> {
                            Timber.d("Contact found and active: $contactId")
                            QrResult.Success(contact)
                        }
                    }
                }

                is QrResult.Error -> {
                    Timber.e("Repository error for contactId $contactId: ${result.error}")
                    QrResult.Error(QrError.ContactsError.LoadError())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception checking contact existence: $contactId")
            QrResult.Error(QrError.SystemError.UnknownError())
        }
    }
}