package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import net.calvuz.qreport.client.client.domain.validator.ClientDataValidator
import javax.inject.Inject

/**
 * Creates a new client after validating data and checking name uniqueness.
 */
class CreateClientUseCase @Inject constructor(
    private val clientRepository: ClientRepository,
    private val validateClientData: ClientDataValidator,
    private val checkCompanyNameUniqueness: CheckCompanyNameUniquenessUseCase
) {
    suspend operator fun invoke(client: Client): QrResult<Unit, QrError.ClientError> {

        // Validate required fields
        when (val valid = validateClientData(client)) {
            is QrResult.Error -> return valid
            is QrResult.Success -> Unit
        }

        // Check name uniqueness
        when (val unique = checkCompanyNameUniqueness(client)) {
            is QrResult.Error -> return unique
            is QrResult.Success -> Unit
        }

        // Create client
        return clientRepository.createClient(client).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.ClientError.CreateError(it.message)) }
        )
    }
}