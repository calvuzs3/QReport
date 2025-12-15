package net.calvuz.qreport.domain.usecase.client.client

import kotlinx.datetime.Clock
import net.calvuz.qreport.domain.model.client.Client
import net.calvuz.qreport.domain.repository.ClientRepository
import net.calvuz.qreport.domain.validator.ClientDataValidator
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
    private val clientRepository: ClientRepository,
    private val checkClientExistsUseCase: CheckClientExistsUseCase,
    private val checkCompanyNameUniqueness: CheckCompanyNameUniquenessUseCase,
    private val checkVatNumberUniquenessUseCase: CheckVatNumberUniquenessUseCase,
    private val validateClientData: ClientDataValidator
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
            checkClientExistsUseCase(client.id).onFailure { return Result.failure(it) }

            // 2. Validazione dati aggiornati
            validateClientData(client).onFailure { return Result.failure(it) }

            // 3. Controllo duplicati ragione sociale (escludendo questo cliente)
            checkCompanyNameUniqueness(client).onFailure { return Result.failure(it) }

            // 4. Controllo duplicati partita IVA (se fornita, escludendo questo cliente)
            client.vatNumber?.let { vatNumber ->
                checkVatNumberUniquenessUseCase(client.id, vatNumber).onFailure { return Result.failure(it) }
            }

            // 5. Aggiornamento con timestamp corrente
            val updatedClient = client.copy(updatedAt = Clock.System.now())
            clientRepository.updateClient(updatedClient)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}