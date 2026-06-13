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

    @Suppress("HardCodedStringLiteral")
    suspend operator fun invoke(contractId: String): QrResult<Unit, QrError> {
            return try {

                Timber.v("Delete contract $contractId")

                // Check input
                if (contractId.isBlank()) {
                    Timber.d("Contract id is blank")
                    return QrResult.Error(QrError.ContractsError.MissingContractId())
                }

                // Check contract exists
                return when (val contract = checkContractExists(contractId)) {
                    is QrResult.Success -> {
                        contractRepository.deactivateContract(contractId).fold(
                            {
                                Timber.d("Successfully deleted contract $contractId")
                                QrResult.Success(it)
                            },
                            {
                                Timber.d("Contract delete error: $it")
                                QrResult.Error(QrError.ContractsError.DeleteError())
                            }
                        )
                    }
                    is QrResult.Error -> {
                        Timber.d("Contract delete error: ${contract.error}")
                        QrResult.Error(QrError.ContractsError.DeleteError())
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Contract delete error")
                QrResult.Error(QrError.ContractsError.DeleteError())
            }
    }


}