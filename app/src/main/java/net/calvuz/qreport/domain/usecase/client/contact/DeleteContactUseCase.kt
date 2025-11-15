package net.calvuz.qreport.domain.usecase.client.contact

import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.domain.repository.ContactRepository
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
    private val contactRepository: ContactRepository
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
     * Verifica che il contatto esista e lo restituisce
     */
    private suspend fun checkContactExists(contactId: String): Result<Contact> {
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

    /**
     * Gestisce l'eliminazione di un contatto primary
     */
    private suspend fun handlePrimaryContactDeletion(
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

    /**
     * Verifica se un contatto può essere eliminato
     *
     * @param contactId ID del contatto da verificare
     * @return Result con Boolean - true se può essere eliminato, false altrimenti
     */
    suspend fun canDeleteContact(contactId: String): Result<Boolean> {
        return try {
            checkContactExists(contactId)
                .map { true } // Se esiste e è attivo, può essere eliminato
                .recover { false } // Se non esiste o non è attivo, non può essere eliminato
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

    /**
     * Elimina tutti i contatti di un cliente (uso interno per eliminazione cliente)
     *
     * @param clientId ID del cliente
     * @return Result con numero di contatti eliminati
     */
    suspend fun deleteAllContactsForClient(clientId: String): Result<Int> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            val contacts = contactRepository.getContactsByClient(clientId)
                .getOrElse { return Result.failure(it) }

            var deletedCount = 0

            contacts.forEach { contact ->
                contactRepository.deleteContact(contact.id)
                    .onSuccess { deletedCount++ }
                    .onFailure { return Result.failure(it) }
            }

            Result.success(deletedCount)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Conta i contatti rimanenti dopo una potenziale eliminazione
     *
     * @param contactId ID del contatto da eliminare
     * @return Result con numero di contatti che rimarrebbero
     */
    suspend fun countRemainingContactsAfterDeletion(contactId: String): Result<Int> {
        return try {
            val contact = checkContactExists(contactId)
                .getOrElse { return Result.failure(it) }

            val currentCount = contactRepository.getContactsCountByClient(contact.clientId)
                .getOrElse { return Result.failure(it) }

            Result.success(currentCount - 1)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}