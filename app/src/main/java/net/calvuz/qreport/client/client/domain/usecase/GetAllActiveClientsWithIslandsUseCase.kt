package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import javax.inject.Inject

class GetAllActiveClientsWithIslandsUseCase @Inject constructor(
    val clientRepository: ClientRepository
){
    /**
     * Recupera clienti con isole associate
     *
     * @return Result con lista clienti che hanno isole robotizzate
     */
    suspend operator fun invoke(): Result<List<Client>> {
        return try {
            clientRepository.getClientsWithIslands()
                .map { clients -> clients.sortedBy { it.companyName.lowercase() } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}