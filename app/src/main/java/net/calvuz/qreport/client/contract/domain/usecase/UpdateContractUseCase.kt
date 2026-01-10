package net.calvuz.qreport.client.contract.domain.usecase

import kotlinx.datetime.Instant
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.repository.ContractRepository
import javax.inject.Inject

class UpdateContractUseCase @Inject constructor(
    private val checkContractExists: CheckContractExists,
    private val repository: ContractRepository
) {

    suspend operator fun invoke(contract: Contract): QrResult<String, QrError> {

        // 1. Validation
        when (val result = checkContractExists(contract.id)) {
            is QrResult.Success -> result.data
            is QrResult.Error -> return QrResult.Error(result.error)
        }

        // 2. Update
        val finalContract = contract.copy(
            updatedAt = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        )
        return when (val result = repository.updateContract(finalContract)) {
            is QrResult.Success -> QrResult.Success(result.data)
            is QrResult.Error -> QrResult.Error(QrError.DatabaseError.UpdateFailed(null))
        }
    }
}