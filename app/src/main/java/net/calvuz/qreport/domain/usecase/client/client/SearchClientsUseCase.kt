package net.calvuz.qreport.domain.usecase.client.client

import net.calvuz.qreport.domain.model.client.Client
import net.calvuz.qreport.domain.repository.ClientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    private val clientRepository: ClientRepository
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
     * Cerca cliente per partita IVA esatta
     *
     * @param vatNumber Partita IVA da cercare
     * @return Result con cliente se trovato
     */
    suspend fun findByVatNumber(vatNumber: String): Result<Client?> {
        return try {
            if (vatNumber.isBlank()) {
                return Result.failure(IllegalArgumentException("Partita IVA non può essere vuota"))
            }

            val cleanVatNumber = vatNumber.replace("\\s+".toRegex(), "")
            if (!isValidVatNumber(cleanVatNumber)) {
                return Result.failure(IllegalArgumentException("Formato partita IVA non valido"))
            }

            clientRepository.getClientByVatNumber(cleanVatNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Filtra clienti per settore/industria
     *
     * @param industry Settore da filtrare
     * @return Result con lista clienti del settore
     */
    suspend fun filterByIndustry(industry: String): Result<List<Client>> {
        return try {
            if (industry.isBlank()) {
                return Result.failure(IllegalArgumentException("Settore non può essere vuoto"))
            }

            clientRepository.getClientsByIndustry(industry.trim())
                .map { clients -> clients.sortedBy { it.companyName.lowercase() } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ricerca avanzata con filtri multipli
     *
     * @param searchCriteria Criteri di ricerca
     * @return Result con lista clienti filtrata
     */
    suspend fun advancedSearch(searchCriteria: ClientSearchCriteria): Result<List<Client>> {
        return try {
            var result = clientRepository.getActiveClients()

            // Applica filtri progressivamente
            result = result.mapCatching { clients ->
                var filtered = clients

                // Filtro per query testuale
                searchCriteria.textQuery?.takeIf { it.isNotBlank() }?.let { query ->
                    filtered = filtered.filter { client ->
                        client.companyName.contains(query, ignoreCase = true) ||
                                client.vatNumber?.contains(query, ignoreCase = true) == true ||
                                client.industry?.contains(query, ignoreCase = true) == true
                    }
                }

                // Filtro per settore
                searchCriteria.industry?.takeIf { it.isNotBlank() }?.let { industry ->
                    filtered = filtered.filter { client ->
                        client.industry?.equals(industry, ignoreCase = true) == true
                    }
                }

                // Filtro per presenza facilities
                if (searchCriteria.hasFacilities == true) {
                    val clientsWithFacilities = clientRepository.getClientsWithFacilities().getOrThrow()
                    val clientIdsWithFacilities = clientsWithFacilities.map { it.id }.toSet()
                    filtered = filtered.filter { it.id in clientIdsWithFacilities }
                }

                // Filtro per presenza contatti
                if (searchCriteria.hasContacts == true) {
                    val clientsWithContacts = clientRepository.getClientsWithContacts().getOrThrow()
                    val clientIdsWithContacts = clientsWithContacts.map { it.id }.toSet()
                    filtered = filtered.filter { it.id in clientIdsWithContacts }
                }

                // Filtro per presenza isole
                if (searchCriteria.hasIslands == true) {
                    val clientsWithIslands = clientRepository.getClientsWithIslands().getOrThrow()
                    val clientIdsWithIslands = clientsWithIslands.map { it.id }.toSet()
                    filtered = filtered.filter { it.id in clientIdsWithIslands }
                }

                // Ordinamento finale
                filtered.sortedBy { it.companyName.lowercase() }
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene tutti i settori disponibili per filtro
     *
     * @return Result con lista settori ordinata alfabeticamente
     */
    suspend fun getAvailableIndustries(): Result<List<String>> {
        return try {
            clientRepository.getAllIndustries()
                .map { industries -> industries.sorted() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validazione formato partita IVA italiana (11 cifre)
     */
    private fun isValidVatNumber(vatNumber: String): Boolean {
        return vatNumber.matches("\\d{11}".toRegex())
    }
}

/**
 * Data class per criteri di ricerca avanzata
 */
data class ClientSearchCriteria(
    val textQuery: String? = null,
    val industry: String? = null,
    val hasFacilities: Boolean? = null,
    val hasContacts: Boolean? = null,
    val hasIslands: Boolean? = null
)