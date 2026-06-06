package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import timber.log.Timber
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

        Timber.d("Checking uniqueness of client company name")

        if (client.companyName.isBlank()) {
            Timber.d("Client company name is blank")
            return QrResult.Error(QrError.ClientError.MissingCompanyName())
        }

        return clientRepository.isCompanyNameTaken(client.companyName, client.id).fold(
            onSuccess = { isTaken ->
                if (isTaken) {
                    Timber.d("Client company name is already taken")
                    QrResult.Error(QrError.ClientError.CreateError())
                }
                else {
                    Timber.d("Client company name is available")
                    QrResult.Success(Unit)
                }
            },
            onFailure = {
                Timber.d( "Failed to check uniqueness of client company name: ${it.message}")
                QrResult.Error(QrError.ClientError.LoadError(it.message))
            }
        )
    }
}