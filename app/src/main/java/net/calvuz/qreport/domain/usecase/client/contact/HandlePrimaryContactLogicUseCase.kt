package net.calvuz.qreport.domain.usecase.client.contact

import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.domain.repository.ContactRepository
import javax.inject.Inject

class HandlePrimaryContactLogicUseCase @Inject constructor(
    private val contactRepository: ContactRepository
){

    /**
     * Gestione automatica primary contact
     */
    suspend operator fun invoke(contact: Contact): Result<Contact> {
        return try {
            // Se è già impostato come primary, mantieni
            if (contact.isPrimary) {
                return Result.success(contact)
            }

            // Controlla se il cliente ha già un contatto primary
            val hasPrimary = contactRepository.hasPrimaryContact(contact.clientId)
                .getOrElse { return Result.failure(it) }

            // Se non ha primary, questo diventa primary automaticamente
            val finalContact = if (!hasPrimary) {
                contact.copy(isPrimary = true)
            } else {
                contact
            }

            Result.success(finalContact)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}