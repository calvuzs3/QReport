package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.usecase.CheckClientExistsUseCase
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

class GetContactsCountByClientUseCase @Inject constructor(
    private val repository: ContactRepository,
    private val checkClientExists: CheckClientExistsUseCase
) {

    suspend operator fun invoke(clientId: String): QrResult<Int, QrError> {
        return try {
            
            // Check input
            if (clientId.isBlank()) {
                Timber.w("ClientId is blank")
                return QrResult.Error(QrError.ContactsError.MissingClientId())
            }

            // Check client exists
            when (val clientCheck = checkClientExists(clientId)) {
                is QrResult.Success -> Unit
                is QrResult.Error -> {
                    Timber.w("Client does not exist: $clientId")
                    return QrResult.Error(clientCheck.error)
                }
            }

            when (val result = repository.getContactsCountByClient(clientId)) {
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
            Timber.e(e, clientId)
            QrResult.Error(QrError.SystemError.UnknownError())
        }
    }
}