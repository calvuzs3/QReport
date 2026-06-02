package net.calvuz.qreport.client.contract.domain.usecase

import net.calvuz.qreport.client.contract.domain.repository.ContractRepository
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import timber.log.Timber
import javax.inject.Inject

class DeleteContractUseCase @Inject constructor(
    private val contractRepository: ContractRepository,
    private val checkContractExists: CheckContractExistsUseCase

) {

    suspend operator fun invoke(contractId: String): QrResult<Int, QrError> {
            return try {

                Timber.d("Delete contract")

                // Check input
                if (contractId.isBlank()) {
                    Timber.d("Contract id is blank")
                    return QrResult.Error(QrError.ContractsError.MissingContractId())
                }

                // Check contract exists
                return when (val contact = checkContractExists(contractId)) {
                    is QrResult.Success -> {
                        return when (val result =contractRepository.deleteContractById(contractId)) {
                            is QrResult.Success -> {
                                Timber.d("Contract deleted successfully: ${contact.data}")
                                QrResult.Success(result.data)
                            }
                            is QrResult.Error -> {
                                Timber.d("Contract delete error: ${result.error}")
                                QrResult.Error(result.error)
                            }
                        }
                    }
                    is QrResult.Error -> {
                        Timber.d("Contract delete error: ${contact.error}")
                        QrResult.Error(QrError.ContractsError.DeleteError())
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
                QrResult.Error(QrError.ContractsError.DeleteError())
            }
    }


}