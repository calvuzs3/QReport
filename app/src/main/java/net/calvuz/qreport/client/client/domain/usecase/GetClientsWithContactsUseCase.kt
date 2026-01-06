package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import javax.inject.Inject

class GetClientsWithContactsUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {

    /**
     * Recupera clienti con contatti associati
     *
     * @return Result con lista clienti che hanno contatti
     */
    suspend operator fun invoke(): Result<List<Client>> {
        return try {
            clientRepository.getClientsWithContacts()
                .map { clients -> clients.sortedBy { it.companyName.lowercase() } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}