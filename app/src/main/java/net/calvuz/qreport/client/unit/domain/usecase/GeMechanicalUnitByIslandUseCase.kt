package net.calvuz.qreport.client.unit.domain.usecase

import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import javax.inject.Inject

/**
 * Use Case per recuperare isole robotizzate di una facility
 *
 * Gestisce:
 * - Validazione esistenza facility
 * - Recupero isole ordinate
 * - Flow reattivo per UI
 * - Filtri per tipo isola
 * - Statistiche per facility
 */
class GeMechanicalUnitByIslandUseCase @Inject constructor(
    private val repository: MechanicalUnitRepository,
    private val islandRepository: IslandRepository
) {

    /**
     * Get all MechaniaclUnit of an Island
     *
     * @param islandID Island ID
     * @return Result with Mechanical Unit List ordered by name, type and s/n
     */
    suspend operator fun invoke(islandId: String): Result<List<MechanicalUnit>> {
        return try {
            // 1. Validazione input
            if (islandId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID Island non può essere vuoto"))
            }

            // 2. Verifica esistenza facility
            checkIslandExists(islandId).onFailure { return Result.failure(it) }

            // 3. Recupero isole ordinate
            repository.getUnitsByIsland(islandId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Osserva tutte le isole di una facility (Flow reattivo)
     *
     * @param facilityId ID della facility
     * @return Flow con lista isole che si aggiorna automaticamente
     */
//    fun observeIslandsByFacility(facilityId: String): Flow<List<Island>> {
//        return repository.getIslandsByFacilityFlow(facilityId)
//            .map { islands ->
//                islands.sortedWith(
//                    compareBy<Island> { it.islandType.name }
//                        .thenBy { it.customName?.lowercase() ?: it.serialNumber.lowercase() }
//                )
//            }
//    }

    /**
     * Recupera solo le isole attive di una facility
     *
     * @param facilityId ID della facility
     * @return Result con lista isole attive
     */
//    suspend fun getActiveIslandsByFacility(facilityId: String): Result<List<Island>> {
//        return try {
//            if (facilityId.isBlank()) {
//                return Result.failure(IllegalArgumentException("ID facility non può essere vuoto"))
//            }
//
//            checkFacilityExists(facilityId).onFailure { return Result.failure(it) }
//
//            repository.getActiveIslandsByFacility(facilityId)
//                .map { islands ->
//                    islands.sortedWith(
//                        compareBy<Island> { it.islandType.name }
//                            .thenBy { it.customName?.lowercase() ?: it.serialNumber.lowercase() }
//                    )
//                }
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

    /**
     * Recupera isole di una facility filtrate per tipo
     *
     * @param facilityId ID della facility
     * @param islandType Tipo di isola da filtrare
     * @return Result con lista isole del tipo specificato
     */
//    suspend fun getIslandsByType(facilityId: String, islandType: IslandType): Result<List<Island>> {
//        return try {
//            if (facilityId.isBlank()) {
//                return Result.failure(IllegalArgumentException("ID facility non può essere vuoto"))
//            }
//
//            checkFacilityExists(facilityId).onFailure { return Result.failure(it) }
//
//            invoke(facilityId).map { islands ->
//                islands.filter { island ->
//                    island.islandType == islandType
//                }
//            }
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }


    /**
     * Conta il numero di isole per una facility
     *
     * @param facilityId ID della facility
     * @return Result con numero di isole
     */
//    suspend fun getIslandsCount(facilityId: String): Result<Int> {
//        return try {
//            if (facilityId.isBlank()) {
//                return Result.failure(IllegalArgumentException("ID facility non può essere vuoto"))
//            }
//
//            repository.getIslandsCountByFacility(facilityId)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

    /**
     * Ottiene statistiche delle isole per tipo per una facility
     *
     * @param facilityId ID della facility
     * @return Result con mappa tipo -> conteggio per la facility
     */
//    suspend fun getIslandTypeStatsForFacility(facilityId: String): Result<Map<IslandType, Int>> {
//        return try {
//            if (facilityId.isBlank()) {
//                return Result.failure(IllegalArgumentException("ID facility non può essere vuoto"))
//            }
//
//            checkFacilityExists(facilityId).onFailure { return Result.failure(it) }
//
//            invoke(facilityId).map { islands ->
//                val stats = mutableMapOf<IslandType, Int>()
//
//                // Inizializza tutti i tipi con 0
//                IslandType.entries.forEach { stats[it] = 0 }
//
//                // Conta le isole per tipo
//                islands.groupBy { it.islandType }
//                    .forEach { (type, islandList) ->
//                        stats[type] = islandList.size
//                    }
//
//                stats.toMap()
//            }
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }


    /**
     * Verifica che la facility esista ed è attiva
     */
    private suspend fun checkIslandExists(islandId: String): Result<Unit> {
        return islandRepository.getIslandById(islandId)
            .mapCatching { island ->
                when {
                    island == null ->
                        throw NoSuchElementException("Island con ID '$islandId' non trovata")
                    !island.isActive ->
                        throw IllegalStateException("Island con ID '$islandId' non attiva")
                }
            }
    }
}