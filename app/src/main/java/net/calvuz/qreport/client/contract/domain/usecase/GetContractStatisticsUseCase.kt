package net.calvuz.qreport.client.contract.domain.usecase

import net.calvuz.qreport.client.contract.data.local.isValid
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.model.ContractStatistics
import net.calvuz.qreport.app.result.domain.QrResult
import javax.inject.Inject

class GetContractStatisticsUseCase @Inject constructor(
    private val getContractsByClientUseCase: GetContractsByClientUseCase
) {

    suspend operator fun invoke(clientId: String): Result<ContractStatistics> = try {

        // Recupera tutti i contatti del cliente usando il use case specifico
        when (val result = getContractsByClientUseCase(clientId)) {

            is QrResult.Error -> {
                Result.failure(Exception("Errore nel recupero dei contatti"))
            }
            is QrResult.Success -> {
                val contracts = result.data
                if (contracts.isEmpty()) {
                    Result.success(ContractStatistics.empty())
                } else {
                    val statistics = calculateStatistics(contracts)
                    Result.success(statistics)
                }
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun calculateStatistics(contracts: List<Contract>): ContractStatistics {
        val activeContracts = contracts.filter { it.isValid() }
        val inactiveContracts = contracts.filter { !it.isValid() }

        return ContractStatistics(
            totalContracts = contracts.size,
            activeContracts = activeContracts.size,
            inactiveContracts = inactiveContracts.size,
        )
    }
}