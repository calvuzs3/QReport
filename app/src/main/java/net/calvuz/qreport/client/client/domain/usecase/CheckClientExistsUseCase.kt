package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import javax.inject.Inject

/**
 * Checks whether a client exists and is active.
 *
 * Returns [QrResult.Success(true)] if the client exists and is active.
 * Returns [QrError.ClientError.NotFound] if not found or inactive.
 * Returns [QrError.ClientError.LoadError] on DB failure.
 */
class CheckClientExistsUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {
    suspend operator fun invoke(clientId: String): QrResult<Boolean, QrError.ClientError> {
        if (clientId.isBlank()) {
            return QrResult.Error(QrError.ClientError.NotFound())
        }

        return clientRepository.getClientById(clientId).fold(
            onSuccess = { client ->
                if (client != null && client.isActive) QrResult.Success(true)
                else QrResult.Error(QrError.ClientError.NotFound())
            },
            onFailure = {
                QrResult.Error(QrError.ClientError.LoadError(it.message))
            }
        )
    }
}