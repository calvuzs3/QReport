package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import javax.inject.Inject

/**
 * Returns a single [Client] by ID.
 *
 * Returns [QrError.ClientError.NotFound] if the client does not exist.
 */
class GetClientByIdUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {
    suspend operator fun invoke(
        clientId: String
    ): QrResult<Client, QrError.ClientError> {

        // CHeck input
        if (clientId.isBlank()) {
            return QrResult.Error(QrError.ClientError.MissingCompanyName())
        }

        // Get
        return clientRepository.getClientById(clientId).fold(
            onSuccess = { client ->
                if (client != null) QrResult.Success(client)
                else QrResult.Error(QrError.ClientError.NotFound())
            },
            onFailure = {
                QrResult.Error(QrError.ClientError.LoadError(it.message))
            }
        )
    }
}