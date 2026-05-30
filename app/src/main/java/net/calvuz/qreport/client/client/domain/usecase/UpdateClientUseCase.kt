package net.calvuz.qreport.client.client.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import net.calvuz.qreport.client.client.domain.validator.ClientDataValidator
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
        // Verify client exists
        when (val exists = checkClientExists(client.id)) {
            is QrResult.Error -> return QrResult.Error(exists.error)
            is QrResult.Success -> Unit
        }

        // Validate fields
        validateClientData(client).onFailure {
            return QrResult.Error(QrError.ClientError.MissingCompanyName(it.message))
        }

        // Check name uniqueness (excluding this client)
        when (val unique = checkCompanyNameUniqueness(client)) {
            is QrResult.Error -> return unique
            is QrResult.Success -> Unit
        }

        val updated = client.copy(updatedAt = Clock.System.now())
        return clientRepository.updateClient(updated).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.ClientError.UpdateError(it.message)) }
        )
    }
}