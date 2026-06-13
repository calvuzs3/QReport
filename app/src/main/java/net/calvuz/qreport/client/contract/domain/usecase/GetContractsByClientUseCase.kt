@file:Suppress("HardCodedStringLiteral")

package net.calvuz.qreport.client.contract.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.repository.ContractRepository
import timber.log.Timber
import javax.inject.Inject

class GetContractsByClientUseCase @Inject constructor(
    private val contractRepository: ContractRepository,
) {
    suspend operator fun invoke(clientId: String): QrResult<List<Contract>, QrError.ContractsError> {
        return try {

            Timber.v("Get contracts for client $clientId")

            // Check input
            if (clientId.isBlank()) {
                Timber.d("Client id is blank")
                return QrResult.Error(QrError.ContractsError.MissingClientId())
            }

            // Get
            when (val result = contractRepository.getContractsByClient(clientId)) {
                is QrResult.Success -> {
                    Timber.d("Successfully retrieved contracts for client $clientId: : ${result.data.size}")
                    QrResult.Success(result.data)
                }
                is QrResult.Error -> {
                    Timber.d("Failed getting contracts for client $clientId:: r${result.error}")
                    QrResult.Error(QrError.ContractsError.NotFound())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get contracts for client $clientId")
            QrResult.Error(QrError.ContractsError.NotFound(e.message))
        }
    }

    suspend fun getActive(clientId: String): QrResult<List<Contract>, QrError.ContractsError> {
        return try {
            Timber.v("Get active contracts for client $clientId")

            if (clientId.isBlank()) {
                Timber.d("Client id is blank")
                return QrResult.Error(QrError.ContractsError.MissingClientId())
            }
            when (val result = contractRepository.getActiveContractsByClient(clientId)) {
                is QrResult.Success -> {
                    Timber.d("Successfully retrieved active contracts for client $clientId: ${result.data.count()} ")
                    QrResult.Success(result.data)
                }
                is QrResult.Error -> {
                    Timber.d("Failed to get active contracts for client $clientId {r${result.error}}")
                    QrResult.Error(QrError.ContractsError.NotFound())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get active contracts for client $clientId")
            QrResult.Error(QrError.ContractsError.NotFound(e.message))
        }
    }
}