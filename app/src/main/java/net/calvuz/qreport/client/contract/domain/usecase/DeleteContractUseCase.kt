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

                if (contractId.isBlank()) {
                    return QrResult.Error(QrError.Contracts.ClientIdEmpty("ID cliente non può essere vuoto"))
                }

                // 2. Verificare che il contatto esista
                return when (val contact = checkContractExists(contractId)) {
                    is QrResult.Success -> {
                        // Il contatto esiste, procedi con l'eliminazione
                        return when (val result =contractRepository.deleteContractById(contractId)) {
                            is QrResult.Success -> {
                                QrResult.Success(result.data)
                            }
                            is QrResult.Error -> {
                                QrResult.Error(result.error)
                            }
                        }
                    }
                    is QrResult.Error -> {
                        QrResult.Error(contact.error)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in delete contact")
                QrResult.Error(QrError.Contracts.DeleteError(e))
            }
    }


}