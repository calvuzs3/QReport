package net.calvuz.qreport.domain.usecase.client.client

import net.calvuz.qreport.domain.model.client.Client
import net.calvuz.qreport.domain.repository.ClientRepository
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