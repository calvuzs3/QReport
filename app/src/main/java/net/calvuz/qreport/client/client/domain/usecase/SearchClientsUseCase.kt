package net.calvuz.qreport.client.client.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import javax.inject.Inject

/**
 * Searches clients by text query across company name and city.
 *
 * Minimum query length: 2 characters.
 * Results are sorted by match quality: exact → starts-with → alphabetical.
 *
 * The reactive [searchFlow] variant is kept for future real-time search UI;
 * it does not return QrResult because Flow errors are handled via Flow.catch.
 */
class SearchClientsUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {
    suspend operator fun invoke(query: String): QrResult<List<Client>, QrError.ClientError> {
        val trimmed = query.trim()

        if (trimmed.length < 2) {
            return QrResult.Error(QrError.ClientError.LoadError("Search query must be at least 2 characters"))
        }

        return clientRepository.searchClients(trimmed).fold(
            onSuccess = { clients -> QrResult.Success(clients.sortedByRelevance(trimmed)) },
            onFailure = { QrResult.Error(QrError.ClientError.LoadError(it.message)) }
        )
    }

    fun searchFlow(query: String): Flow<List<Client>> =
        clientRepository.searchClientsFlow(query.trim())
            .map { clients -> clients.sortedByRelevance(query.trim()) }

    // -------------------------------------------------------------------------

    private fun List<Client>.sortedByRelevance(query: String): List<Client> =
        sortedWith(
            compareBy<Client> { !it.companyName.equals(query, ignoreCase = true) }
                .thenBy { !it.companyName.startsWith(query, ignoreCase = true) }
                .thenBy { it.companyName.lowercase() }
        )
}