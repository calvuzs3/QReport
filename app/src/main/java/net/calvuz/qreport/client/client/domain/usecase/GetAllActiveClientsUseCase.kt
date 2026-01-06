package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import javax.inject.Inject

class GetAllActiveClientsUseCase @Inject constructor(
    private val clientRepository: ClientRepository
){

    suspend operator fun invoke(): Result<List<Client>> {
        return try {
            clientRepository.getActiveClients()
                .map { clients -> clients.sortedBy { it.companyName.lowercase() } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}