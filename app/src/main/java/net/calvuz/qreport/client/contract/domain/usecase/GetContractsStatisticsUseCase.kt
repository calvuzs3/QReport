package net.calvuz.qreport.client.contract.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.error.presentation.toUiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contract.data.local.mapper.isValid
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.model.ContractStatistics
import timber.log.Timber
import javax.inject.Inject

class GetContractStatisticsUseCase @Inject constructor(
    private val getContractsByClient: GetContractsByClientUseCase
) {
    suspend operator fun invoke(clientId: String): QrResult<ContractStatistics, QrError.ContractsError> {

        Timber.d("Get contracts statistics")

        return when (val result = getContractsByClient(clientId)) {
            is QrResult.Error -> {
                Timber.d("Error in getting statistics: ${result.error}")
                QrResult.Error(result.error)
            }
            is QrResult.Success -> {
                Timber.d("Get statistics successfully: ${result.data.count()}")
                val contracts = result.data
                if (contracts.isEmpty()) {
                    Timber.d("Stats: empty")
                    QrResult.Success(ContractStatistics.empty())
                } else {
                    Timber.d("Stats: ${calculateStatistics(contracts)}")
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