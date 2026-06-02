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
    suspend operator fun invoke(contract: Contract): QrResult<String, QrError> {
        return try {

            Timber.d("Create contract")

            // Check client exist
            when (checkClientExists(contract.clientId)) {
                is QrResult.Error -> {
                    Timber.d("Client not found ${contract.clientId}")
                    return QrResult.Error(QrError.ContractsError.ClientNotFound())
                }
                is QrResult.Success -> Unit
            }

            // Create
            when (val result = repository.createContract(contract)) {
                is QrResult.Error -> {
                    Timber.d("Create contract error: ${result.error}")
                    QrResult.Error(QrError.DatabaseError.InsertFailed(null))
                }
                is QrResult.Success -> {
                    Timber.d("Contract created successfully: ${result.data}")
                    QrResult.Success(result.data)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            QrResult.Error(QrError.DatabaseError.InsertFailed(null))
        }
    }
}