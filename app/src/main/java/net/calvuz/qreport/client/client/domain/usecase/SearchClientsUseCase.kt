package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.client.domain.validator.ClientDataValidator
import javax.inject.Inject

/**
 * Use Case per ricerca e filtro clienti
 *
 * Gestisce:
 * - Ricerca testuale su più campi
 * - Filtro per settore/industria
 * - Ricerca per partita IVA
 * - Flow reattivo per UI con ricerca dinamica
 */
class SearchClientsUseCase @Inject constructor(
    private val clientRepository: ClientRepository,
    private val clientDataValidator: ClientDataValidator
) {

    /**
     * Ricerca clienti per query testuale
     *
     * Cerca in: ragione sociale, partita IVA, settore
     *
     * @param query Testo da cercare
     * @return Result con lista clienti ordinata per relevanza
     */
    suspend operator fun invoke(query: String): Result<List<Client>> {
        return try {
            // Validazione input
            if (query.isBlank()) {
                return Result.failure(IllegalArgumentException("Query di ricerca non può essere vuota"))
            }

            if (query.length < 2) {
                return Result.failure(IllegalArgumentException("Query di ricerca deve essere di almeno 2 caratteri"))
            }

            clientRepository.searchClients(query.trim())
                .map { clients ->
                    // Ordina per relevanza: prima risultati esatti, poi parziali
                    clients.sortedWith(compareBy<Client> { client ->
                        // Prima i match esatti sulla ragione sociale
                        !client.companyName.equals(query.trim(), ignoreCase = true)
                    }.thenBy { client ->
                        // Poi i match che iniziano con la query
                        !client.companyName.startsWith(query.trim(), ignoreCase = true)
                    }.thenBy { client ->
                        // Infine ordine alfabetico
                        client.companyName.lowercase()
                    })
                }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Flow reattivo per ricerca dinamica
     *
     * @param query Testo da cercare
     * @return Flow con lista clienti che si aggiorna in tempo reale
     */
    fun searchFlow(query: String): Flow<List<Client>> {
        return clientRepository.searchClientsFlow(query.trim())
            .map { clients ->
                clients.sortedWith(compareBy<Client> { client ->
                    !client.companyName.equals(query.trim(), ignoreCase = true)
                }.thenBy { client ->
                    !client.companyName.startsWith(query.trim(), ignoreCase = true)
                }.thenBy { client ->
                    client.companyName.lowercase()
                })
            }
    }

    /**
     * Validazione formato partita IVA italiana (11 cifre)
     */
    private fun isValidVatNumber(vatNumber: String): Boolean {
        return vatNumber.matches("\\d{11}".toRegex())
    }
}