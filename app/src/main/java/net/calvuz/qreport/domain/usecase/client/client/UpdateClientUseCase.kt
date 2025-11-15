package net.calvuz.qreport.domain.usecase.client.client

import kotlinx.datetime.Clock
import net.calvuz.qreport.domain.model.client.Client
import net.calvuz.qreport.domain.repository.ClientRepository
import java.net.URL
import javax.inject.Inject

/**
 * Use Case per aggiornamento di un cliente esistente
 *
 * Gestisce:
 * - Validazione esistenza cliente
 * - Validazione dati aggiornati
 * - Controllo duplicati escludendo il cliente corrente
 * - Aggiornamento timestamp
 */
class UpdateClientUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {

    /**
     * Aggiorna un cliente esistente
     *
     * @param client Cliente con dati aggiornati (deve avere ID esistente)
     * @return Result con Unit se successo, errore con dettagli se fallimento
     */
    suspend operator fun invoke(client: Client): Result<Unit> {
        return try {
            // 1. Validazione esistenza cliente
            checkClientExists(client.id).onFailure { return Result.failure(it) }

            // 2. Validazione dati aggiornati
            validateClientData(client).onFailure { return Result.failure(it) }

            // 3. Controllo duplicati ragione sociale (escludendo questo cliente)
            checkCompanyNameUniqueness(client).onFailure { return Result.failure(it) }

            // 4. Controllo duplicati partita IVA (se fornita, escludendo questo cliente)
            client.vatNumber?.let { vatNumber ->
                checkVatNumberUniqueness(client.id, vatNumber).onFailure { return Result.failure(it) }
            }

            // 5. Aggiornamento con timestamp corrente
            val updatedClient = client.copy(updatedAt = Clock.System.now())
            clientRepository.updateClient(updatedClient)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica che il cliente esista
     */
    private suspend fun checkClientExists(clientId: String): Result<Unit> {
        return clientRepository.getClientById(clientId)
            .mapCatching { client ->
                if (client == null) {
                    throw NoSuchElementException("Cliente con ID '$clientId' non trovato")
                }
            }
    }

    /**
     * Validazione dati client
     */
    private fun validateClientData(client: Client): Result<Unit> {
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

            client.industry?.length ?: 0 > 100 ->
                Result.failure(IllegalArgumentException("Settore troppo lungo (max 100 caratteri)"))

            client.website?.isNotBlank() == true && !isValidWebsite(client.website) ->
                Result.failure(IllegalArgumentException("Formato website non valido"))

            else -> Result.success(Unit)
        }
    }

    /**
     * Controllo univocità ragione sociale escludendo il cliente corrente
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
     * Controllo univocità partita IVA escludendo il cliente corrente
     */
    private suspend fun checkVatNumberUniqueness(clientId: String, vatNumber: String): Result<Unit> {
        return clientRepository.isVatNumberTaken(vatNumber, clientId)
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

    /**
     * Validazione formato website
     */
    private fun isValidWebsite(website: String): Boolean {
        return try {
            val cleanWebsite = if (!website.startsWith("http")) "https://$website" else website
            URL(cleanWebsite)
            true
        } catch (e: Exception) {
            false
        }
    }
}