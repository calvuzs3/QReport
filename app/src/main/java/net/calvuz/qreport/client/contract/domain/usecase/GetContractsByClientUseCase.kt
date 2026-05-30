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
    suspend operator fun invoke(clientId: String): QrResult<List<Contract>, QrError> {
        return try {
            if (clientId.isBlank()) {
                return QrResult.Error(QrError.Contracts.ClientIdEmpty("ID cliente non può essere vuoto"))
            }

            // Verify client exists
            when (checkClientExists(clientId)) {
                is QrResult.Error -> return QrResult.Error(QrError.Contracts.ClientNotFound(clientId))
                is QrResult.Success -> Unit
            }

            // Fetch contracts
            when (val result = contractRepository.getContractsByClient(clientId)) {
                is QrResult.Success -> QrResult.Success(result.data)
                is QrResult.Error -> QrResult.Error(QrError.Contracts.ContractNotFound(result.error.toString()))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting contracts for client $clientId")
            QrResult.Error(QrError.Contracts.ContractNotFound(e.message))
        }
    }

    fun observeContractsByClient(clientId: String): Flow<List<Contract>> =
        contractRepository.getContractsByClientFlow(clientId)
}