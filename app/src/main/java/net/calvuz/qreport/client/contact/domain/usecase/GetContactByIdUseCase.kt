package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Use Case per recuperare un contatto per ID
 *
 * Gestisce:
 * - Validazione ID input
 * - Recupero dal repository
 * - Gestione contatto non trovato
 */
class GetContactByIdUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {

    suspend operator fun invoke(contactId: String): QrResult<Contact, QrError> {
        return try {
            // 1. Validazione ID
            if (contactId.isBlank()) {
                Timber.w("GetContactByIdUseCase: contactId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(contactId.toString()))
            }

            // 2. Recupero dal repository
            when (val result = contactRepository.getContactById(contactId)) {
                is QrResult.Success -> {
                    val contact = result.data
                    if (contact != null) {
                        Timber.d("GetContactByIdUseCase: Contact found successfully: $contactId")
                        QrResult.Success(contact)
                    } else {
                        Timber.w("GetContactByIdUseCase: Contact not found: $contactId")
                        QrResult.Error(QrError.DatabaseError.NotFound(contactId))
                    }
                }

                is QrResult.Error -> {
                    Timber.e("GetContactByIdUseCase: Repository error for contactId $contactId: ${result.error}")
                    QrResult.Error(result.error)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "GetContactByIdUseCase: Exception getting contact: $contactId")
            QrResult.Error(QrError.SystemError.Unknown(e))
        }
    }
}