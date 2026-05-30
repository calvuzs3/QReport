package net.calvuz.qreport.client.contract.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contract.data.local.mapper.isValid
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.model.ContractStatistics
import javax.inject.Inject

class NewGetContractStatisticsUseCase @Inject constructor(
    private val getContractsByClient: GetContractsByClientUseCase
) {
    suspend operator fun invoke(clientId: String): QrResult<ContractStatistics, QrError> {
        return when (val result = getContractsByClient(clientId)) {
            is QrResult.Error -> QrResult.Error(result.error)
            is QrResult.Success -> {
                val contracts = result.data
                if (contracts.isEmpty()) {
                    QrResult.Success(ContractStatistics.empty())
                } else {
                    QrResult.Success(calculateStatistics(contracts))
                }
            }
        }
    }

    private fun calculateStatistics(contracts: List<Contract>): ContractStatistics {
        val active = contracts.filter { it.isValid() }
        return ContractStatistics(
            totalContracts = contracts.size,
            activeContracts = active.size,
            inactiveContracts = contracts.size - active.size
        )
    }
}