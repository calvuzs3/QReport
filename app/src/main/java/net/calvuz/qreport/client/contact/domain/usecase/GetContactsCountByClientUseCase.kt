package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.usecase.MyCheckClientExistsUseCase
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

class GetContactsCountByClientUseCase @Inject constructor(
    private val repository: ContactRepository,
    private val checkClientExists: MyCheckClientExistsUseCase
) {

    suspend operator fun invoke(clientId: String): QrResult<Int, QrError> {
        return try {
            // 1. Input validation
            if (clientId.isBlank()) {
                Timber.w("GetContactsCountByClientUseCase.invoke: clientId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
            }

            // 2. Check client exists
            when (val clientCheck = checkClientExists(clientId)) {
                is QrResult.Error -> {
                    Timber.w("GetContactsByClientUseCase.invoke: Client does not exist: $clientId")
                    return QrResult.Error(clientCheck.error)
                }
                is QrResult.Success -> {
                    // Client exists, continue
                }
            }

            when (val result = repository.getContactsCountByClient(clientId)) {
                is QrResult.Success -> {
                    val count = result.data
                    Timber.d("GetContactsByClientUseCase.invoke: Retrieved $count contacts for client: $clientId")
                    return QrResult.Success(count)
                }
                is QrResult.Error -> {
                    Timber.e("GetContactsByClientUseCase.invoke: Repository error for clientId $clientId: ${result.error}")
                    return QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "GetContactsCountByClientUseCase.invoke: Exception getting contacts for client: $clientId")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }
}