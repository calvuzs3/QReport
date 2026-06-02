package net.calvuz.qreport.client.contract.domain.usecase

import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.repository.ContractRepository
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import timber.log.Timber
import javax.inject.Inject

class CheckContractExistsUseCase @Inject constructor(
    private val contractRepository: ContractRepository
) {
    suspend operator fun invoke(contractId: String): QrResult<Contract, QrError> {
        return try {

            Timber.d("Check contract exists")

            // Check input
            if (contractId.isBlank()) {
                return QrResult.Error(QrError.ContractsError.MissingClientId())
            }

            // Get
            when (val result = contractRepository.getContractById(contractId)) {
                is QrResult.Success -> {
                    if (result.data == null) {
                        Timber.d("Contract found: ${result.data}")
                        return QrResult.Error(QrError.ContractsError.NotFound())
                    }
                    QrResult.Success(result.data)
                }
                is QrResult.Error -> {
                    Timber.d("Contract not found: ${result.error}")
                    return QrResult.Error(QrError.ContractsError.NotFound())
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            QrResult.Error(QrError.ContractsError.NotFound())
        }
    }
}