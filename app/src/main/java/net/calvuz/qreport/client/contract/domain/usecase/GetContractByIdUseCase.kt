package net.calvuz.qreport.client.contract.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.repository.ContractRepository
import timber.log.Timber
import javax.inject.Inject

class GetContractByIdUseCase @Inject constructor(
    private val repo: ContractRepository
) {

    suspend operator fun invoke(contractId: String): QrResult<Contract, QrError.ContractsError> {
        return try {

            Timber.d("Get a contact by id")

            // Check input
            if (contractId.isBlank()) {
                Timber.d("Contact id is blank")
                return QrResult.Error(QrError.ContractsError.MissingContractId())
            }

            // Get
            when (val result = repo.getContractById(contractId)) {
                is QrResult.Success -> {
                    if (result.data != null) {
                        QrResult.Success(result.data)
                    } else {
                        Timber.d("Contract not found: $contractId")
                        QrResult.Error(QrError.ContractsError.NotFound())
                    }

                }
                is QrResult.Error -> {
                    Timber.d("Get contract error: ${result.error}")
                    return QrResult.Error(QrError.ContractsError.LoadError())
                }

            }
        } catch (e: Exception) {
            Timber.e(e)
            QrResult.Error(QrError.ContractsError.NotFound())
        }
    }
}