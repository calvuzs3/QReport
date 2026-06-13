package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Returns a single [Client] by ID.
 *
 * Returns [QrError.ClientError.NotFound] if the client does not exist.
 */
class GetClientByIdUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {
    
    @Suppress("HardCodedStringLiteral")
    suspend operator fun invoke(
        clientId: String
    ): QrResult<Client, QrError.ClientError> {

        Timber.v("Getting client $clientId")

        // CHeck input
        if (clientId.isBlank()) {
            Timber.d("Client ID is blank")
            return QrResult.Error(QrError.ClientError.MissingCompanyName())
        }

        // Get
        return clientRepository.getClientById(clientId).fold(onSuccess = { client ->
            if (client != null) {
                Timber.d("Got $client")
                QrResult.Success(client)
            } else {
                Timber.d("Client $clientId not found")
                QrResult.Error(QrError.ClientError.NotFound())
            }
        }, onFailure = {
            Timber.e(it, "Failed to get client $clientId")
            QrResult.Error(QrError.ClientError.LoadError(it.message))
        })
    }
}