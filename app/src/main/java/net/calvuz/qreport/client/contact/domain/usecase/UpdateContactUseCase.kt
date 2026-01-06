package net.calvuz.qreport.client.contact.domain.usecase

import kotlinx.datetime.Instant
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import net.calvuz.qreport.client.contact.domain.validator.ContactDataValidator
import javax.inject.Inject

/**
 * Use Case per aggiornamento di un contatto esistente
 *
 * Gestisce:
 * - Validazione esistenza contatto
 * - Validazione dati aggiornati
 * - Controllo duplicati escludendo il contatto corrente
 * - Gestione cambio primary contact
 * - Aggiornamento timestamp
 */
class UpdateContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val checkContactExists: CheckContactExistsUseCase,
    private val checkEmailUniqueness: CheckEmailUniquenessUseCase,
    private val checkPhoneUniqueness: CheckPhoneUniquenessUseCase,
    private val validateContactData: ContactDataValidator,
    private val handlePrimaryContactUpdate: HandlePrimaryContactUpdateUseCase
) {

    /**
     * Aggiorna un contatto esistente
     *
     * @param contact Contatto con dati aggiornati (deve avere ID esistente)
     * @return Result con Unit se successo, errore con dettagli se fallimento
     */
    suspend operator fun invoke(contact: Contact): Result<Unit> {
        return try {
            // 1. Validazione esistenza contatto
            val originalContact = checkContactExists(contact.id)
                .getOrElse { return Result.failure(it) }

            // 2. Validazione dati aggiornati
            validateContactData(contact).onFailure { return Result.failure(it) }

            // 3. Controllo duplicati email (se cambiata e fornita)
            contact.email?.takeIf { it.isNotBlank() }?.let { email ->
                if (email != originalContact.email) {
                    checkEmailUniqueness( email).onFailure { return Result.failure(it) }
                }
            }

            // 4. Controllo duplicati telefono (se cambiato e fornito)
            contact.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                if (phone != originalContact.phone) {
                    checkPhoneUniqueness( phone).onFailure { return Result.failure(it) }
                }
            }

            // 5. Controllo mobile phone (se cambiato e fornito)
            contact.mobilePhone?.takeIf { it.isNotBlank() }?.let { mobile ->
                if (mobile != originalContact.mobilePhone) {
                    checkPhoneUniqueness( mobile).onFailure { return Result.failure(it) }
                }
            }

            // 6. Gestione cambio primary contact
            val finalContact = handlePrimaryContactUpdate(originalContact, contact)
                .getOrElse { return Result.failure(it) }

            // 7. Aggiornamento con timestamp corrente
            val updatedContact = finalContact.copy(updatedAt = Instant.fromEpochMilliseconds(System.currentTimeMillis()))
            contactRepository.updateContact(updatedContact)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}