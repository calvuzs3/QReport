package net.calvuz.qreport.client.contract.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.usecase.CheckClientExistsUseCase
import net.calvuz.qreport.client.contract.domain.repository.ContractRepository
import timber.log.Timber
import javax.inject.Inject

class GetContractsCountByClientUseCase @Inject constructor(
    private val repository: ContractRepository,
    private val checkClientExists: CheckClientExistsUseCase
) {

    suspend operator fun invoke(clientId: String): QrResult<Int, QrError> {
        return try {
            // 1. Input validation
            if (clientId.isBlank()) {
                Timber.w("ClientId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(clientId))
            }

            // 2. Check client exists
            when (val clientCheck = checkClientExists(clientId)) {
                is QrResult.Error -> {
                    Timber.w("Client does not exist: $clientId")
                    return QrResult.Error(clientCheck.error)
                }
                is QrResult.Success -> {
                    // Client exists, continue
                }
            }

            when (val result = repository.getContractsCountByClient(clientId)) {
                is QrResult.Success -> {
                    val count = result.data
                    Timber.d("Retrieved $count contacts for client: $clientId")
                    return QrResult.Success(count)
                }
                is QrResult.Error -> {
                    Timber.e("Repository error for clientId $clientId: ${result.error}")
                    return QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception getting contacts for client: $clientId")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }
}