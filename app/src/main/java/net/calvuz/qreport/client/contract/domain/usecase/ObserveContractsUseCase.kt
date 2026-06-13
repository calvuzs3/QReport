package net.calvuz.qreport.client.contract.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.repository.ContractRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Reactive Flow of contacts, optionally filtered by client.
 */
class ObserveContractsUseCase @Inject constructor(
    private val repository: ContractRepository
) {
    /**
     * Reactive Flow of contacts, optionally filtered by client.
     *
     * - [clientId] null or blank → all contacts across all clients (active + inactive)
     * - [clientId] provided     → all contacts for that client (active + inactive)
     *
     * Status filtering (ACTIVE / INACTIVE / ALL) is responsibility of the
     * presentation layer (ContactListViewModel.applyFiltersAndSort).
     *
     * Sort order: primary first, then alphabetical by first/last name.
     */
    operator fun invoke(clientId: String? = null): Flow<List<Contract>> {

        Timber.v("Observing contracts clientId=${clientId ?: "all"}")

        val flow = if (clientId.isNullOrBlank()) {
            repository.getContractsFlow()          // all clients, active + inactive
        } else {
            repository.getContractsByClientFlow(clientId)   // one client, active + inactive
        }

        return flow.map { contracts ->
            contracts.sortedWith(
                compareByDescending<Contract> { it.name?.lowercase() }
                    .thenBy { it.hasPriority }
                    .thenBy { it.hasRemoteAssistance }
                    .thenBy { it.hasMaintenance }
            )
        }
    }
}