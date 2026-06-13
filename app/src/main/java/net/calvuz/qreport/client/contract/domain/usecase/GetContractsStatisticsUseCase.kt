package net.calvuz.qreport.client.contract.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contract.data.local.mapper.isValid
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.model.ContractStatistics
import timber.log.Timber
import javax.inject.Inject

class GetContractStatisticsUseCase @Inject constructor(
    private val getContractsByClient: GetContractsByClientUseCase
) {
    
    @Suppress("HardCodedStringLiteral")
    suspend operator fun invoke(clientId: String): QrResult<ContractStatistics, QrError.ContractsError> {
        return try {
            Timber.v("Calculating contract statistics for client: $clientId")

            // Check input
            if (clientId.isBlank()) {
                Timber.w("ClientId is blank")
                return QrResult.Error(QrError.ContractsError.MissingClientId())
            }

            when (val result = getContractsByClient(clientId)) {
                is QrResult.Success -> {
                    Timber.d("GSuccessfully retrieved contracts for client $clientId: ${result.data.size}")
                    QrResult.Success(calculateStatistics(result.data))
                }

                is QrResult.Error -> {
                    Timber.d("Error retrieving contracts for client $clientId: ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception calculating statistics for client: $clientId")
            QrResult.Error(QrError.ContractsError.NotFound())
        }
    }

    private fun calculateStatistics(contracts: List<Contract>): ContractStatistics {
        val active = contracts.filter { it.isValid() }
        return ContractStatistics(
            totalContracts = contracts.size,
            validContracts = active.size,
            outdatedContracts = contracts.size - active.size
        )
    }
}