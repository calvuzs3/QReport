package net.calvuz.qreport.domain.usecase.client.client

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.domain.model.client.Client
import net.calvuz.qreport.domain.repository.ClientRepository
import javax.inject.Inject

class ObserveAllClientsUseCase @Inject constructor(
    val clientRepository: ClientRepository
) {

    /**
     * Osserva tutti i clienti attivi (Flow reattivo)
     *
     * @return Flow con lista clienti che si aggiorna automaticamente
     */
    operator fun invoke(): Flow<List<Client>> {
        return clientRepository.getAllClientsFlow()
            .map { clients -> clients.sortedBy { it.companyName.lowercase() } }
    }
}