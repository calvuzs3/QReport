package net.calvuz.qreport.domain.usecase.client.client

import net.calvuz.qreport.domain.model.client.Client
import net.calvuz.qreport.domain.repository.ClientRepository
import javax.inject.Inject

/**
 * Use Case per creazione di un nuovo cliente
 *
 * Gestisce:
 * - Validazione dati client
 * - Controllo duplicati (ragione sociale e partita IVA)
 * - Creazione nel repository
 *
 * Business Rules:
 * - Ragione sociale deve essere unica
 * - Partita IVA deve essere unica (se fornita)
 * - Dati minimi obbligatori
 */
class CreateClientUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {

    /**
     * Crea un nuovo cliente
     *
     * @param client Cliente da creare
     * @return Result con Unit se successo, errore con dettagli se fallimento
     */
    suspend operator fun invoke(client: Client): Result<Unit> {
        return try {
            // 1. Validazione dati base
            validateClientData(client).onFailure { return Result.failure(it) }

            // 2. Controllo duplicati ragione sociale
            checkCompanyNameUniqueness(client).onFailure { return Result.failure(it) }

            // 3. Controllo duplicati partita IVA (se fornita)
            client.vatNumber?.let { vatNumber ->
                checkVatNumberUniqueness(vatNumber).onFailure { return Result.failure(it) }
            }

            // 4. Creazione nel repository
            clientRepository.createClient(client)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validazione dati client
     */
    private fun validateClientData(client: Client): Result<Unit> {
        return when {
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

            else -> Result.success(Unit)
        }
    }

    /**
     * Controllo univocità ragione sociale
     */
    private suspend fun checkCompanyNameUniqueness(client: Client): Result<Unit> {
        return clientRepository.isCompanyNameTaken(client.companyName, client.id)
            .mapCatching { isTaken ->
                if (isTaken) {
                    throw IllegalArgumentException("Ragione sociale '${client.companyName}' già esistente")
                }
            }
    }

    /**
     * Controllo univocità partita IVA
     */
    private suspend fun checkVatNumberUniqueness(vatNumber: String): Result<Unit> {
        return clientRepository.isVatNumberTaken(vatNumber)
            .mapCatching { isTaken ->
                if (isTaken) {
                    throw IllegalArgumentException("Partita IVA '$vatNumber' già esistente")
                }
            }
    }

    /**
     * Validazione formato partita IVA italiana (11 cifre)
     */
    private fun isValidVatNumber(vatNumber: String): Boolean {
        val cleanVat = vatNumber.replace("\\s+".toRegex(), "")
        return cleanVat.matches("\\d{11}".toRegex())
    }

    /**
     * Validazione formato codice fiscale (16 caratteri alfanumerici)
     */
    private fun isValidFiscalCode(fiscalCode: String): Boolean {
        val cleanCode = fiscalCode.replace("\\s+".toRegex(), "").uppercase()
        return cleanCode.matches("[A-Z0-9]{16}".toRegex())
    }
}