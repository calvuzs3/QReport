package net.calvuz.qreport.client.client.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Reactive Flow of clients, sorted alphabetically.
 *
 * Flow use cases do not wrap in QrResult — errors propagate via Flow.catch
 * in the ViewModel, which is the established pattern for flows in this project.
 *
 * @param activeOnly if true (default) observes only active (non-deleted) clients
 */
class ObserveClientsUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {
    operator fun invoke(activeOnly: Boolean = false): Flow<List<Client>> {

        Timber.v("Observing clients activeOnly=$activeOnly)")

        val flow = if (activeOnly) {
            clientRepository.getActiveClientsFlow()
        } else {
            clientRepository.getClientsFlow()
        }
        return flow.map { clients -> clients.sortedBy { it.companyName.lowercase() } }
    }
}