package net.calvuz.qreport.domain.usecase.client.client

import net.calvuz.qreport.domain.model.client.Client
import net.calvuz.qreport.domain.repository.ClientRepository
import javax.inject.Inject

/**
 * Use Case per recuperare un cliente per ID
 *
 * Gestisce:
 * - Validazione ID input
 * - Recupero dal repository
 * - Gestione cliente non trovato
 */
class GetClientByIdUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {

    /**
     * Recupera un cliente per ID
     *
     * @param clientId ID del cliente da recuperare
     * @return Result con Client se trovato, errore se non trovato o errore di sistema
     */
    suspend operator fun invoke(clientId: String): Result<Client> {
        return try {
            // 1. Validazione ID
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            // 2. Recupero dal repository
            clientRepository.getClientById(clientId)
                .mapCatching { client ->
                    // 3. Gestione cliente non trovato
                    client ?: throw NoSuchElementException("Cliente con ID '$clientId' non trovato")
                }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}