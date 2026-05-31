package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import javax.inject.Inject

/**
 * Returns active clients that have at least one ContactsError associated.
 */
class GetActiveClientsWithContactsUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {
    suspend operator fun invoke(): QrResult<List<Client>, QrError.ClientError> =
        clientRepository.getActiveClientsWithContacts().fold(
            onSuccess = { clients ->
                QrResult.Success(clients.sortedBy { it.companyName.lowercase() })
            },
            onFailure = {
                QrResult.Error(QrError.ClientError.LoadError(it.message))
            }
        )
}