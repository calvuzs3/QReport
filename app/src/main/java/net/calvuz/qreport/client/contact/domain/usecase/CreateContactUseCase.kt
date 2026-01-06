package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import net.calvuz.qreport.client.client.domain.usecase.CheckClientExistsUseCase
import net.calvuz.qreport.client.contact.domain.validator.ContactDataValidator
import javax.inject.Inject

/**
 * Use Case per creazione di un nuovo contatto
 *
 * Gestisce:
 * - Validazione dati contatto
 * - Verifica esistenza cliente
 * - Controllo duplicati email/telefono
 * - Gestione primary contact automatica
 *
 * Business Rules:
 * - Email deve essere unica globalmente
 * - Telefono deve essere unico globalmente
 * - Se è il primo contatto del cliente, diventa automaticamente primary
 * - Solo un contatto primary per cliente
 */
class CreateContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val validateContactData: ContactDataValidator,
    private val checkClientExists: CheckClientExistsUseCase,
    private val checkEmailUniqueness: CheckEmailUniquenessUseCase,
    private val checkPhoneUniqueness: CheckPhoneUniquenessUseCase,
    private val handlePrimaryContactLogic: HandlePrimaryContactLogicUseCase
) {

    /**
     * Crea un nuovo contatto
     *
     * @param contact Contatto da creare
     * @return Result con Unit se successo, errore con dettagli se fallimento
     */
    suspend operator fun invoke(contact: Contact): Result<Unit> {
        return try {
            // 1. Validazione dati base
            validateContactData(contact).onFailure { return Result.failure(it) }

            // 2. Verifica che il cliente esista
            checkClientExists(contact.clientId).onFailure { return Result.failure(it) }

            // 3. Controllo duplicati email (se fornita)
            contact.email?.takeIf { it.isNotBlank() }?.let { email ->
                checkEmailUniqueness(email).onFailure { return Result.failure(it) }
            }

            // 4. Controllo duplicati telefono (se fornito)
            contact.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                checkPhoneUniqueness(phone).onFailure { return Result.failure(it) }
            }

            // 5. Controllo mobile phone (se fornito)
            contact.mobilePhone?.takeIf { it.isNotBlank() }?.let { mobile ->
                checkPhoneUniqueness(mobile).onFailure { return Result.failure(it) }
            }

            // 6. Gestione primary contact automatica
            val finalContact = handlePrimaryContactLogic(contact)
                .getOrElse { return Result.failure(it) }

            // 7. Creazione nel repository
            contactRepository.createContact(finalContact)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }




}