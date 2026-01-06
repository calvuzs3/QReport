package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import javax.inject.Inject

/**
 * Use Case per eliminazione di un contatto
 *
 * Gestisce:
 * - Validazione esistenza contatto
 * - Gestione eliminazione primary contact
 * - Assegnazione automatica nuovo primary
 * - Eliminazione sicura (soft delete)
 *
 * Business Rules:
 * - Se si elimina l'ultimo contatto del cliente, viene permesso
 * - Se si elimina un primary contact e ci sono altri contatti, il primo diventa primary
 * - Eliminazione soft per mantenere tracciabilità
 */
class DeleteContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val checkContactExists: CheckContactExistsUseCase,
    private val handlePrimaryContactDeletion: HandlePrimaryContactDeletionUseCase
) {

    /**
     * Elimina un contatto
     *
     * @param contactId ID del contatto da eliminare
     * @return Result con Unit se successo, errore con dettagli se fallimento
     */
    suspend operator fun invoke(contactId: String): Result<Unit> {
        return try {
            // 1. Validazione input
            if (contactId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID contatto non può essere vuoto"))
            }

            // 2. Verificare che il contatto esista
            val contact = checkContactExists(contactId)
                .getOrElse { return Result.failure(it) }

            // 3. Gestire primary contact se necessario
            if (contact.isPrimary) {
                handlePrimaryContactDeletion(contact)
                    .onFailure { return Result.failure(it) }
            }

            // 4. Eliminazione contatto (soft delete)
            contactRepository.deleteContact(contactId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene informazioni sul contatto che diventerà primary se si elimina il contatto specificato
     *
     * @param contactId ID del contatto da eliminare
     * @return Result con nuovo contatto primary (null se non ce ne sarà uno)
     */
    suspend fun getNewPrimaryContactAfterDeletion(contactId: String): Result<Contact?> {
        return try {
            val contact = checkContactExists(contactId)
                .getOrElse { return Result.failure(it) }

            // Se non è primary, niente cambia
            if (!contact.isPrimary) {
                return Result.success(null)
            }

            // Ottieni altri contatti del cliente
            val clientContacts = contactRepository.getContactsByClient(contact.clientId)
                .getOrElse { return Result.failure(it) }

            val otherActiveContacts = clientContacts.filter {
                it.id != contact.id && it.isActive
            }

            if (otherActiveContacts.isEmpty()) {
                Result.success(null) // Nessun altro contatto
            } else {
                // Ritorna il contatto che diventerà primary
                val newPrimary = otherActiveContacts.sortedWith(
                    compareBy<Contact> { it.firstName.lowercase() }
                        .thenBy { it.lastName?.lowercase() ?: "" }
                ).first()

                Result.success(newPrimary)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}