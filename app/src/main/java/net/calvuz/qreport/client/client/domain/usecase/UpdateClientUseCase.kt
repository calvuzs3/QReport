package net.calvuz.qreport.client.client.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import net.calvuz.qreport.client.client.domain.validator.ClientDataValidator
import timber.log.Timber
import javax.inject.Inject

/**
 * Updates an existing client, refreshing its [Client.updatedAt] timestamp.
 */
class UpdateClientUseCase @Inject constructor(
    private val clientRepository: ClientRepository,
    private val checkClientExists: CheckClientExistsUseCase,
    private val checkCompanyNameUniqueness: CheckCompanyNameUniquenessUseCase,
    private val validateClientData: ClientDataValidator
) {
    suspend operator fun invoke(client: Client): QrResult<Unit, QrError.ClientError> {

        Timber.d("Updating client: $client")

        // Verify client exists
        when (val exists = checkClientExists(client.id)) {
            is QrResult.Error -> return QrResult.Error(exists.error)
            is QrResult.Success -> Unit
        }

        // Validate fields
        when (val valid = validateClientData(client)) {
            is QrResult.Error -> return valid
            is QrResult.Success -> Unit
        }

        // Check name uniqueness (excluding this client)
        when (val unique = checkCompanyNameUniqueness(client)) {
            is QrResult.Error -> return unique
            is QrResult.Success -> Unit
        }

        // Update client
        val updated = client.copy(updatedAt = Clock.System.now())
        return clientRepository.updateClient(updated).fold(
            onSuccess = {
                Timber.d("Client updated: $updated")
                QrResult.Success(Unit) },
            onFailure = {
                Timber.d( "Client update failed: ${it.message}")
                QrResult.Error(QrError.ClientError.UpdateError(it.message)) }
        )
    }
}