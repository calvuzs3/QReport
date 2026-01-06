package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import javax.inject.Inject

class GetClientsWithFacilitiesUseCase @Inject constructor(
    val clientRepository: ClientRepository
) {

    /**
     * Recupera clienti con facilities associate
     *
     * @return Result con lista clienti che hanno stabilimenti
     */
    suspend operator fun invoke(): Result<List<Client>> {
        return try {
            clientRepository.getClientsWithFacilities()
                .map { clients -> clients.sortedBy { it.companyName.lowercase() } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}