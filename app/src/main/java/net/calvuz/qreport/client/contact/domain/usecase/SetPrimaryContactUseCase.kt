package net.calvuz.qreport.client.contact.domain.usecase

import kotlinx.datetime.Instant
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
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
 *
 * Updated to use QrResult<T, QrError> pattern for all methods
 */
class SetPrimaryContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val checkContactExists: CheckContactExistsUseCase
) {

    /**
     * Imposta un contatto come primary per il suo cliente
     *
     * @param contactId ID del contatto da impostare come primary
     * @return QrResult.Success se operazione completata, QrResult.Error per errori
     */
    suspend operator fun invoke(contactId: String): QrResult<Unit, QrError> {
        return try {
            // 1. Validazione input
            if (contactId.isBlank()) {
                Timber.w("SetPrimaryContactUseCase: contactId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(contactId.toString()))
            }

            Timber.d("SetPrimaryContactUseCase: Setting primary contact: $contactId")

            // 2. Verificare che il contatto esista ed è attivo
            val contact = when (val contactResult = checkContactExists(contactId)) {
                is QrResult.Success -> contactResult.data
                is QrResult.Error -> {
                    Timber.w("SetPrimaryContactUseCase: Contact existence check failed: ${contactResult.error}")
                    return QrResult.Error(contactResult.error)
                }
            }

            // 3. Se è già primary, non serve fare nulla
            if (contact.isPrimary) {
                Timber.d("SetPrimaryContactUseCase: Contact is already primary: $contactId")
                return QrResult.Success(Unit)
            }

            // 4. Impostare come primary (il repository gestisce la rimozione del precedente)
            when (val setPrimaryResult = contactRepository.setPrimaryContact(contact.clientId, contactId)) {
                is QrResult.Success -> {
                    Timber.d("SetPrimaryContactUseCase: Primary contact set successfully: $contactId")
                    QrResult.Success(Unit)
                }

                is QrResult.Error -> {
                    Timber.e("SetPrimaryContactUseCase: Repository error setting primary contact $contactId: ${setPrimaryResult.error}")
                    QrResult.Error(setPrimaryResult.error)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "SetPrimaryContactUseCase: Exception setting primary contact: $contactId")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Imposta un contatto come primary per un cliente specifico
     *
     * @param clientId ID del cliente
     * @param contactId ID del contatto da impostare come primary
     * @return QrResult.Success se operazione completata, QrResult.Error per errori
     */
    suspend fun setPrimaryContactForClient(clientId: String, contactId: String): QrResult<Unit, QrError> {
        return try {
            // 1. Validazione input
            if (clientId.isBlank()) {
                Timber.w("SetPrimaryContactUseCase.setPrimaryContactForClient: clientId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
            }
            if (contactId.isBlank()) {
                Timber.w("SetPrimaryContactUseCase.setPrimaryContactForClient: contactId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(contactId.toString()))
            }

            Timber.d("SetPrimaryContactUseCase.setPrimaryContactForClient: Setting contact $contactId as primary for client $clientId")

            // 2. Verificare che il contatto esista ed è attivo
            val contact = when (val contactResult = checkContactExists(contactId)) {
                is QrResult.Success -> contactResult.data
                is QrResult.Error -> {
                    Timber.w("SetPrimaryContactUseCase.setPrimaryContactForClient: Contact existence check failed: ${contactResult.error}")
                    return QrResult.Error(contactResult.error)
                }
            }

            // 3. Verificare che il contatto appartenga al cliente
            if (contact.clientId != clientId) {
                Timber.w("SetPrimaryContactUseCase.setPrimaryContactForClient: Contact $contactId does not belong to client $clientId")
                return QrResult.Error(QrError.ValidationError.InvalidOperation(QrError.Contacts.ValidationError.ContactDoesntBelongToClient))
            }

            // 4. Se è già primary, non serve fare nulla
            if (contact.isPrimary) {
                Timber.d("SetPrimaryContactUseCase.setPrimaryContactForClient: Contact is already primary: $contactId")
                return QrResult.Success(Unit)
            }

            // 5. Impostare come primary
            when (val setPrimaryResult = contactRepository.setPrimaryContact(clientId, contactId)) {
                is QrResult.Success -> {
                    Timber.d("SetPrimaryContactUseCase.setPrimaryContactForClient: Primary contact set successfully: $contactId for client $clientId")
                    QrResult.Success(Unit)
                }

                is QrResult.Error -> {
                    Timber.e("SetPrimaryContactUseCase.setPrimaryContactForClient: Repository error setting primary contact $contactId for client $clientId: ${setPrimaryResult.error}")
                    QrResult.Error(setPrimaryResult.error)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "SetPrimaryContactUseCase.setPrimaryContactForClient: Exception setting primary contact $contactId for client $clientId")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Rimuove il flag primary da un contatto (se possibile)
     *
     * @param contactId ID del contatto da cui rimuovere il flag primary
     * @return QrResult.Success se operazione completata, QrResult.Error per errori
     */
    suspend fun removePrimaryStatus(contactId: String): QrResult<Unit, QrError> {
        return try {
            // 1. Validazione input
            if (contactId.isBlank()) {
                Timber.w("SetPrimaryContactUseCase.removePrimaryStatus: contactId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(contactId.toString()))
            }

            Timber.d("SetPrimaryContactUseCase.removePrimaryStatus: Removing primary status from contact: $contactId")

            // 2. Verificare che il contatto esista ed è primary
            val contact = when (val contactResult = checkContactExists(contactId)) {
                is QrResult.Success -> contactResult.data
                is QrResult.Error -> {
                    Timber.w("SetPrimaryContactUseCase.removePrimaryStatus: Contact existence check failed: ${contactResult.error}")
                    return QrResult.Error(contactResult.error)
                }
            }

            if (!contact.isPrimary) {
                Timber.w("SetPrimaryContactUseCase.removePrimaryStatus: Contact is not primary: $contactId")
                return QrResult.Error(QrError.ValidationError.InvalidOperation(QrError.ValidationError.IsNotPrimary()))
            }

            // 3. Verificare che ci siano altri contatti per il cliente
            val clientContacts = when (val contactsResult = contactRepository.getContactsByClient(contact.clientId)) {
                is QrResult.Success -> contactsResult.data
                is QrResult.Error -> {
                    Timber.e("SetPrimaryContactUseCase.removePrimaryStatus: Error getting client contacts: ${contactsResult.error}")
                    return QrResult.Error(contactsResult.error)
                }
            }

            val otherActiveContacts = clientContacts.filter {
                it.id != contact.id && it.isActive
            }

            if (otherActiveContacts.isEmpty()) {
                Timber.w("SetPrimaryContactUseCase.removePrimaryStatus: Cannot remove primary - is the only active contact: $contactId")
                return QrResult.Error(QrError.ValidationError.InvalidOperation(QrError.Contacts.ValidationError.CannotRemovePrimaryFlag))
            }

            // 4. Aggiornare il contatto per rimuovere primary
            val updatedContact = contact.copy(
                isPrimary = false,
                updatedAt = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            )

            when (val updateResult = contactRepository.updateContact(updatedContact)) {
                is QrResult.Success -> {
                    Timber.d("SetPrimaryContactUseCase.removePrimaryStatus: Primary status removed successfully: $contactId")
                    QrResult.Success(Unit)
                }

                is QrResult.Error -> {
                    Timber.e("SetPrimaryContactUseCase.removePrimaryStatus: Repository error updating contact $contactId: ${updateResult.error}")
                    QrResult.Error(updateResult.error)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "SetPrimaryContactUseCase.removePrimaryStatus: Exception removing primary status: $contactId")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Ottiene il contatto primary di un cliente
     *
     * @param clientId ID del cliente
     * @return QrResult.Success con contatto primary se esiste, null altrimenti
     */
    suspend fun getCurrentPrimaryContact(clientId: String): QrResult<Contact?, QrError> {
        return try {
            if (clientId.isBlank()) {
                Timber.w("SetPrimaryContactUseCase.getCurrentPrimaryContact: clientId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
            }

            Timber.d("SetPrimaryContactUseCase.getCurrentPrimaryContact: Getting primary contact for client: $clientId")

            when (val result = contactRepository.getPrimaryContact(clientId)) {
                is QrResult.Success -> {
                    val contact = result.data
                    if (contact != null) {
                        Timber.d("SetPrimaryContactUseCase.getCurrentPrimaryContact: Primary contact found for client: $clientId")
                    } else {
                        Timber.d("SetPrimaryContactUseCase.getCurrentPrimaryContact: No primary contact found for client: $clientId")
                    }
                    QrResult.Success(contact)
                }

                is QrResult.Error -> {
                    Timber.e("SetPrimaryContactUseCase.getCurrentPrimaryContact: Repository error for client $clientId: ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SetPrimaryContactUseCase.getCurrentPrimaryContact: Exception getting primary contact for client: $clientId")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Verifica se un cliente ha un contatto primary
     *
     * @param clientId ID del cliente
     * @return QrResult.Success con Boolean - true se ha primary, false altrimenti
     */
    suspend fun hasPrimaryContact(clientId: String): QrResult<Boolean, QrError> {
        return try {
            if (clientId.isBlank()) {
                Timber.w("SetPrimaryContactUseCase.hasPrimaryContact: clientId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
            }

            Timber.d("SetPrimaryContactUseCase.hasPrimaryContact: Checking if client has primary contact: $clientId")

            when (val result = contactRepository.hasPrimaryContact(clientId)) {
                is QrResult.Success -> {
                    val hasPrimary = result.data
                    Timber.d("SetPrimaryContactUseCase.hasPrimaryContact: Client $clientId ${if (hasPrimary) "has" else "does not have"} primary contact")
                    QrResult.Success(hasPrimary)
                }

                is QrResult.Error -> {
                    Timber.e("SetPrimaryContactUseCase.hasPrimaryContact: Repository error for client $clientId: ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SetPrimaryContactUseCase.hasPrimaryContact: Exception checking primary contact for client: $clientId")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Suggerisce il miglior candidato per diventare primary contact
     *
     * @param clientId ID del cliente
     * @return QrResult.Success con contatto suggerito, null se non ci sono contatti
     */
    suspend fun suggestBestPrimaryCandidate(clientId: String): QrResult<Contact?, QrError> {
        return try {
            if (clientId.isBlank()) {
                Timber.w("SetPrimaryContactUseCase.suggestBestPrimaryCandidate: clientId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
            }

            Timber.d("SetPrimaryContactUseCase.suggestBestPrimaryCandidate: Finding best primary candidate for client: $clientId")

            val contacts = when (val contactsResult = contactRepository.getContactsByClient(clientId)) {
                is QrResult.Success -> contactsResult.data
                is QrResult.Error -> {
                    Timber.e("SetPrimaryContactUseCase.suggestBestPrimaryCandidate: Error getting client contacts: ${contactsResult.error}")
                    return QrResult.Error(contactsResult.error)
                }
            }

            if (contacts.isEmpty()) {
                Timber.d("SetPrimaryContactUseCase.suggestBestPrimaryCandidate: No contacts found for client: $clientId")
                return QrResult.Success(null)
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

            if (bestCandidate != null) {
                Timber.d("SetPrimaryContactUseCase.suggestBestPrimaryCandidate: Best candidate found: ${bestCandidate.id} for client: $clientId")
            } else {
                Timber.d("SetPrimaryContactUseCase.suggestBestPrimaryCandidate: No suitable candidate found for client: $clientId")
            }

            QrResult.Success(bestCandidate)

        } catch (e: Exception) {
            Timber.e(e, "SetPrimaryContactUseCase.suggestBestPrimaryCandidate: Exception finding best candidate for client: $clientId")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Assegna automaticamente un primary contact se il cliente non ne ha uno
     *
     * @param clientId ID del cliente
     * @return QrResult.Success con contatto che è stato impostato come primary, null se non ce n'erano
     */
    suspend fun autoAssignPrimaryIfMissing(clientId: String): QrResult<Contact?, QrError> {
        return try {
            if (clientId.isBlank()) {
                Timber.w("SetPrimaryContactUseCase.autoAssignPrimaryIfMissing: clientId is blank")
                return QrResult.Error(QrError.ValidationError.InvalidOperation(QrError.Contacts.ValidationError.IdMandatory()))
            }

            Timber.d("SetPrimaryContactUseCase.autoAssignPrimaryIfMissing: Auto-assigning primary if missing for client: $clientId")

            // Controlla se ha già un primary
            val hasPrimary = when (val hasPrimaryResult = contactRepository.hasPrimaryContact(clientId)) {
                is QrResult.Success -> hasPrimaryResult.data
                is QrResult.Error -> {
                    Timber.e("SetPrimaryContactUseCase.autoAssignPrimaryIfMissing: Error checking primary contact: ${hasPrimaryResult.error}")
                    return QrResult.Error(hasPrimaryResult.error)
                }
            }

            if (hasPrimary) {
                // Ha già un primary, ritorna quello esistente
                Timber.d("SetPrimaryContactUseCase.autoAssignPrimaryIfMissing: Client already has primary, returning existing")
                return when (val primaryResult = contactRepository.getPrimaryContact(clientId)) {
                    is QrResult.Success -> QrResult.Success(primaryResult.data)
                    is QrResult.Error -> QrResult.Error(primaryResult.error)
                }
            }

            // Non ha primary, trova il miglior candidato
            val bestCandidate = when (val candidateResult = suggestBestPrimaryCandidate(clientId)) {
                is QrResult.Success -> candidateResult.data
                is QrResult.Error -> {
                    Timber.e("SetPrimaryContactUseCase.autoAssignPrimaryIfMissing: Error finding best candidate: ${candidateResult.error}")
                    return QrResult.Error(candidateResult.error)
                }
            }

            bestCandidate?.let { candidate ->
                when (val setPrimaryResult = contactRepository.setPrimaryContact(clientId, candidate.id)) {
                    is QrResult.Success -> {
                        Timber.d("SetPrimaryContactUseCase.autoAssignPrimaryIfMissing: Auto-assigned primary contact: ${candidate.id}")

                        // Ritorna il contatto aggiornato
                        when (val updatedContactResult = contactRepository.getContactById(candidate.id)) {
                            is QrResult.Success -> QrResult.Success(updatedContactResult.data)
                            is QrResult.Error -> QrResult.Error(updatedContactResult.error)
                        }
                    }

                    is QrResult.Error -> {
                        Timber.e("SetPrimaryContactUseCase.autoAssignPrimaryIfMissing: Error setting primary contact: ${setPrimaryResult.error}")
                        QrResult.Error(setPrimaryResult.error)
                    }
                }
            } ?: run {
                Timber.d("SetPrimaryContactUseCase.autoAssignPrimaryIfMissing: No candidate found for auto-assignment")
                QrResult.Success(null)
            }

        } catch (e: Exception) {
            Timber.e(e, "SetPrimaryContactUseCase.autoAssignPrimaryIfMissing: Exception auto-assigning primary for client: $clientId")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }
}