package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import javax.inject.Inject

/**
 * Checks that the client's company name is not already taken.
 *
 * Returns [QrResult.Success(Unit)] if the name is available.
 * Returns [QrError.ClientError.MissingCompanyName] if blank.
 * Returns [QrError.ClientError.CreateError] if already taken (business rule).
 */
class CheckCompanyNameUniquenessUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {
    suspend operator fun invoke(client: Client): QrResult<Unit, QrError.ClientError> {
        if (client.companyName.isBlank()) {
            return QrResult.Error(QrError.ClientError.MissingCompanyName())
        }

        return clientRepository.isCompanyNameTaken(client.companyName, client.id).fold(
            onSuccess = { isTaken ->
                if (isTaken) QrResult.Error(QrError.ClientError.CreateError())
                else QrResult.Success(Unit)
            },
            onFailure = {
                QrResult.Error(QrError.ClientError.LoadError(it.message))
            }
        )
    }
}