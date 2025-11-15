package net.calvuz.qreport.domain.usecase.client.facility

import net.calvuz.qreport.domain.model.client.Facility
import net.calvuz.qreport.domain.model.client.FacilityType
import net.calvuz.qreport.domain.repository.FacilityRepository
import net.calvuz.qreport.domain.repository.ClientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use Case per recuperare stabilimenti di un cliente
 *
 * Gestisce:
 * - Validazione esistenza cliente
 * - Recupero facilities ordinate
 * - Flow reattivo per UI
 * - Filtri per tipo facility
 * - Statistiche per cliente
 */
class GetFacilitiesByClientUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val clientRepository: ClientRepository
) {

    /**
     * Recupera tutti gli stabilimenti di un cliente
     *
     * @param clientId ID del cliente
     * @return Result con lista facilities ordinata per primario e nome
     */
    suspend operator fun invoke(clientId: String): Result<List<Facility>> {
        return try {
            // 1. Validazione input
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            // 2. Verifica esistenza cliente
            checkClientExists(clientId).onFailure { return Result.failure(it) }

            // 3. Recupero facilities ordinate
            facilityRepository.getFacilitiesByClient(clientId)
                .map { facilities ->
                    facilities.sortedWith(
                        compareByDescending<Facility> { it.isPrimary } // Prima il primario
                            .thenByDescending { it.isActive } // Poi gli attivi
                            .thenBy { it.name.lowercase() } // Poi per nome
                    )
                }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Osserva tutti gli stabilimenti di un cliente (Flow reattivo)
     *
     * @param clientId ID del cliente
     * @return Flow con lista facilities che si aggiorna automaticamente
     */
    fun observeFacilitiesByClient(clientId: String): Flow<List<Facility>> {
        return facilityRepository.getFacilitiesByClientFlow(clientId)
            .map { facilities ->
                facilities.sortedWith(
                    compareByDescending<Facility> { it.isPrimary }
                        .thenByDescending { it.isActive }
                        .thenBy { it.name.lowercase() }
                )
            }
    }

    /**
     * Recupera solo gli stabilimenti attivi di un cliente
     *
     * @param clientId ID del cliente
     * @return Result con lista facilities attive
     */
    suspend fun getActiveFacilitiesByClient(clientId: String): Result<List<Facility>> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            checkClientExists(clientId).onFailure { return Result.failure(it) }

            facilityRepository.getActiveFacilitiesByClient(clientId)
                .map { facilities ->
                    facilities.sortedWith(
                        compareByDescending<Facility> { it.isPrimary }
                            .thenBy { it.name.lowercase() }
                    )
                }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera lo stabilimento primario di un cliente
     *
     * @param clientId ID del cliente
     * @return Result con facility primaria se esiste
     */
    suspend fun getPrimaryFacility(clientId: String): Result<Facility?> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            checkClientExists(clientId).onFailure { return Result.failure(it) }

            facilityRepository.getPrimaryFacility(clientId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera facilities di un cliente filtrate per tipo
     *
     * @param clientId ID del cliente
     * @param facilityType Tipo di facility da filtrare
     * @return Result con lista facilities del tipo specificato
     */
    suspend fun getFacilitiesByType(
        clientId: String,
        facilityType: FacilityType
    ): Result<List<Facility>> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            checkClientExists(clientId).onFailure { return Result.failure(it) }

            invoke(clientId).map { facilities ->
                facilities.filter { facility ->
                    facility.facilityType == facilityType
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Conta il numero di stabilimenti per un cliente
     *
     * @param clientId ID del cliente
     * @return Result con numero di facilities
     */
    suspend fun getFacilitiesCount(clientId: String): Result<Int> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            facilityRepository.getFacilitiesCountByClient(clientId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene statistiche delle facilities per tipo per un cliente
     *
     * @param clientId ID del cliente
     * @return Result con mappa tipo -> conteggio per il cliente
     */
    suspend fun getFacilityTypeStatsForClient(clientId: String): Result<Map<FacilityType, Int>> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            checkClientExists(clientId).onFailure { return Result.failure(it) }

            invoke(clientId).map { facilities ->
                val stats = mutableMapOf<FacilityType, Int>()

                // Inizializza tutti i tipi con 0
                FacilityType.entries.forEach { stats[it] = 0 }

                // Conta le facilities per tipo
                facilities.groupBy { it.facilityType }
                    .forEach { (type, facilityList) ->
                        stats[type] = facilityList.size
                    }

                stats.toMap()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene il riepilogo operativo delle facilities per cliente
     *
     * @param clientId ID del cliente
     * @return Result con statistiche operative aggregate
     */
    suspend fun getClientFacilitiesSummary(clientId: String): Result<ClientFacilitiesSummary> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            checkClientExists(clientId).onFailure { return Result.failure(it) }

            val facilities = invoke(clientId).getOrElse { return Result.failure(it) }

            val summary = ClientFacilitiesSummary(
                clientId = clientId,
                totalFacilities = facilities.size,
                activeFacilities = facilities.count { it.isActive },
                facilitiesByType = facilities.groupBy { it.facilityType }.mapValues { it.value.size },
                primaryFacility = facilities.find { it.isPrimary },
                facilitiesWithIslands = facilities.count { it.hasIslands() },
                hasPrimaryFacility = facilities.any { it.isPrimary },
                hasCompleteFacilities = facilities.count { it.isComplete() }
            )

            Result.success(summary)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera facilities con isole associate
     *
     * @param clientId ID del cliente
     * @return Result con lista facilities che hanno isole
     */
    suspend fun getFacilitiesWithIslands(clientId: String): Result<List<Facility>> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            checkClientExists(clientId).onFailure { return Result.failure(it) }

            invoke(clientId).map { facilities ->
                facilities.filter { it.hasIslands() }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cerca facilities di un cliente per nome/codice
     *
     * @param clientId ID del cliente
     * @param query Termini di ricerca
     * @return Result con lista facilities che corrispondono alla ricerca
     */
    suspend fun searchFacilitiesForClient(
        clientId: String,
        query: String
    ): Result<List<Facility>> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            if (query.isBlank()) {
                return invoke(clientId)
            }

            checkClientExists(clientId).onFailure { return Result.failure(it) }

            invoke(clientId).map { facilities ->
                val searchQuery = query.lowercase()
                facilities.filter { facility ->
                    facility.name.lowercase().contains(searchQuery) ||
                            facility.code?.lowercase()?.contains(searchQuery) == true ||
                            facility.description?.lowercase()?.contains(searchQuery) == true ||
                            facility.facilityType.displayName.lowercase().contains(searchQuery)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica che il cliente esista ed è attivo
     */
    private suspend fun checkClientExists(clientId: String): Result<Unit> {
        return clientRepository.getClientById(clientId)
            .mapCatching { client ->
                when {
                    client == null ->
                        throw NoSuchElementException("Cliente con ID '$clientId' non trovato")
                    !client.isActive ->
                        throw IllegalStateException("Cliente con ID '$clientId' non attivo")
                }
            }
    }
}

/**
 * Riepilogo operativo delle facilities per un cliente
 */
data class ClientFacilitiesSummary(
    val clientId: String,
    val totalFacilities: Int,
    val activeFacilities: Int,
    val facilitiesByType: Map<FacilityType, Int>,
    val primaryFacility: Facility?,
    val facilitiesWithIslands: Int,
    val hasPrimaryFacility: Boolean,
    val hasCompleteFacilities: Int
) {
    val inactiveFacilities: Int = totalFacilities - activeFacilities
    val completionRate: Float = if (totalFacilities > 0) hasCompleteFacilities.toFloat() / totalFacilities else 0f
}