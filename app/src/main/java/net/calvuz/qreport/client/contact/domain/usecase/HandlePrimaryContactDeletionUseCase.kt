package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Use Case per gestire l'eliminazione di un contatto primary
 *
 * Updated to use QrResult<Unit, QrError> pattern
 */
class HandlePrimaryContactDeletionUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {

    /**
     * Gestisce l'eliminazione di un contatto primary
     * Se ci sono altri contatti attivi, assegna il primary al primo contatto disponibile
     *
     * @param primaryContact Contatto primary da eliminare
     * @return QrResult.Success se gestione completata, QrResult.Error per errori
     */
    suspend operator fun invoke(primaryContact: Contact): QrResult<Unit, QrError> {
        return try {
            // Validazione input
            if (!primaryContact.isPrimary) {
                Timber.w("HandlePrimaryContactDeletionUseCase: Contact is not primary: ${primaryContact.id}")
                return QrResult.Error(QrError.ValidationError.EmptyField(primaryContact.id.toString()))
            }

            if (primaryContact.clientId.isBlank()) {
                Timber.w("HandlePrimaryContactDeletionUseCase: clientId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(primaryContact.clientId.toString()))
            }

            Timber.d("HandlePrimaryContactDeletionUseCase: Handling primary contact deletion: ${primaryContact.id} for client: ${primaryContact.clientId}")

            // Ottieni tutti gli altri contatti attivi del cliente
            when (val contactsResult = contactRepository.getContactsByClient(primaryContact.clientId)) {
                is QrResult.Success -> {
                    val clientContacts = contactsResult.data
                    val otherActiveContacts = clientContacts.filter {
                        it.id != primaryContact.id && it.isActive
                    }

                    // Se non ci sono altri contatti, ok eliminare (il cliente rimane senza contatti)
                    if (otherActiveContacts.isEmpty()) {
                        Timber.d("HandlePrimaryContactDeletionUseCase: No other active contacts, deletion allowed for: ${primaryContact.id}")
                        return QrResult.Success(Unit)
                    }

                    // Altrimenti, assegna primary al primo contatto nell'ordinamento
                    val newPrimaryContact = otherActiveContacts.sortedWith(
                        compareBy<Contact> { it.firstName.lowercase() }
                            .thenBy { it.lastName?.lowercase() ?: "" }
                    ).first()

                    Timber.d("HandlePrimaryContactDeletionUseCase: Assigning primary to new contact: ${newPrimaryContact.id}")

                    // Imposta il nuovo primary contact
                    when (val setPrimaryResult = contactRepository.setPrimaryContact(primaryContact.clientId, newPrimaryContact.id)) {
                        is QrResult.Success -> {
                            Timber.d("HandlePrimaryContactDeletionUseCase: Successfully assigned new primary contact: ${newPrimaryContact.id}")
                            QrResult.Success(Unit)
                        }

                        is QrResult.Error -> {
                            Timber.e("HandlePrimaryContactDeletionUseCase: Failed to set new primary contact ${newPrimaryContact.id}: ${setPrimaryResult.error}")
                            QrResult.Error(setPrimaryResult.error)
                        }
                    }
                }

                is QrResult.Error -> {
                    Timber.e("HandlePrimaryContactDeletionUseCase: Error getting client contacts for ${primaryContact.clientId}: ${contactsResult.error}")
                    QrResult.Error(contactsResult.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "HandlePrimaryContactDeletionUseCase: Exception handling primary contact deletion: ${primaryContact.id}")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }
}