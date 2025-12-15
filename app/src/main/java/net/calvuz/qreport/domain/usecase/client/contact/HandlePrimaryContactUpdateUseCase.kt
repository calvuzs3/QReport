package net.calvuz.qreport.domain.usecase.client.contact

import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.domain.repository.ContactRepository
import javax.inject.Inject

class HandlePrimaryContactUpdateUseCase @Inject constructor(
    private val contactRepository: ContactRepository
){

    /**
     * Gestisce la logica di aggiornamento del primary contact
     */
    suspend operator fun invoke(
        originalContact: Contact,
        updatedContact: Contact
    ): Result<Contact> {
        return try {
            // Se non c'è cambio nel flag primary, mantieni tutto uguale
            if (originalContact.isPrimary == updatedContact.isPrimary) {
                return Result.success(updatedContact)
            }

            when {
                // Caso 1: Sta diventando primary
                updatedContact.isPrimary && !originalContact.isPrimary -> {
                    // Il repository gestirà automaticamente la rimozione del primary precedente
                    Result.success(updatedContact)
                }

                // Caso 2: Sta smettendo di essere primary
                !updatedContact.isPrimary && originalContact.isPrimary -> {
                    // Verifica che non sia l'unico contatto del cliente
                    val clientContacts = contactRepository.getContactsByClient(originalContact.clientId)
                        .getOrElse { return Result.failure(it) }

                    if (clientContacts.size == 1) {
                        throw IllegalStateException("Non è possibile rimuovere il flag primary: è l'unico contatto del cliente")
                    }

                    Result.success(updatedContact)
                }

                else -> Result.success(updatedContact)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}