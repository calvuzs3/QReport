package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import javax.inject.Inject

class HandlePrimaryContactDeletionUseCase @Inject constructor(
    private val contactRepository: ContactRepository
){

    /**
     * Gestisce l'eliminazione di un contatto primary
     */
    suspend operator fun invoke(
        primaryContact: Contact
    ): Result<Unit> {
        return try {
            // Ottieni tutti gli altri contatti attivi del cliente
            val clientContacts = contactRepository.getContactsByClient(primaryContact.clientId)
                .getOrElse { return Result.failure(it) }

            val otherActiveContacts = clientContacts.filter {
                it.id != primaryContact.id && it.isActive
            }

            // Se non ci sono altri contatti, ok eliminare (il cliente rimane senza contatti)
            if (otherActiveContacts.isEmpty()) {
                return Result.success(Unit)
            }

            // Altrimenti, assegna primary al primo contatto nell'ordinamento
            val newPrimaryContact = otherActiveContacts.sortedWith(
                compareBy<Contact> { it.firstName.lowercase() }
                    .thenBy { it.lastName?.lowercase() ?: "" }
            ).first()

            // Imposta il nuovo primary contact
            contactRepository.setPrimaryContact(primaryContact.clientId, newPrimaryContact.id)
                .onFailure { return Result.failure(it) }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}