package net.calvuz.qreport.client.contact.domain.validator

import android.util.Patterns
import net.calvuz.qreport.client.contact.domain.model.Contact
import javax.inject.Inject

/**
 * Validazione dati contatto
 */
class ContactDataValidator @Inject constructor() {

    operator fun invoke(contact: Contact): Result<Unit> {
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

            (contact.title?.length ?: 0) > 100 ->
                Result.failure(IllegalArgumentException("Titolo troppo lungo (max 100 caratteri)"))

            (contact.role?.length ?: 0) > 100 ->
                Result.failure(IllegalArgumentException("Ruolo troppo lungo (max 100 caratteri)"))

            (contact.department?.length ?: 0) > 100 ->
                Result.failure(IllegalArgumentException("Dipartimento troppo lungo (max 100 caratteri)"))

            !hasAnyContactInfo(contact) ->
                Result.failure(IllegalArgumentException("Deve essere fornito almeno un metodo di contatto (email, telefono o cellulare)"))

            else -> Result.success(Unit)
        }
    }


    /**
     * Validazione formato email
     */
    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validazione formato telefono (italiano/internazionale)
     */
    fun isValidPhone(phone: String): Boolean {
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
     * Verifica che abbia almeno un metodo di contatto
     */
    fun hasAnyContactInfo(contact: Contact): Boolean {
        return contact.email?.isNotBlank() == true ||
                contact.phone?.isNotBlank() == true ||
                contact.mobilePhone?.isNotBlank() == true
    }
}