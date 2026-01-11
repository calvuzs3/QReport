package net.calvuz.qreport.client.contact.domain.validator

import android.util.Patterns
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import javax.inject.Inject

/**
 * Validazione dati contatto
 */
class ContactDataValidator @Inject constructor() {

    operator fun invoke(contact: Contact): QrResult<Unit, QrError> {
        return when {
            contact.clientId.isBlank() ->
                QrResult.Error(QrError.Contacts.ValidationError.IdClientMandatory())

            contact.firstName.isBlank() ->
                QrResult.Error(QrError.Contacts.ValidationError.IdClientMandatory())

            contact.firstName.length < 2 ->
                QrResult.Error(QrError.Contacts.ValidationError.IdClientMandatory())

            contact.firstName.length > 100 ->
                QrResult.Error(QrError.Contacts.ValidationError.IdClientMandatory())

            contact.lastName?.let { it.length > 100 } == true ->
                QrResult.Error(QrError.Contacts.ValidationError.IdClientMandatory())

            contact.email?.isNotBlank() == true && !isValidEmail(contact.email) ->
                QrResult.Error(QrError.Contacts.ValidationError.IdClientMandatory())

            contact.phone?.isNotBlank() == true && !isValidPhone(contact.phone) ->
                QrResult.Error(QrError.Contacts.ValidationError.IdClientMandatory())

            contact.mobilePhone?.isNotBlank() == true && !isValidPhone(contact.mobilePhone) ->
                QrResult.Error(QrError.Contacts.ValidationError.IdClientMandatory())

            (contact.title?.length ?: 0) > 100 ->
                QrResult.Error(QrError.Contacts.ValidationError.IdClientMandatory())

            (contact.role?.length ?: 0) > 100 ->
                QrResult.Error(QrError.Contacts.ValidationError.IdClientMandatory())

            (contact.department?.length ?: 0) > 100 ->
                QrResult.Error(QrError.Contacts.ValidationError.IdClientMandatory())

            !hasAnyContactInfo(contact) ->
                QrResult.Error(QrError.Contacts.ValidationError.IdClientMandatory())

            else -> QrResult.Success(Unit)
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