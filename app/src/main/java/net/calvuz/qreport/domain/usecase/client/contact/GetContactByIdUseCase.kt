package net.calvuz.qreport.domain.usecase.client.contact

import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.domain.repository.ContactRepository
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

    /**
     * Recupera un contatto per ID
     *
     * @param contactId ID del contatto da recuperare
     * @return Result con Contact se trovato, errore se non trovato o errore di sistema
     */
    suspend operator fun invoke(contactId: String): Result<Contact> {
        return try {
            // 1. Validazione ID
            if (contactId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID contatto non può essere vuoto"))
            }

            // 2. Recupero dal repository
            contactRepository.getContactById(contactId)
                .mapCatching { contact ->
                    // 3. Gestione contatto non trovato
                    contact ?: throw NoSuchElementException("Contatto con ID '$contactId' non trovato")
                }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}