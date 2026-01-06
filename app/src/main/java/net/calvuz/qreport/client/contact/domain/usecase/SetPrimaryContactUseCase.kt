package net.calvuz.qreport.client.contact.domain.usecase

import kotlinx.datetime.Instant
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import javax.inject.Inject

/**
 * Use Case per impostare un contatto come primary
 *
 * Gestisce:
 * - Validazione esistenza contatto e cliente
 * - Rimozione automatica primary precedente
 * - Impostazione nuovo primary
 * - Verifica che contatto e cliente siano attivi
 *
 * Business Rules:
 * - Solo un contatto primary per cliente
 * - Il nuovo primary deve appartenere al cliente specificato
 * - Operazione atomica (rimuovi vecchio + imposta nuovo)
 */
class SetPrimaryContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val checkContactExists: CheckContactExistsUseCase
) {

    /**
     * Imposta un contatto come primary per il suo cliente
     *
     * @param contactId ID del contatto da impostare come primary
     * @return Result con Unit se successo, errore con dettagli se fallimento
     */
    suspend operator fun invoke(contactId: String): Result<Unit> {
        return try {
            // 1. Validazione input
            if (contactId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID contatto non può essere vuoto"))
            }

            // 2. Verificare che il contatto esista ed è attivo
            val contact = checkContactExists(contactId)
                .getOrElse { return Result.failure(it) }

            // 3. Se è già primary, non serve fare nulla
            if (contact.isPrimary) {
                return Result.success(Unit)
            }

            // 4. Impostare come primary (il repository gestisce la rimozione del precedente)
            contactRepository.setPrimaryContact(contact.clientId, contactId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imposta un contatto come primary per un cliente specifico
     *
     * @param clientId ID del cliente
     * @param contactId ID del contatto da impostare come primary
     * @return Result con Unit se successo, errore se fallimento
     */
    suspend fun setPrimaryContactForClient(clientId: String, contactId: String): Result<Unit> {
        return try {
            // 1. Validazione input
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }
            if (contactId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID contatto non può essere vuoto"))
            }

            // 2. Verificare che il contatto esista ed è attivo
            val contact = checkContactExists(contactId)
                .getOrElse { return Result.failure(it) }

            // 3. Verificare che il contatto appartenga al cliente
            if (contact.clientId != clientId) {
                return Result.failure(
                    IllegalArgumentException("Il contatto '$contactId' non appartiene al cliente '$clientId'")
                )
            }

            // 4. Se è già primary, non serve fare nulla
            if (contact.isPrimary) {
                return Result.success(Unit)
            }

            // 5. Impostare come primary
            contactRepository.setPrimaryContact(clientId, contactId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Rimuove il flag primary da un contatto (se possibile)
     *
     * @param contactId ID del contatto da cui rimuovere il flag primary
     * @return Result con Unit se successo, errore se fallimento
     */
    suspend fun removePrimaryStatus(contactId: String): Result<Unit> {
        return try {
            // 1. Validazione input
            if (contactId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID contatto non può essere vuoto"))
            }

            // 2. Verificare che il contatto esista ed è primary
            val contact = checkContactExists(contactId)
                .getOrElse { return Result.failure(it) }

            if (!contact.isPrimary) {
                return Result.failure(
                    IllegalArgumentException("Il contatto '$contactId' non è primary")
                )
            }

            // 3. Verificare che ci siano altri contatti per il cliente
            val clientContacts = contactRepository.getContactsByClient(contact.clientId)
                .getOrElse { return Result.failure(it) }

            val otherActiveContacts = clientContacts.filter {
                it.id != contact.id && it.isActive
            }

            if (otherActiveContacts.isEmpty()) {
                return Result.failure(
                    IllegalStateException("Impossibile rimuovere primary: è l'unico contatto attivo del cliente")
                )
            }

            // 4. Aggiornare il contatto per rimuovere primary
            val updatedContact = contact.copy(
                isPrimary = false,
                updatedAt = Instant.fromEpochMilliseconds( System.currentTimeMillis())
            )

            contactRepository.updateContact(updatedContact)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene il contatto primary di un cliente
     *
     * @param clientId ID del cliente
     * @return Result con contatto primary se esiste, null altrimenti
     */
    suspend fun getCurrentPrimaryContact(clientId: String): Result<Contact?> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            contactRepository.getPrimaryContact(clientId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica se un cliente ha un contatto primary
     *
     * @param clientId ID del cliente
     * @return Result con Boolean - true se ha primary, false altrimenti
     */
    suspend fun hasPrimaryContact(clientId: String): Result<Boolean> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            contactRepository.hasPrimaryContact(clientId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Suggerisce il miglior candidato per diventare primary contact
     *
     * @param clientId ID del cliente
     * @return Result con contatto suggerito, null se non ci sono contatti
     */
    suspend fun suggestBestPrimaryCandidate(clientId: String): Result<Contact?> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            val contacts = contactRepository.getContactsByClient(clientId)
                .getOrElse { return Result.failure(it) }

            if (contacts.isEmpty()) {
                return Result.success(null)
            }

            // Logica di selezione del miglior candidato:
            // 1. Chi ha più metodi di contatto
            // 2. Chi ha email (prioritaria per comunicazioni)
            // 3. Ordine alfabetico per nome

            val bestCandidate = contacts
                .filter { it.isActive && !it.isPrimary }
                .sortedWith(
                    compareByDescending<Contact> { contact ->
                        // Conta i metodi di contatto disponibili
                        var score = 0
                        if (contact.email?.isNotBlank() == true) score += 3 // Email vale di più
                        if (contact.phone?.isNotBlank() == true) score += 1
                        if (contact.mobilePhone?.isNotBlank() == true) score += 1
                        score
                    }
                        .thenBy { !it.email.isNullOrBlank() } // Preferisci chi ha email
                        .thenBy { it.firstName.lowercase() }
                        .thenBy { it.lastName?.lowercase() ?: "" }
                )
                .firstOrNull()

            Result.success(bestCandidate)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Assegna automaticamente un primary contact se il cliente non ne ha uno
     *
     * @param clientId ID del cliente
     * @return Result con contatto che è stato impostato come primary, null se non ce n'erano
     */
    suspend fun autoAssignPrimaryIfMissing(clientId: String): Result<Contact?> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            // Controlla se ha già un primary
            val hasPrimary = contactRepository.hasPrimaryContact(clientId)
                .getOrElse { return Result.failure(it) }

            if (hasPrimary) {
                // Ha già un primary, ritorna quello esistente
                return contactRepository.getPrimaryContact(clientId)
            }

            // Non ha primary, trova il miglior candidato
            val bestCandidate = suggestBestPrimaryCandidate(clientId)
                .getOrElse { return Result.failure(it) }

            bestCandidate?.let { candidate ->
                contactRepository.setPrimaryContact(clientId, candidate.id)
                    .onFailure { return Result.failure(it) }

                // Ritorna il contatto aggiornato
                contactRepository.getContactById(candidate.id)
            } ?: Result.success(null)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}