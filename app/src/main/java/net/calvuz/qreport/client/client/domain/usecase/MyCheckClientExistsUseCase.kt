package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import javax.inject.Inject


class MyCheckClientExistsUseCase @Inject constructor(
    val clientRepository: ClientRepository
) {


    suspend operator fun invoke(clientId: String): QrResult<Boolean, QrError> {
        if (clientId.isBlank()) {
            return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
        }

        return clientRepository.getClientById(clientId).fold(
            onSuccess = { client ->
                QrResult.Success(client != null && client.isActive)
            },
            onFailure = {
                QrResult.Error(QrError.DatabaseError.NotFound(clientId))
            }
        )
    }
}