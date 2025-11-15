package net.calvuz.qreport.domain.usecase.client.contact

import android.util.Patterns
import kotlinx.datetime.Instant
import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.domain.model.client.ContactMethod
import net.calvuz.qreport.domain.repository.ContactRepository
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
    private val contactRepository: ContactRepository
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
                    checkEmailUniqueness(contact.id, email).onFailure { return Result.failure(it) }
                }
            }

            // 4. Controllo duplicati telefono (se cambiato e fornito)
            contact.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                if (phone != originalContact.phone) {
                    checkPhoneUniqueness(contact.id, phone).onFailure { return Result.failure(it) }
                }
            }

            // 5. Controllo mobile phone (se cambiato e fornito)
            contact.mobilePhone?.takeIf { it.isNotBlank() }?.let { mobile ->
                if (mobile != originalContact.mobilePhone) {
                    checkPhoneUniqueness(contact.id, mobile).onFailure { return Result.failure(it) }
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

    /**
     * Verifica che il contatto esista e lo restituisce
     */
    private suspend fun checkContactExists(contactId: String): Result<Contact> {
        return contactRepository.getContactById(contactId)
            .mapCatching { contact ->
                contact ?: throw NoSuchElementException("Contatto con ID '$contactId' non trovato")
            }
    }

    /**
     * Validazione dati contatto
     */
    private fun validateContactData(contact: Contact): Result<Unit> {
        return when {
            contact.id.isBlank() ->
                Result.failure(IllegalArgumentException("ID contatto è obbligatorio"))

            contact.clientId.isBlank() ->
                Result.failure(IllegalArgumentException("ID cliente è obbligatorio"))

            contact.firstName.isBlank() ->
                Result.failure(IllegalArgumentException("Nome è obbligatorio"))

            contact.firstName.length < 2 ->
                Result.failure(IllegalArgumentException("Nome deve essere di almeno 2 caratteri"))

            contact.firstName.length > 100 ->
                Result.failure(IllegalArgumentException("Nome troppo lungo (max 100 caratteri)"))

            contact.lastName?.let { it.length > 100 } == true ->
                Result.failure(IllegalArgumentException("Cognome troppo lungo (max 100 caratteri)"))

            contact.email?.isNotBlank() == true && !isValidEmail(contact.email) ->
                Result.failure(IllegalArgumentException("Formato email non valido"))

            contact.phone?.isNotBlank() == true && !isValidPhone(contact.phone) ->
                Result.failure(IllegalArgumentException("Formato telefono non valido"))

            contact.mobilePhone?.isNotBlank() == true && !isValidPhone(contact.mobilePhone) ->
                Result.failure(IllegalArgumentException("Formato cellulare non valido"))

            (contact.title?.length ?: 0) > 100 ->
                Result.failure(IllegalArgumentException("Titolo troppo lungo (max 100 caratteri)"))

            (contact.role?.length ?: 0) > 100 ->
                Result.failure(IllegalArgumentException("Ruolo troppo lungo (max 100 caratteri)"))

            (contact.department?.length ?: 0) > 100 ->
                Result.failure(IllegalArgumentException("Dipartimento troppo lungo (max 100 caratteri)"))

            contact.alternativeEmail?.isNotBlank() == true && !isValidEmail(contact.alternativeEmail) ->
                Result.failure(IllegalArgumentException("Formato email alternativa non valido"))

            !hasAnyContactInfo(contact) ->
                Result.failure(IllegalArgumentException("Deve essere fornito almeno un metodo di contatto (email, telefono o cellulare)"))

            else -> Result.success(Unit)
        }
    }

    /**
     * Controllo univocità email escludendo il contatto corrente
     */
    private suspend fun checkEmailUniqueness(contactId: String, email: String): Result<Unit> {
        return contactRepository.isEmailTaken(email, contactId)
            .mapCatching { isTaken ->
                if (isTaken) {
                    throw IllegalArgumentException("Email '$email' già utilizzata da un altro contatto")
                }
            }
    }

    /**
     * Controllo univocità telefono escludendo il contatto corrente
     */
    private suspend fun checkPhoneUniqueness(contactId: String, phone: String): Result<Unit> {
        return contactRepository.isPhoneTaken(phone, contactId)
            .mapCatching { isTaken ->
                if (isTaken) {
                    throw IllegalArgumentException("Numero '$phone' già utilizzato da un altro contatto")
                }
            }
    }

    /**
     * Gestisce la logica di aggiornamento del primary contact
     */
    private suspend fun handlePrimaryContactUpdate(
        originalContact: Contact,
        updatedContact: Contact
    ): Result<Contact> {
        return try {
            // Se non c'è cambio nel flag primary, mantieni tutto uguale
            if (originalContact.isPrimary == updatedContact.isPrimary) {
                return Result.success(updatedContact)
            }

            when {
                // Caso 1: Sta diventando primary
                updatedContact.isPrimary && !originalContact.isPrimary -> {
                    // Il repository gestirà automaticamente la rimozione del primary precedente
                    Result.success(updatedContact)
                }

                // Caso 2: Sta smettendo di essere primary
                !updatedContact.isPrimary && originalContact.isPrimary -> {
                    // Verifica che non sia l'unico contatto del cliente
                    val clientContacts = contactRepository.getContactsByClient(originalContact.clientId)
                        .getOrElse { return Result.failure(it) }

                    if (clientContacts.size == 1) {
                        throw IllegalStateException("Non è possibile rimuovere il flag primary: è l'unico contatto del cliente")
                    }

                    Result.success(updatedContact)
                }

                else -> Result.success(updatedContact)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica che abbia almeno un metodo di contatto
     */
    private fun hasAnyContactInfo(contact: Contact): Boolean {
        return contact.email?.isNotBlank() == true ||
                contact.phone?.isNotBlank() == true ||
                contact.mobilePhone?.isNotBlank() == true
    }

    /**
     * Validazione formato email
     */
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validazione formato telefono (italiano/internazionale)
     */
    private fun isValidPhone(phone: String): Boolean {
        val cleanPhone = phone.replace("\\s+".toRegex(), "").replace("-", "")

        return when {
            // Numero italiano fisso/mobile
            cleanPhone.matches("\\+39\\d{9,10}".toRegex()) -> true
            cleanPhone.matches("39\\d{9,10}".toRegex()) -> true
            cleanPhone.matches("0\\d{8,10}".toRegex()) -> true // Fisso
            cleanPhone.matches("3\\d{8,9}".toRegex()) -> true  // Mobile
            // Formato internazionale generico
            cleanPhone.matches("\\+\\d{7,15}".toRegex()) -> true
            // Solo numeri (minimo 7, massimo 15 cifre)
            cleanPhone.matches("\\d{7,15}".toRegex()) -> true
            else -> false
        }
    }

    /**
     * Aggiorna solo campi specifici di un contatto
     *
     * @param contactId ID del contatto da aggiornare
     * @param updates Mappa campo -> nuovo valore
     * @return Result con Unit se successo
     */
    suspend fun updateContactFields(
        contactId: String,
        updates: Map<String, Any?>
    ): Result<Unit> {
        return try {
            val originalContact = checkContactExists(contactId)
                .getOrElse { return Result.failure(it) }

            var updatedContact = originalContact

            updates.forEach { (field, value) ->
                updatedContact = when (field) {
                    "firstName" -> updatedContact.copy(firstName = value as String)
                    "lastName" -> updatedContact.copy(lastName = value as? String)
                    "title" -> updatedContact.copy(title = value as? String)
                    "role" -> updatedContact.copy(role = value as? String)
                    "department" -> updatedContact.copy(department = value as? String)
                    "email" -> updatedContact.copy(email = value as? String)
                    "alternativeEmail" -> updatedContact.copy(alternativeEmail = value as? String)
                    "phone" -> updatedContact.copy(phone = value as? String)
                    "mobilePhone" -> updatedContact.copy(mobilePhone = value as? String)
                    "preferredContactMethod" -> updatedContact.copy(preferredContactMethod = value as? ContactMethod)
                    "notes" -> updatedContact.copy(notes = value as? String)
                    "isPrimary" -> updatedContact.copy(isPrimary = value as Boolean)
                    else -> throw IllegalArgumentException("Campo '$field' non supportato")
                }
            }

            invoke(updatedContact)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}