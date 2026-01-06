package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandType
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
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
class GetIslandsByFacilityUseCase @Inject constructor(
    private val islandRepository: IslandRepository,
    private val facilityRepository: FacilityRepository
) {

    /**
     * Recupera tutte le isole di una facility
     *
     * @param facilityId ID della facility
     * @return Result con lista isole ordinata per tipo e serial number
     */
    suspend operator fun invoke(facilityId: String): Result<List<Island>> {
        return try {
            // 1. Validazione input
            if (facilityId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID facility non può essere vuoto"))
            }

            // 2. Verifica esistenza facility
            checkFacilityExists(facilityId).onFailure { return Result.failure(it) }

            // 3. Recupero isole ordinate
            islandRepository.getIslandsByFacility(facilityId)
                .map { islands ->
                    islands.sortedWith(
                        compareBy<Island> { it.islandType.name } // Prima per tipo
                            .thenBy { it.customName?.lowercase() ?: it.serialNumber.lowercase() } // Poi per nome/serial
                    )
                }

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
    fun observeIslandsByFacility(facilityId: String): Flow<List<Island>> {
        return islandRepository.getIslandsByFacilityFlow(facilityId)
            .map { islands ->
                islands.sortedWith(
                    compareBy<Island> { it.islandType.name }
                        .thenBy { it.customName?.lowercase() ?: it.serialNumber.lowercase() }
                )
            }
    }

    /**
     * Recupera solo le isole attive di una facility
     *
     * @param facilityId ID della facility
     * @return Result con lista isole attive
     */
    suspend fun getActiveIslandsByFacility(facilityId: String): Result<List<Island>> {
        return try {
            if (facilityId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID facility non può essere vuoto"))
            }

            checkFacilityExists(facilityId).onFailure { return Result.failure(it) }

            islandRepository.getActiveIslandsByFacility(facilityId)
                .map { islands ->
                    islands.sortedWith(
                        compareBy<Island> { it.islandType.name }
                            .thenBy { it.customName?.lowercase() ?: it.serialNumber.lowercase() }
                    )
                }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera isole di una facility filtrate per tipo
     *
     * @param facilityId ID della facility
     * @param islandType Tipo di isola da filtrare
     * @return Result con lista isole del tipo specificato
     */
    suspend fun getIslandsByType(facilityId: String, islandType: IslandType): Result<List<Island>> {
        return try {
            if (facilityId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID facility non può essere vuoto"))
            }

            checkFacilityExists(facilityId).onFailure { return Result.failure(it) }

            invoke(facilityId).map { islands ->
                islands.filter { island ->
                    island.islandType == islandType
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera isole che richiedono manutenzione per una facility
     *
     * @param facilityId ID della facility
     * @param currentTime Timestamp corrente (opzionale)
     * @return Result con lista isole che richiedono manutenzione
     */
    suspend fun getIslandsDueMaintenance(facilityId: String, currentTime: Instant? = null): Result<List<Island>> {
        return try {
            if (facilityId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID facility non può essere vuoto"))
            }

            checkFacilityExists(facilityId).onFailure { return Result.failure(it) }

            val timestamp = currentTime ?: Clock.System.now()
            val allMaintenanceRequired = islandRepository.getIslandsRequiringMaintenance(timestamp)
                .getOrElse { return Result.failure(it) }

            // Filtra solo quelle della facility specificata
            val facilityIslands = allMaintenanceRequired.filter { island ->
                island.facilityId == facilityId
            }

            Result.success(facilityIslands.sortedBy { it.nextScheduledMaintenance })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera isole sotto garanzia per una facility
     *
     * @param facilityId ID della facility
     * @param currentTime Timestamp corrente (opzionale)
     * @return Result con lista isole ancora sotto garanzia
     */
    suspend fun getIslandsUnderWarranty(facilityId: String, currentTime: Instant? = null): Result<List<Island>> {
        return try {
            if (facilityId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID facility non può essere vuoto"))
            }

            checkFacilityExists(facilityId).onFailure { return Result.failure(it) }

            val timestamp = currentTime ?: Clock.System.now()
            val allUnderWarranty = islandRepository.getIslandsUnderWarranty(timestamp)
                .getOrElse { return Result.failure(it) }

            // Filtra solo quelle della facility specificata
            val facilityIslands = allUnderWarranty.filter { island ->
                island.facilityId == facilityId
            }

            Result.success(facilityIslands.sortedBy { it.warrantyExpiration })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Conta il numero di isole per una facility
     *
     * @param facilityId ID della facility
     * @return Result con numero di isole
     */
    suspend fun getIslandsCount(facilityId: String): Result<Int> {
        return try {
            if (facilityId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID facility non può essere vuoto"))
            }

            islandRepository.getIslandsCountByFacility(facilityId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene statistiche delle isole per tipo per una facility
     *
     * @param facilityId ID della facility
     * @return Result con mappa tipo -> conteggio per la facility
     */
    suspend fun getIslandTypeStatsForFacility(facilityId: String): Result<Map<IslandType, Int>> {
        return try {
            if (facilityId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID facility non può essere vuoto"))
            }

            checkFacilityExists(facilityId).onFailure { return Result.failure(it) }

            invoke(facilityId).map { islands ->
                val stats = mutableMapOf<IslandType, Int>()

                // Inizializza tutti i tipi con 0
                IslandType.entries.forEach { stats[it] = 0 }

                // Conta le isole per tipo
                islands.groupBy { it.islandType }
                    .forEach { (type, islandList) ->
                        stats[type] = islandList.size
                    }

                stats.toMap()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene il riepilogo operativo delle isole per facility
     *
     * @param facilityId ID della facility
     * @return Result con statistiche operative aggregate
     */
    suspend fun getFacilityOperationalSummary(facilityId: String): Result<FacilityOperationalSummary> {
        return try {
            if (facilityId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID facility non può essere vuoto"))
            }

            checkFacilityExists(facilityId).onFailure { return Result.failure(it) }

            val islands = invoke(facilityId).getOrElse { return Result.failure(it) }
            val currentTime = Clock.System.now()

            val summary = FacilityOperationalSummary(
                facilityId = facilityId,
                totalIslands = islands.size,
                activeIslands = islands.count { it.isActive },
                islandsByType = islands.groupBy { it.islandType }.mapValues { it.value.size },
                totalOperatingHours = islands.sumOf { it.operatingHours },
                totalCycles = islands.sumOf { it.cycleCount },
                islandsUnderWarranty = islands.count { island ->
                    island.warrantyExpiration?.let { it > currentTime } == true
                },
                islandsDueMaintenance = islands.count { island ->
                    island.nextScheduledMaintenance?.let { it <= currentTime } == true
                },
                averageOperatingHours = if (islands.isNotEmpty()) {
                    islands.map { it.operatingHours }.average().toInt()
                } else 0
            )

            Result.success(summary)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica che la facility esista ed è attiva
     */
    private suspend fun checkFacilityExists(facilityId: String): Result<Unit> {
        return facilityRepository.getFacilityById(facilityId)
            .mapCatching { facility ->
                when {
                    facility == null ->
                        throw NoSuchElementException("Facility con ID '$facilityId' non trovata")
                    !facility.isActive ->
                        throw IllegalStateException("Facility con ID '$facilityId' non attiva")
                }
            }
    }
}

/**
 * Riepilogo operativo delle isole per una facility
 */
data class FacilityOperationalSummary(
    val facilityId: String,
    val totalIslands: Int,
    val activeIslands: Int,
    val islandsByType: Map<IslandType, Int>,
    val totalOperatingHours: Int,
    val totalCycles: Long,
    val islandsUnderWarranty: Int,
    val islandsDueMaintenance: Int,
    val averageOperatingHours: Int
)