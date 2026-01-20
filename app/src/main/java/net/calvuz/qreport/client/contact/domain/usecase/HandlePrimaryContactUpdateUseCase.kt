package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Use Case per gestire la logica di aggiornamento del primary contact
 *
 * Updated to use QrResult<Contact, QrError> pattern
 */
class HandlePrimaryContactUpdateUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {

    /**
     * Gestisce la logica di aggiornamento del primary contact
     *
     * @param originalContact Contatto originale prima dell'aggiornamento
     * @param updatedContact Contatto con dati aggiornati
     * @return QrResult.Success con Contact finale, QrResult.Error per errori
     */
    suspend operator fun invoke(
        originalContact: Contact,
        updatedContact: Contact
    ): QrResult<Contact, QrError> {
        return try {
            // Validazione input
            if (originalContact.id != updatedContact.id) {
                Timber.w("HandlePrimaryContactUpdateUseCase: Contact IDs don't match: ${originalContact.id} vs ${updatedContact.id}")
                return QrResult.Error(QrError.ValidationError.EmptyField(originalContact.id.toString()))
            }

            if (originalContact.clientId.isBlank() || updatedContact.clientId.isBlank()) {
                Timber.w("HandlePrimaryContactUpdateUseCase: clientId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(originalContact.clientId.toString()))
            }

            if (originalContact.clientId != updatedContact.clientId) {
                Timber.w("HandlePrimaryContactUpdateUseCase: Cannot change client association: ${originalContact.clientId} to ${updatedContact.clientId}")
                return QrResult.Error(QrError.ValidationError.InvalidOperation(QrError.Contacts.ValidationError.CannotChangeClientAssociation ) )
            }

            Timber.d("HandlePrimaryContactUpdateUseCase: Processing primary update for contact: ${updatedContact.id}")

            // Se non c'è cambio nel flag primary, mantieni tutto uguale
            if (originalContact.isPrimary == updatedContact.isPrimary) {
                Timber.d("HandlePrimaryContactUpdateUseCase: No change in primary status, returning updated contact: ${updatedContact.id}")
                return QrResult.Success(updatedContact)
            }

            when {
                // Caso 1: Sta diventando primary
                updatedContact.isPrimary && !originalContact.isPrimary -> {
                    Timber.d("HandlePrimaryContactUpdateUseCase: Contact becoming primary: ${updatedContact.id}")
                    // Il repository gestirà automaticamente la rimozione del primary precedente
                    QrResult.Success(updatedContact)
                }

                // Caso 2: Sta smettendo di essere primary
                !updatedContact.isPrimary && originalContact.isPrimary -> {
                    Timber.d("HandlePrimaryContactUpdateUseCase: Contact losing primary status: ${updatedContact.id}")

                    // Verifica che non sia l'unico contatto del cliente
                    when (val contactsResult = contactRepository.getContactsByClient(originalContact.clientId)) {
                        is QrResult.Success -> {
                            val clientContacts = contactsResult.data

                            if (clientContacts.size == 1) {
                                Timber.w("HandlePrimaryContactUpdateUseCase: Cannot remove primary flag - is the only contact: ${updatedContact.id}")
                                return QrResult.Error(QrError.ValidationError.InvalidOperation(
                                    QrError.Contacts.ValidationError.CannotRemovePrimaryFlag))
                            }

                            Timber.d("HandlePrimaryContactUpdateUseCase: Primary status removed successfully: ${updatedContact.id}")
                            QrResult.Success(updatedContact)
                        }

                        is QrResult.Error -> {
                            Timber.e("HandlePrimaryContactUpdateUseCase: Error getting client contacts for ${originalContact.clientId}: ${contactsResult.error}")
                            QrResult.Error(contactsResult.error)
                        }
                    }
                }

                else -> {
                    // Shouldn't reach here, but return updated contact
                    Timber.d("HandlePrimaryContactUpdateUseCase: No primary logic changes needed: ${updatedContact.id}")
                    QrResult.Success(updatedContact)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "HandlePrimaryContactUpdateUseCase: Exception handling primary update for contact: ${updatedContact.id}")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }
}