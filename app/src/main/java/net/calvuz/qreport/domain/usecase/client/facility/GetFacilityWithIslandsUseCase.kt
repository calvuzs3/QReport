package net.calvuz.qreport.domain.usecase.client.facility

import net.calvuz.qreport.domain.model.client.Facility
import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.model.island.IslandType
import net.calvuz.qreport.domain.repository.FacilityRepository
import net.calvuz.qreport.domain.repository.FacilityIslandRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * Use Case per recuperare facility con dettagli isole associate
 *
 * Gestisce:
 * - Recupero facility con isole caricate
 * - Aggregazione statistiche isole
 * - Flow reattivo per UI
 * - Filtri per stato isole
 * - Metriche operative
 */
class GetFacilityWithIslandsUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val facilityIslandRepository: FacilityIslandRepository
) {

    /**
     * Recupera una facility con tutte le isole associate
     *
     * @param facilityId ID della facility
     * @return Result con FacilityWithIslands contenente facility e isole
     */
    suspend operator fun invoke(facilityId: String): Result<FacilityWithIslands> {
        return try {
            // 1. Validazione input
            if (facilityId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID facility non può essere vuoto"))
            }

            // 2. Recupero facility
            val facility = facilityRepository.getFacilityById(facilityId)
                .getOrElse { return Result.failure(it) }
                ?: return Result.failure(NoSuchElementException("Facility non trovata"))

            // 3. Recupero isole associate
            val islands = facilityIslandRepository.getIslandsByFacility(facilityId)
                .getOrElse { emptyList() }

            // 4. Creazione oggetto aggregato
            val facilityWithIslands = FacilityWithIslands(
                facility = facility,
                islands = islands.sortedWith(
                    compareByDescending<FacilityIsland> { it.isActive }
                        .thenBy { it.islandType.name }
                        .thenBy { it.displayName.lowercase() }
                ),
                statistics = calculateIslandStatistics(islands)
            )

            Result.success(facilityWithIslands)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Osserva una facility con isole (Flow reattivo)
     *
     * @param facilityId ID della facility
     * @return Flow con FacilityWithIslands che si aggiorna automaticamente
     */
    fun observeFacilityWithIslands(facilityId: String): Flow<FacilityWithIslands?> {
        val facilityFlow = facilityRepository.getFacilityByIdFlow(facilityId)
        val islandsFlow = facilityIslandRepository.getIslandsByFacilityFlow(facilityId)

        return combine(facilityFlow, islandsFlow) { facility, islands ->
            if (facility != null) {
                val sortedIslands = islands.sortedWith(
                    compareByDescending<FacilityIsland> { it.isActive }
                        .thenBy { it.islandType.name }
                        .thenBy { it.displayName.lowercase() }
                )

                FacilityWithIslands(
                    facility = facility,
                    islands = sortedIslands,
                    statistics = calculateIslandStatistics(sortedIslands)
                )
            } else null
        }
    }

    /**
     * Recupera solo le isole attive di una facility
     *
     * @param facilityId ID della facility
     * @return Result con facility e solo isole attive
     */
    suspend fun getWithActiveIslandsOnly(facilityId: String): Result<FacilityWithIslands> {
        return invoke(facilityId).map { facilityWithIslands ->
            val activeIslands = facilityWithIslands.islands.filter { it.isActive }
            facilityWithIslands.copy(
                islands = activeIslands,
                statistics = calculateIslandStatistics(activeIslands)
            )
        }
    }

    /**
     * Recupera facility con isole filtrate per tipo
     *
     * @param facilityId ID della facility
     * @param islandTypes Tipi di isole da includere
     * @return Result con facility e isole filtrate
     */
    suspend fun getWithIslandsByType(
        facilityId: String,
        vararg islandTypes: IslandType
    ): Result<FacilityWithIslands> {
        return invoke(facilityId).map { facilityWithIslands ->
            val filteredIslands = facilityWithIslands.islands.filter {
                it.islandType in islandTypes
            }
            facilityWithIslands.copy(
                islands = filteredIslands,
                statistics = calculateIslandStatistics(filteredIslands)
            )
        }
    }

    /**
     * Recupera facility con isole che richiedono manutenzione
     *
     * @param facilityId ID della facility
     * @return Result con facility e isole da manutenzione
     */
    suspend fun getWithMaintenanceRequired(facilityId: String): Result<FacilityWithIslands> {
        return invoke(facilityId).map { facilityWithIslands ->
            val maintenanceIslands = facilityWithIslands.islands.filter {
                it.needsMaintenance()
            }
            facilityWithIslands.copy(
                islands = maintenanceIslands,
                statistics = calculateIslandStatistics(maintenanceIslands)
            )
        }
    }

    /**
     * Recupera multiple facilities con le loro isole
     *
     * @param facilityIds Lista ID delle facilities
     * @return Result con lista di FacilityWithIslands
     */
    suspend fun getMultipleWithIslands(facilityIds: List<String>): Result<List<FacilityWithIslands>> {
        return try {
            val results = mutableListOf<FacilityWithIslands>()

            facilityIds.forEach { facilityId ->
                invoke(facilityId)
                    .onSuccess { facilityWithIslands ->
                        results.add(facilityWithIslands)
                    }
                    .onFailure {
                        // Log errore ma continua con altre facilities
                        // TODO: Add proper logging
                    }
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera tutte le facilities di un cliente con le loro isole
     *
     * @param clientId ID del cliente
     * @return Result con lista di FacilityWithIslands per il cliente
     */
    suspend fun getAllForClientWithIslands(clientId: String): Result<List<FacilityWithIslands>> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            val facilities = facilityRepository.getFacilitiesByClient(clientId)
                .getOrThrow()

            val facilityIds = facilities.map { it.id }
            getMultipleWithIslands(facilityIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Osserva tutte le facilities di un cliente con isole
     *
     * @param clientId ID del cliente
     * @return Flow con lista di FacilityWithIslands
     */
    fun observeAllForClientWithIslands(clientId: String): Flow<List<FacilityWithIslands>> {
        return facilityRepository.getFacilitiesByClientFlow(clientId)
            .map { facilities ->
                // Per ogni facility, recupera le isole (sincrono per semplicità)
                // In un'implementazione reale, potresti voler ottimizzare questo
                val facilitiesWithIslands = mutableListOf<FacilityWithIslands>()

                facilities.forEach { facility ->
                    // Qui dovresti idealmente usare una query join optimizzata
                    val facilityWithIslands = FacilityWithIslands(
                        facility = facility,
                        islands = emptyList(), // Placeholder - richiede ottimizzazione
                        statistics = IslandStatistics()
                    )
                    facilitiesWithIslands.add(facilityWithIslands)
                }

                facilitiesWithIslands
            }
    }

    /**
     * Calcola statistiche aggregate delle isole
     */
    private fun calculateIslandStatistics(islands: List<FacilityIsland>): IslandStatistics {
        if (islands.isEmpty()) {
            return IslandStatistics()
        }

        return IslandStatistics(
            totalCount = islands.size,
            activeCount = islands.count { it.isActive },
            inactiveCount = islands.count { !it.isActive },
            byType = islands.groupBy { it.islandType }.mapValues { it.value.size },
            totalOperatingHours = islands.sumOf { it.operatingHours },
            totalCycleCount = islands.sumOf { it.cycleCount },
            averageOperatingHours = if (islands.isNotEmpty()) {
                islands.map { it.operatingHours }.average()
            } else 0.0,
            maintenanceDueCount = islands.count { it.needsMaintenance() },
            underWarrantyCount = islands.count { it.isUnderWarranty() },
            oldestInstallation = islands.mapNotNull { it.installationDate }.minOrNull(),
            newestInstallation = islands.mapNotNull { it.installationDate }.maxOrNull()
        )
    }

    /**
     * Cerca facilities con isole che corrispondono ai criteri
     *
     * @param clientId ID del cliente
     * @param searchCriteria Criteri di ricerca
     * @return Result con facilities filtrate
     */
    suspend fun searchFacilitiesWithIslands(
        clientId: String,
        searchCriteria: IslandSearchCriteria
    ): Result<List<FacilityWithIslands>> {
        return try {
            val allFacilitiesWithIslands = getAllForClientWithIslands(clientId)
                .getOrThrow()

            val filteredResults = allFacilitiesWithIslands.mapNotNull { facilityWithIslands ->
                val matchingIslands = facilityWithIslands.islands.filter { island ->
                    matchesSearchCriteria(island, searchCriteria)
                }

                if (matchingIslands.isNotEmpty() || searchCriteria.includeEmptyFacilities) {
                    facilityWithIslands.copy(
                        islands = matchingIslands,
                        statistics = calculateIslandStatistics(matchingIslands)
                    )
                } else null
            }

            Result.success(filteredResults)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica se un'isola corrisponde ai criteri di ricerca
     */
    private fun matchesSearchCriteria(
        island: FacilityIsland,
        criteria: IslandSearchCriteria
    ): Boolean {
        return when {
            criteria.islandTypes.isNotEmpty() && island.islandType !in criteria.islandTypes -> false
            criteria.activeOnly && !island.isActive -> false
            criteria.maintenanceDueOnly && !island.needsMaintenance() -> false
            criteria.underWarrantyOnly && !island.isUnderWarranty() -> false
            criteria.serialNumberQuery != null && !island.serialNumber.contains(criteria.serialNumberQuery, ignoreCase = true) -> false
            criteria.customNameQuery != null && !island.customName.orEmpty().contains(criteria.customNameQuery, ignoreCase = true) -> false
            criteria.minOperatingHours != null && island.operatingHours < criteria.minOperatingHours -> false
            criteria.maxOperatingHours != null && island.operatingHours > criteria.maxOperatingHours -> false
            else -> true
        }
    }
}

/**
 * Facility con isole e statistiche associate
 */
data class FacilityWithIslands(
    val facility: Facility,
    val islands: List<FacilityIsland>,
    val statistics: IslandStatistics
) {
    val hasIslands: Boolean = islands.isNotEmpty()
    val displayName: String = facility.displayName
    val islandCount: Int = islands.size
    val activeIslandCount: Int = statistics.activeCount
}

/**
 * Statistiche aggregate delle isole
 */
data class IslandStatistics(
    val totalCount: Int = 0,
    val activeCount: Int = 0,
    val inactiveCount: Int = 0,
    val byType: Map<IslandType, Int> = emptyMap(),
    val totalOperatingHours: Int = 0,
    val totalCycleCount: Long = 0L,
    val averageOperatingHours: Double = 0.0,
    val maintenanceDueCount: Int = 0,
    val underWarrantyCount: Int = 0,
    val oldestInstallation: Instant? = null,
    val newestInstallation: Instant? = null
) {
    val hasActiveIslands: Boolean = activeCount > 0
    val maintenanceRate: Float = if (totalCount > 0) maintenanceDueCount.toFloat() / totalCount else 0f
    val warrantyRate: Float = if (totalCount > 0) underWarrantyCount.toFloat() / totalCount else 0f
}

/**
 * Criteri di ricerca per isole
 */
data class IslandSearchCriteria(
    val islandTypes: Set<IslandType> = emptySet(),
    val activeOnly: Boolean = false,
    val maintenanceDueOnly: Boolean = false,
    val underWarrantyOnly: Boolean = false,
    val serialNumberQuery: String? = null,
    val customNameQuery: String? = null,
    val minOperatingHours: Int? = null,
    val maxOperatingHours: Int? = null,
    val includeEmptyFacilities: Boolean = false
)