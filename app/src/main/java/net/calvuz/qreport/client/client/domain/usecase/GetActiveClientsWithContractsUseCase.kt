package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Returns active clients that have at least one ContractsError associated.
 */
class GetActiveClientsWithContractsUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {
    suspend operator fun invoke(): QrResult<List<Client>, QrError.ClientError> {

        Timber.d("Getting active clients with contracts")

        return clientRepository.getActiveClientsWithContracts().fold(
            onSuccess = { clients ->
                Timber.d("Active clients with contracts: ${clients.size}")
                QrResult.Success(clients.sortedBy { it.companyName.lowercase() })
            },
            onFailure = {
                Timber.d("Error getting active clients with contracts: ${it.message}")
                QrResult.Error(QrError.ClientError.LoadError(it.message))
            }
        )
    }
}