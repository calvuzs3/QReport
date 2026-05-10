package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import javax.inject.Inject

class GetClientByIdUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {

    /**
     * Get a Client by ID
     *
     * @param clientId Client ID
     * @return Client
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