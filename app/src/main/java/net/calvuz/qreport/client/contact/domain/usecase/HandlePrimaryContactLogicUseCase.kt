package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Use Case per gestione automatica primary contact durante creazione
 *
 * Updated to use QrResult<Contact, QrError> pattern
 */
class HandlePrimaryContactLogicUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {

    /**
     * Gestisce la logica automatica del primary contact
     * Se il cliente non ha un contatto primary, questo diventa automaticamente primary
     *
     * @param contact Contatto da processare per la logica primary
     * @return QrResult.Success con Contact (possibilmente aggiornato), QrResult.Error per errori
     */
    suspend operator fun invoke(contact: Contact): QrResult<Contact, QrError> {
        return try {
            // Validazione input
            if (contact.clientId.isBlank()) {
                Timber.w("HandlePrimaryContactLogicUseCase: clientId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(contact.clientId.toString()))
            }

            Timber.d("HandlePrimaryContactLogicUseCase: Processing primary logic for contact: ${contact.id}, client: ${contact.clientId}")

            // Se è già impostato come primary, mantieni
            if (contact.isPrimary) {
                Timber.d("HandlePrimaryContactLogicUseCase: Contact is already primary, no changes needed: ${contact.id}")
                return QrResult.Success(contact)
            }

            // Controlla se il cliente ha già un contatto primary
            when (val hasPrimaryResult = contactRepository.hasPrimaryContact(contact.clientId)) {
                is QrResult.Success -> {
                    val hasPrimary = hasPrimaryResult.data

                    // Se non ha primary, questo diventa primary automaticamente
                    val finalContact = if (!hasPrimary) {
                        Timber.d("HandlePrimaryContactLogicUseCase: Client has no primary contact, setting this as primary: ${contact.id}")
                        contact.copy(isPrimary = true)
                    } else {
                        Timber.d("HandlePrimaryContactLogicUseCase: Client already has primary contact, keeping as non-primary: ${contact.id}")
                        contact
                    }

                    QrResult.Success(finalContact)
                }

                is QrResult.Error -> {
                    Timber.e("HandlePrimaryContactLogicUseCase: Error checking primary contact for client ${contact.clientId}: ${hasPrimaryResult.error}")
                    QrResult.Error(hasPrimaryResult.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "HandlePrimaryContactLogicUseCase: Exception processing primary logic for contact: ${contact.id}")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }
}