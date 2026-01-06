package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import javax.inject.Inject

class CheckClientExistsUseCase @Inject constructor(
    val clientRepository: ClientRepository
){

    /**
     * Verifica che il cliente esista
     */
    suspend operator fun invoke(clientId: String): Result<Unit> {
        return clientRepository.getClientById(clientId)
            .mapCatching { client ->
                if (client == null) {
                    throw NoSuchElementException("Cliente '$clientId' non trovato")
                }
            }
    }
}