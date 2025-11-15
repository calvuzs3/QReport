package net.calvuz.qreport.domain.usecase.client.contact

import android.util.Patterns
import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.domain.repository.ClientRepository
import net.calvuz.qreport.domain.repository.ContactRepository
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
    private val clientRepository: ClientRepository
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

    /**
     * Validazione dati contatto
     */
    private fun validateContactData(contact: Contact): Result<Unit> {
        return when {
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

            contact.title?.length ?: 0 > 100 ->
                Result.failure(IllegalArgumentException("Titolo troppo lungo (max 100 caratteri)"))

            contact.role?.length ?: 0 > 100 ->
                Result.failure(IllegalArgumentException("Ruolo troppo lungo (max 100 caratteri)"))

            contact.department?.length ?: 0 > 100 ->
                Result.failure(IllegalArgumentException("Dipartimento troppo lungo (max 100 caratteri)"))

            !hasAnyContactInfo(contact) ->
                Result.failure(IllegalArgumentException("Deve essere fornito almeno un metodo di contatto (email, telefono o cellulare)"))

            else -> Result.success(Unit)
        }
    }

    /**
     * Verifica che il cliente esista
     */
    private suspend fun checkClientExists(clientId: String): Result<Unit> {
        return clientRepository.getClientById(clientId)
            .mapCatching { client ->
                when {
                    client == null ->
                        throw NoSuchElementException("Cliente con ID '$clientId' non trovato")
                    !client.isActive ->
                        throw IllegalStateException("Cliente con ID '$clientId' non attivo")
                }
            }
    }

    /**
     * Controllo univocità email globale
     */
    private suspend fun checkEmailUniqueness(email: String): Result<Unit> {
        return contactRepository.isEmailTaken(email)
            .mapCatching { isTaken ->
                if (isTaken) {
                    throw IllegalArgumentException("Email '$email' già utilizzata")
                }
            }
    }

    /**
     * Controllo univocità telefono globale
     */
    private suspend fun checkPhoneUniqueness(phone: String): Result<Unit> {
        return contactRepository.isPhoneTaken(phone)
            .mapCatching { isTaken ->
                if (isTaken) {
                    throw IllegalArgumentException("Numero di telefono '$phone' già utilizzato")
                }
            }
    }

    /**
     * Gestione automatica primary contact
     */
    private suspend fun handlePrimaryContactLogic(contact: Contact): Result<Contact> {
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
     * Crea contatto con controllo esplicito primary
     *
     * @param contact Contatto da creare
     * @param forcePrimary Se true, forza questo contatto come primary (rimuove primary da altri)
     * @return Result con Unit se successo
     */
    suspend fun createContactWithPrimary(contact: Contact, forcePrimary: Boolean): Result<Unit> {
        return try {
            if (forcePrimary) {
                // Prima valida e crea il contatto come non-primary
                val tempContact = contact.copy(isPrimary = false)
                invoke(tempContact).onFailure { return Result.failure(it) }

                // Poi imposta come primary (che rimuoverà primary da altri)
                contactRepository.setPrimaryContact(contact.clientId, contact.id)
            } else {
                invoke(contact)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}