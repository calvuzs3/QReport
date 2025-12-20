package net.calvuz.qreport.domain.usecase.client.client

import net.calvuz.qreport.domain.model.client.Client
import net.calvuz.qreport.domain.repository.ClientRepository
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