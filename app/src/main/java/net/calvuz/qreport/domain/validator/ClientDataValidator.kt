package net.calvuz.qreport.domain.validator

import net.calvuz.qreport.domain.model.client.Client
import java.net.URL
import javax.inject.Inject

class ClientDataValidator @Inject constructor(){

    /**
     * Validazione dati client
     */
    operator fun invoke(client: Client): Result<Unit> {
        return when {
            client.id.isBlank() ->
                Result.failure(IllegalArgumentException("ID cliente è obbligatorio"))

            client.companyName.isBlank() ->
                Result.failure(IllegalArgumentException("Ragione sociale è obbligatoria"))

            client.companyName.length < 2 ->
                Result.failure(IllegalArgumentException("Ragione sociale deve essere di almeno 2 caratteri"))

            client.companyName.length > 255 ->
                Result.failure(IllegalArgumentException("Ragione sociale troppo lunga (max 255 caratteri)"))

            client.vatNumber?.isNotBlank() == true && !isValidVatNumber(client.vatNumber) ->
                Result.failure(IllegalArgumentException("Formato partita IVA non valido"))

            client.fiscalCode?.isNotBlank() == true && !isValidFiscalCode(client.fiscalCode) ->
                Result.failure(IllegalArgumentException("Formato codice fiscale non valido"))

            (client.industry?.length ?: 0) > 100 ->
                Result.failure(IllegalArgumentException("Settore troppo lungo (max 100 caratteri)"))

            client.website?.isNotBlank() == true && !isValidWebsite(client.website) ->
                Result.failure(IllegalArgumentException("Formato website non valido"))

            else -> Result.success(Unit)
        }
    }

    /**
     * Validazione formato partita IVA italiana (11 cifre)
     */
    fun isValidVatNumber(vatNumber: String): Boolean {
        val cleanVat = vatNumber.replace("\\s+".toRegex(), "")
        return cleanVat.matches("\\d{11}".toRegex())
    }

    /**
     * Validazione formato codice fiscale (16 caratteri alfanumerici)
     */
    fun isValidFiscalCode(fiscalCode: String): Boolean {
        val cleanCode = fiscalCode.replace("\\s+".toRegex(), "").uppercase()
        return cleanCode.matches("[A-Z0-9]{16}".toRegex())
    }

    /**
     * Validazione formato website
     */
    fun isValidWebsite(website: String): Boolean {
        return try {
            val cleanWebsite = if (!website.startsWith("http")) "https://$website" else website
            URL(cleanWebsite)
            true
        } catch (e: Exception) {
            false
        }
    }
}