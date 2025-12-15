package net.calvuz.qreport.domain.usecase.client.client

import net.calvuz.qreport.domain.model.client.Client
import net.calvuz.qreport.domain.repository.ClientRepository
import net.calvuz.qreport.domain.validator.ClientDataValidator
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
    private val clientRepository: ClientRepository,
    private val validateClientData: ClientDataValidator,
    private val checkCompanyNameUniqueness: CheckCompanyNameUniquenessUseCase,
    private val checkVatNumberUniqueness: CheckVatNumberUniquenessUseCase

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
                checkVatNumberUniqueness(client.id, vatNumber).onFailure { return Result.failure(it) }
            }

            // 4. Creazione nel repository
            clientRepository.createClient(client)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}