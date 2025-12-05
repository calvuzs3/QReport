package net.calvuz.qreport.domain.usecase.client.contact

import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.domain.repository.ContactRepository
import javax.inject.Inject

class CheckContactExistsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
){

    /**
     * Verifica che il contatto esista e lo restituisce
     */
    suspend operator fun invoke(contactId: String): Result<Contact> {
        return contactRepository.getContactById(contactId)
            .mapCatching { contact ->
                when {
                    contact == null ->
                        throw NoSuchElementException("Contatto con ID '$contactId' non trovato")
                    !contact.isActive ->
                        throw IllegalStateException("Contatto con ID '$contactId' già eliminato")
                    else -> contact
                }
            }
    }
}