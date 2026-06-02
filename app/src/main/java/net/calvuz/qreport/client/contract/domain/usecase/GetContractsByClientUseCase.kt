package net.calvuz.qreport.client.contract.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.usecase.CheckClientExistsUseCase
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.repository.ContractRepository
import timber.log.Timber
import javax.inject.Inject

class GetContractsByClientUseCase @Inject constructor(
    private val contractRepository: ContractRepository,
    private val checkClientExists: CheckClientExistsUseCase
) {
    suspend operator fun invoke(clientId: String): QrResult<List<Contract>, QrError.ContractsError> {
        return try {

            Timber.d("Get contracts by client id")

            // Check input
            if (clientId.isBlank()) {
                Timber.d("Client id is blank")
                return QrResult.Error(QrError.ContractsError.MissingClientId())
            }

            // Check client exists
            when (checkClientExists(clientId)) {
                is QrResult.Error -> {
                    Timber.d("Client not found: $clientId")
                    return QrResult.Error(QrError.ContractsError.ClientNotFound())
                }
                is QrResult.Success -> Unit
            }

            // Fetch contracts
            when (val result = contractRepository.getContractsByClient(clientId)) {
                is QrResult.Error -> {
                    Timber.d("Get contracts by client error: r${result.error}")
                    QrResult.Error(QrError.ContractsError.NotFound())
                }
                is QrResult.Success -> {
                    Timber.d("Get contracts by client successful: ${result.data.count()}")
                    QrResult.Success(result.data)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            QrResult.Error(QrError.ContractsError.NotFound(e.message))
        }
    }

    fun observeContractsByClient(clientId: String): Flow<List<Contract>> =
        contractRepository.getContractsByClientFlow(clientId)
}