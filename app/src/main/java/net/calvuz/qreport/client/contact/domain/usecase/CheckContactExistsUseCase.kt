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
            // Validazione input
            if (contactId.isBlank()) {
                Timber.w("CheckContactExistsUseCase: contactId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(contactId.toString()))
            }

            Timber.d("CheckContactExistsUseCase: Checking existence of contact: $contactId")

            // Recupera contatto dal repository
            when (val result = contactRepository.getContactById(contactId)) {
                is QrResult.Success -> {
                    val contact = result.data
                    when {
                        contact == null -> {
                            Timber.w("CheckContactExistsUseCase: Contact not found: $contactId")
                            QrResult.Error(QrError.DatabaseError.NotFound(contactId))
                        }

                        !contact.isActive -> {
                            Timber.w("CheckContactExistsUseCase: Contact is not active: $contactId")
                            QrResult.Error(QrError.ValidationError.IsNotActive(contactId))
                        }

                        else -> {
                            Timber.d("CheckContactExistsUseCase: Contact found and active: $contactId")
                            QrResult.Success(contact)
                        }
                    }
                }

                is QrResult.Error -> {
                    Timber.e("CheckContactExistsUseCase: Repository error for contactId $contactId: ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "CheckContactExistsUseCase: Exception checking contact existence: $contactId")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }
}