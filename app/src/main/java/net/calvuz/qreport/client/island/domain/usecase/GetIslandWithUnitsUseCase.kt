package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandType
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.model.UnitType
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import javax.inject.Inject

class GetIslandWithUnitsUseCase @Inject constructor(
    private val repository: IslandRepository,
    private val unitRepository: MechanicalUnitRepository
) {

    /**
     * Rescue an Island with all Units associated
     *
     * @param islandId Island ID
     * @return Result with IslandWithUnits
     */
    suspend operator fun invoke(islandId: String): Result<IslandWithUnits> {
        return try {

            // 1. Validazione input
            if (islandId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID Island non può essere vuoto"))
            }

            // 2. Recupero facility
            val island = repository.getIslandById(islandId)
                .getOrElse { return Result.failure(it) }
                ?: return Result.failure(NoSuchElementException("Island non trovata"))

            // 3. Recupero unità associate
            val units = unitRepository.getUnitsByIsland(islandId)
                .getOrElse { emptyList() }

            // 4. Creazione oggetto aggregato
            val islandWithUnits = IslandWithUnits(
                island = island,
                units = units.sortedWith(
                    compareByDescending<MechanicalUnit> { it.isActive }
                        .thenBy { it.unitType.name }
                ),
                statistics = calculateUnitStatistics(units)
            )

            Result.success(islandWithUnits)

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
//    fun observeFacilityWithIslands(facilityId: String): Flow<FacilityWithIslands?> {
//        val facilityFlow = repository.getFacilityByIdFlow(facilityId)
//        val islandsFlow = unitRepository.getIslandsByFacilityFlow(facilityId)
//
//        return combine(facilityFlow, islandsFlow) { facility, islands ->
//            if (facility != null) {
//                val sortedIslands = islands.sortedWith(
//                    compareByDescending<Island> { it.isActive }
//                        .thenBy { it.islandType.name }
//                        .thenBy { it.displayName.lowercase() }
//                )
//
//                FacilityWithIslands(
//                    facility = facility,
//                    islands = sortedIslands,
//                    statistics = calculateIslandStatistics(sortedIslands)
//                )
//            } else null
//        }
//    }

    /**
     * Recupera solo le isole attive di una facility
     *
     * @param facilityId ID della facility
     * @return Result con facility e solo isole attive
     */
//    suspend fun getWithActiveIslandsOnly(facilityId: String): Result<FacilityWithIslands> {
//        return invoke(facilityId).map { facilityWithIslands ->
//            val activeIslands = facilityWithIslands.islands.filter { it.isActive }
//            facilityWithIslands.copy(
//                islands = activeIslands,
//                statistics = calculateIslandStatistics(activeIslands)
//            )
//        }
//    }

    /**
     * Recupera facility con isole filtrate per tipo
     *
     * @param facilityId ID della facility
     * @param islandTypes Tipi di isole da includere
     * @return Result con facility e isole filtrate
     */
//    suspend fun getWithIslandsByType(
//        facilityId: String,
//        vararg islandTypes: IslandType
//    ): Result<FacilityWithIslands> {
//        return invoke(facilityId).map { facilityWithIslands ->
//            val filteredIslands = facilityWithIslands.islands.filter {
//                it.islandType in islandTypes
//            }
//            facilityWithIslands.copy(
//                islands = filteredIslands,
//                statistics = calculateIslandStatistics(filteredIslands)
//            )
//        }
//    }


//    /**
//     * Recupera multiple facilities con le loro isole
//     *
//     * @param facilityIds Lista ID delle facilities
//     * @return Result con lista di FacilityWithIslands
//     */
//    suspend fun getMultipleWithIslands(facilityIds: List<String>): Result<List<FacilityWithIslands>> {
//        return try {
//            val results = mutableListOf<FacilityWithIslands>()
//
//            facilityIds.forEach { facilityId ->
//                invoke(facilityId)
//                    .onSuccess { facilityWithIslands ->
//                        results.add(facilityWithIslands)
//                    }
//                    .onFailure {
//                        // Log errore ma continua con altre facilities
//                        // TODO: Add proper logging
//                    }
//            }
//
//            Result.success(results)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

    /**
     * Recupera tutte le facilities di un cliente con le loro isole
     *
     * @param clientId ID del cliente
     * @return Result con lista di FacilityWithIslands per il cliente
     */
//    suspend fun getAllForClientWithIslands(clientId: String): Result<List<FacilityWithIslands>> {
//        return try {
//            if (clientId.isBlank()) {
//                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
//            }
//
//            val facilities = repository.getFacilitiesByClient(clientId)
//                .getOrThrow()
//
//            val facilityIds = facilities.map { it.id }
//            getMultipleWithIslands(facilityIds)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

    /**
     * Osserva tutte le facilities di un cliente con isole
     *
     * @param clientId ID del cliente
     * @return Flow con lista di FacilityWithIslands
     */
//    fun observeAllForClientWithIslands(clientId: String): Flow<List<FacilityWithIslands>> {
//        return repository.getFacilitiesByClientFlow(clientId)
//            .map { facilities ->
//                // Per ogni facility, recupera le isole (sincrono per semplicità)
//                // In un'implementazione reale, potresti voler ottimizzare questo
//                val facilitiesWithIslands = mutableListOf<FacilityWithIslands>()
//
//                facilities.forEach { facility ->
//                    // Qui dovresti idealmente usare una query join optimizzata
//                    val facilityWithIslands = FacilityWithIslands(
//                        facility = facility,
//                        islands = emptyList(), // Placeholder - richiede ottimizzazione
//                        statistics = IslandStatistics()
//                    )
//                    facilitiesWithIslands.add(facilityWithIslands)
//                }
//
//                facilitiesWithIslands
//            }
//    }

    private fun calculateUnitStatistics(units: List<MechanicalUnit>): UnitStatistics {
        if (units.isEmpty()) {
            return UnitStatistics()
        }

        return UnitStatistics(
            totalCount = units.size,
            activeCount = units.count { it.isActive },
            inactiveCount = units.count { !it.isActive },
            byType = units.groupBy { it.unitType }.mapValues { it.value.size },
        )
    }

    /**
     * Cerca facilities con isole che corrispondono ai criteri
     *
     * @param clientId ID del cliente
     * @param searchCriteria Criteri di ricerca
     * @return Result con facilities filtrate
     */
//    suspend fun searchFacilitiesWithIslands(
//        clientId: String,
//        searchCriteria: IslandSearchCriteria
//    ): Result<List<FacilityWithIslands>> {
//        return try {
//            val allFacilitiesWithIslands = getAllForClientWithIslands(clientId)
//                .getOrThrow()
//
//            val filteredResults = allFacilitiesWithIslands.mapNotNull { facilityWithIslands ->
//                val matchingIslands = facilityWithIslands.islands.filter { island ->
//                    matchesSearchCriteria(island, searchCriteria)
//                }
//
//                if (matchingIslands.isNotEmpty() || searchCriteria.includeEmptyFacilities) {
//                    facilityWithIslands.copy(
//                        islands = matchingIslands,
//                        statistics = calculateIslandStatistics(matchingIslands)
//                    )
//                } else null
//            }
//
//            Result.success(filteredResults)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

    /**
     * Verifica se un'isola corrisponde ai criteri di ricerca
     */
//    private fun matchesSearchCriteria(
//        island: Island,
//        criteria: IslandSearchCriteria
//    ): Boolean {
//        return when {
//            criteria.islandTypes.isNotEmpty() && island.islandType !in criteria.islandTypes -> false
//            criteria.activeOnly && !island.isActive -> false
//            criteria.maintenanceDueOnly && !island.needsMaintenance() -> false
//            criteria.underWarrantyOnly && !island.isUnderWarranty() -> false
//            criteria.serialNumberQuery != null && !island.serialNumber.contains(criteria.serialNumberQuery, ignoreCase = true) -> false
//            criteria.customNameQuery != null && !island.customName.orEmpty().contains(criteria.customNameQuery, ignoreCase = true) -> false
//            criteria.minOperatingHours != null && island.operatingHours < criteria.minOperatingHours -> false
//            criteria.maxOperatingHours != null && island.operatingHours > criteria.maxOperatingHours -> false
//            else -> true
//        }
//    }
}

/**
 * Facility con isole e statistiche associate
 */
data class IslandWithUnits(
    val island: Island,
    val units: List<MechanicalUnit>,
    val statistics: UnitStatistics
) {
    val hasUnits: Boolean = units.isNotEmpty()
    val displayName: String = island.displayName
    val unitsCount: Int = units.size
}

/**
 * Statistiche aggregate delle isole
 */
data class UnitStatistics(
    val totalCount: Int = 0,
    val activeCount: Int = 0,
    val inactiveCount: Int = 0,
    val byType: Map<UnitType, Int> = emptyMap(),
) {
    val hasActiveIslands: Boolean = activeCount > 0
    val hasInactiveUnits: Boolean = inactiveCount >0
}

/**
 * Criteri di ricerca per isole
 */
data class UnitSearchCriteria(
    val unitType: Set<UnitType> = emptySet(),
    val activeOnly: Boolean = false,
)