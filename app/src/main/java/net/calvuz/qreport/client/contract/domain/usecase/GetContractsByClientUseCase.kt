package net.calvuz.qreport.client.contract.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.client.domain.usecase.CheckClientExistsUseCase
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.repository.ContractRepository
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import timber.log.Timber
import javax.inject.Inject

class GetContractsByClientUseCase @Inject constructor(
    private val contractRepository: ContractRepository,
    private val checkClientExists: CheckClientExistsUseCase

) {

    suspend operator fun invoke(clientId: String): QrResult<List<Contract>, QrError> {
        return try {
            // 1. Validazione input
            if (clientId.isBlank()) {
                return QrResult.Error(QrError.Contracts.ClientIdEmpty("ID cliente non può essere vuoto")) //("ID cliente non può essere vuoto"))
            }

            // 2. Verifica esistenza cliente
            checkClientExists(clientId).onFailure {
                return QrResult.Error(
                    QrError.Contracts.ClientNotFound(
                        it.message
                    )
                )
            }

            // 3. Recupero contatti ordinati
            when (val result = contractRepository.getContractsByClient(clientId)) {

                is QrResult.Success -> {
                    return QrResult.Success(result.data)
                }

                is QrResult.Error -> {
                    return QrResult.Error (QrError.Contracts.ContractNotFound(result.error.toString()))
                }
            }


        } catch (e: Exception) {
            Timber.e(e, "Error in get contacts")
            QrResult.Error(QrError.Contracts.ContractNotFound(e.message))
        }
    }

    fun observeContractsByClient(clientId: String): Flow<List<Contract>> {
        return contractRepository.getContractsByClientFlow(clientId)
    }
}