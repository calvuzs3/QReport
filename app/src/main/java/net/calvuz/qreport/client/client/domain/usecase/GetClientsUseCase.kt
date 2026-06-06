package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Returns a sorted list of clients.
 *
 * Filtering is done at DB level for efficiency.
 *
 * @param activeOnly if true (default) returns only active (non-deleted) clients
 */
class GetClientsUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {
    suspend operator fun invoke(
        activeOnly: Boolean = true
    ): QrResult<List<Client>, QrError.ClientError> {

        Timber.d("Getting clients")

        val result = if (activeOnly) {
            clientRepository.getActiveClients()
        } else {
            clientRepository.getClients()
        }

        return result.fold(
            onSuccess = { clients ->
                Timber.d("Retrieved ${clients.size} clients")
                QrResult.Success(clients.sortedBy { it.companyName.lowercase() })
            },
            onFailure = {
                Timber.d( "Failed to get clients: ${it.message}")
                QrResult.Error(QrError.ClientError.LoadError(it.message))
            }
        )
    }
}