package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Deactivate a client, optionally cascading to its dependencies.
 *
 * @param clientId  ID of the client to delete
 * @param cascade   if true (default) deletes facilities and contacts first;
 *                  if false, fails when dependencies exist
 */
class DeleteClientUseCase @Inject constructor(
    private val clientRepository: ClientRepository,
    private val checkClientExists: CheckClientExistsUseCase,
    private val checkClientDependencies: CheckClientDependenciesUseCase,
) {
    suspend operator fun invoke(
        clientId: String,
        cascade: Boolean = true
    ): QrResult<Unit, QrError.ClientError> {

        Timber.d("Deactivating client $clientId")

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

        // If not cascading, block when dependencies are present
        if (!cascade) {
            when (val dependencies = checkClientDependencies(clientId)) {
                is QrResult.Error -> return dependencies
                is QrResult.Success -> Unit
            }
        }

        // Soft-delete the client itself
        return clientRepository.deactivateClient(clientId).fold(
            onSuccess = {
                Timber.d("Client $clientId successfully deactivated")
                QrResult.Success(Unit) },
            onFailure = {
                Timber.d("Failed to deactivate client: ${it.message}")
                QrResult.Error(QrError.ClientError.DeleteError(it.message)) }
        )
    }
}