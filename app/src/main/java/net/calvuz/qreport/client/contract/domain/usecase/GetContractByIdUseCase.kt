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

    suspend operator fun invoke(contractId: String): QrResult<Contract, QrError> {
        return try {
            if (contractId.isBlank()) {
                return QrResult.Error(QrError.Contracts.ContractIdEmpty(null))
            }

            when (val result = repo.getContractById(contractId)) {
                is QrResult.Success -> {
                    if (result.data != null) {
                        QrResult.Success(result.data)
                    } else {
                        Timber.w("Contatto non trovato {id=$contractId}")
                        QrResult.Error(QrError.Contracts.ContractNotFound("Contatto non trovato"))
                    }

                }
                is QrResult.Error -> {
                    return QrResult.Error(result.error)
                }

            }
        } catch (e: Exception) {
            Timber.e(e, "Errore in getContractById {id=$contractId}")
            QrResult.Error(QrError.Contracts.ContractNotFound(e.message))
        }
    }
}