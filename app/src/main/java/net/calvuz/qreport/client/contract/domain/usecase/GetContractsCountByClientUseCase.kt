package net.calvuz.qreport.client.contract.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.usecase.CheckClientExistsUseCase
import net.calvuz.qreport.client.contract.domain.repository.ContractRepository
import timber.log.Timber
import javax.inject.Inject

class GetContractsCountByClientUseCase @Inject constructor(
    private val repository: ContractRepository,
    private val checkClientExists: CheckClientExistsUseCase
) {

    suspend operator fun invoke(clientId: String): QrResult<Int, QrError.ContractsError> {
        return try {

            Timber.d("Get contracts count by client")

            // Check input
            if (clientId.isBlank()) {
                Timber.d("Client id is blank")
                return QrResult.Error(QrError.ContractsError.MissingClientId())
            }

            // Check client exists
            when (val clientCheck = checkClientExists(clientId)) {
                is QrResult.Error -> {
                    Timber.d("Client not found: ${clientCheck.error}")
                    return QrResult.Error(QrError.ContractsError.ClientNotFound())
                }
                is QrResult.Success -> Unit
            }

            // Get
            when (val result = repository.getContractsCountByClient(clientId)) {
                is QrResult.Success -> {
                    Timber.d("Successfully retrieved contacts for client:  ${result.data}-$clientId")
                    return QrResult.Success(result.data)
                }
                is QrResult.Error -> {
                    Timber.d("Error in retrieving contacts: ${result.error}")
                    return QrResult.Error(QrError.ContractsError.NotFound())
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            QrResult.Success(0)
        }
    }
}