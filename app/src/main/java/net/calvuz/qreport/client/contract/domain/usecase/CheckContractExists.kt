package net.calvuz.qreport.client.contract.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.repository.ContractRepository
import javax.inject.Inject

class CheckContractExists @Inject constructor(
    private val getContractById: GetContractByIdUseCase
) {

    suspend operator fun invoke(id: String): QrResult<Contract?, QrError> {
        return when (val result = getContractById(id)) {
            is QrResult.Success -> result.data?.let {
                QrResult.Success(it)
            } ?: QrResult.Error(QrError.Contracts.ContractNotFound(id))

            is QrResult.Error -> result
        }
    }
}