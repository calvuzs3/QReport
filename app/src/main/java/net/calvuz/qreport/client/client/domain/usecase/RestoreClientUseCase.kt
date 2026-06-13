package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Activate a client.
 */
class RestoreClientUseCase @Inject constructor(
    private val clientRepository: ClientRepository,
    private val checkClientExists: CheckClientExistsUseCase,
) {
    /**
     * Activate a client.
     *
     * @param clientId  ID of the client to activate
     */
    suspend operator fun invoke(
        clientId: String,
    ): QrResult<Unit, QrError.ClientError> {

        Timber.v("Activating client $clientId")

        // Check input
        if (clientId.isBlank()) {
            Timber.d("Client ID is blank")
            return QrResult.Error(QrError.ClientError.NotFound())
        }

        // Verify client exists
        when (val exists = checkClientExists(clientId)) {
            is QrResult.Error -> return QrResult.Error(exists.error)
            is QrResult.Success -> Unit
        }

        // Restore the client itself
        return clientRepository.restoreClient(clientId).fold(onSuccess = {
            Timber.d("Client $clientId successfully activated")
            QrResult.Success(Unit)
        }, onFailure = {
            Timber.d("Failed to activate client: ${it.message}")
            QrResult.Error(QrError.ClientError.DeleteError(it.message))
        })
    }
}