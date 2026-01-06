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

            if (contractId.isBlank()) {
                return QrResult.Error(QrError.Contracts.ClientIdEmpty("ID cliente non può essere vuoto"))
            }

            when (val result = contractRepository.getContractById(contractId)) {
                is QrResult.Success -> {
                    if (result.data == null) {
                        throw NoSuchElementException("Contatto con ID '$contractId' non trovato")
                    }
                    QrResult.Success(result.data)
                }
                is QrResult.Error -> {
                    throw NoSuchElementException("Contatto con ID '$contractId' non trovato")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in check contract exists")
            QrResult.Error(QrError.Contracts.ContractNotFound(e.message))
        }
    }
}