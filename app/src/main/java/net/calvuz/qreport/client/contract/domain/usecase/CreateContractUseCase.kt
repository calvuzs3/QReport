package net.calvuz.qreport.client.contract.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.usecase.CheckClientExistsUseCase
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.repository.ContractRepository
import timber.log.Timber
import javax.inject.Inject

class CreateContractUseCase @Inject constructor(
    private val checkClientExists: CheckClientExistsUseCase,

    private val repository: ContractRepository
) {

    suspend operator fun invoke( contract : Contract) : QrResult<String, QrError> {
        return try {

            // 1. Validation
            checkClientExists(contract.clientId).onFailure { return QrResult.Error(QrError.Contracts.ClientNotFound(contract.clientId)) }

            // 2. Create
             when (val result = repository.createContract(contract)) {
                 is QrResult.Error -> QrResult.Error(QrError.DatabaseError.InsertFailed(null))
                 is QrResult.Success -> QrResult.Success(result.data)
             }
        } catch (e: Exception) {
            Timber.e(e, "Error creating contract")
            QrResult.Error(QrError.DatabaseError.InsertFailed(null))
        }
    }
}