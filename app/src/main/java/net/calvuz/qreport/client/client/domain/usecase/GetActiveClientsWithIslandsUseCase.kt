package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Returns active clients that have at least one Islands associated.
 */
class GetActiveClientsWithIslandsUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {
    suspend operator fun invoke(): QrResult<List<Client>, QrError.ClientError> {

        Timber.v("Getting active clients with islands")

        return clientRepository.getActiveClientsWithIslands().fold(onSuccess = { clients ->
            Timber.d("Active clients with islands: ${clients.size}")
            QrResult.Success(clients.sortedBy { it.companyName.lowercase() })
        }, onFailure = {
            Timber.e(it, "Error getting active clients with islands")
            QrResult.Error(QrError.ClientError.LoadError(it.message))
        })
    }
}