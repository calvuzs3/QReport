package net.calvuz.qreport.client.island.domain.usecase

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandType
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

/**
 * Use Case per ricerca e filtro isole robotizzate
 *
 * Gestisce:
 * - Ricerca testuale su più campi
 * - Filtro per tipo isola, facility, stato
 * - Ricerca per serial number
 * - Filtri per stato manutenzione e garanzia
 * - Ricerca cross-cliente
 */
class SearchIslandsUseCase @Inject constructor(
    private val islandRepository: IslandRepository
) {

    /**
     * Ricerca isole per query testuale
     *
     * Cerca in: serial number, nome personalizzato, modello, ubicazione
     *
     * @param query Testo da cercare
     * @return Result con lista isole ordinata per relevanza
     */
    suspend operator fun invoke(query: String): Result<List<Island>> {
        return try {
            // Validazione input
            if (query.isBlank()) {
                return Result.failure(IllegalArgumentException("Query di ricerca non può essere vuota"))
            }

            if (query.length < 2) {
                return Result.failure(IllegalArgumentException("Query di ricerca deve essere di almeno 2 caratteri"))
            }

            islandRepository.searchIslands(query.trim())
                .map { islands ->
                    // Ordina per relevanza
                    islands.sortedWith(
                        compareBy<Island> { island ->
                            // Prima per match esatto serial number
                            !island.serialNumber.equals(query.trim(), ignoreCase = true)
                        }
                            .thenBy { island ->
                                // Poi per match che inizia con query
                                !island.serialNumber.startsWith(query.trim(), ignoreCase = true) &&
                                        island.customName?.startsWith(query.trim(), ignoreCase = true) != true
                            }
                            .thenBy { island ->
                                // Infine ordine alfabetico per serial number
                                island.serialNumber.lowercase()
                            }
                    )
                }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cerca isola per serial number esatto
     *
     * @param serialNumber Serial number da cercare
     * @return Result con isola se trovata
     */
    suspend fun findBySerialNumber(serialNumber: String): Result<Island?> {
        return try {
            if (serialNumber.isBlank()) {
                return Result.failure(IllegalArgumentException("Serial number non può essere vuoto"))
            }

            islandRepository.getIslandBySerialNumber(serialNumber.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Filtra isole per tipo
     *
     * @param islandType Tipo di isola da filtrare
     * @return Result con lista isole del tipo specificato
     */
    suspend fun filterByType(islandType: IslandType): Result<List<Island>> {
        return try {
            islandRepository.getIslandsByType(islandType)
                .map { islands ->
                    islands.sortedWith(
                        compareBy<Island> { it.serialNumber.lowercase() }
                    )
                }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Filtra isole per cliente
     *
     * @param clientId ID del cliente
     * @return Result con lista isole del cliente
     */
    suspend fun filterByClient(clientId: String): Result<List<Island>> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            islandRepository.getIslandsByClient(clientId)
                .map { islands ->
                    islands.sortedWith(
                        compareBy<Island> { it.islandType.name }
                            .thenBy { it.serialNumber.lowercase() }
                    )
                }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ricerca avanzata con filtri multipli
     *
     * @param searchCriteria Criteri di ricerca
     * @return Result con lista isole filtrata
     */
    suspend fun advancedSearch(searchCriteria: IslandSearchCriteria): Result<List<Island>> {
        return try {
            var result = islandRepository.getActiveIslands()

            // Applica filtri progressivamente
            result = result.mapCatching { islands ->
                var filtered = islands

                // Filtro per query testuale
                searchCriteria.textQuery?.takeIf { it.isNotBlank() }?.let { query ->
                    filtered = filtered.filter { island ->
                        island.serialNumber.contains(query, ignoreCase = true) ||
                                island.customName?.contains(query, ignoreCase = true) == true ||
                                island.model?.contains(query, ignoreCase = true) == true ||
                                island.location?.contains(query, ignoreCase = true) == true
                    }
                }

                // Filtro per tipo isola
                searchCriteria.islandType?.let { type ->
                    filtered = filtered.filter { island ->
                        island.islandType == type
                    }
                }

                // Filtro per facility
                searchCriteria.facilityId?.takeIf { it.isNotBlank() }?.let { facilityId ->
                    filtered = filtered.filter { island ->
                        island.facilityId == facilityId
                    }
                }

                // Filtro per stato attivo
                searchCriteria.isActive?.let { active ->
                    filtered = filtered.filter { island ->
                        island.isActive == active
                    }
                }

                // Filtro per garanzia attiva
                searchCriteria.underWarranty?.let { underWarranty ->
                    val currentTime = Clock.System.now()
                    filtered = filtered.filter { island ->
                        val isUnderWarranty = island.warrantyExpiration?.let { it > currentTime } == true
                        isUnderWarranty == underWarranty
                    }
                }

                // Filtro per manutenzione scaduta
                searchCriteria.maintenanceOverdue?.let { overdue ->
                    val currentTime = Clock.System.now()
                    filtered = filtered.filter { island ->
                        val isOverdue = island.nextScheduledMaintenance?.let { it <= currentTime } == true
                        isOverdue == overdue
                    }
                }

                // Filtro per range ore operative
                searchCriteria.operatingHoursMin?.let { minHours ->
                    filtered = filtered.filter { island ->
                        island.operatingHours >= minHours
                    }
                }

                searchCriteria.operatingHoursMax?.let { maxHours ->
                    filtered = filtered.filter { island ->
                        island.operatingHours <= maxHours
                    }
                }

                // Filtro per range conteggio cicli
                searchCriteria.cycleCountMin?.let { minCycles ->
                    filtered = filtered.filter { island ->
                        island.cycleCount >= minCycles
                    }
                }

                searchCriteria.cycleCountMax?.let { maxCycles ->
                    filtered = filtered.filter { island ->
                        island.cycleCount <= maxCycles
                    }
                }

                // Filtro per modello
                searchCriteria.model?.takeIf { it.isNotBlank() }?.let { model ->
                    filtered = filtered.filter { island ->
                        island.model?.contains(model, ignoreCase = true) == true
                    }
                }

                // Ordinamento finale
                filtered.sortedWith(
                    compareBy<Island> { it.islandType.name }
                        .thenBy { it.serialNumber.lowercase() }
                )
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene isole che richiedono attenzione (manutenzione, garanzia, etc.)
     *
     * @param currentTime Timestamp corrente (opzionale)
     * @return Result con mappa categoria -> lista isole
     */
    suspend fun getIslandsRequiringAttention(currentTime: Instant? = null): Result<Map<String, List<Island>>> {
        return try {
            val timestamp = currentTime ?: Clock.System.now()
            val categories = mutableMapOf<String, List<Island>>()

            // Isole con manutenzione scaduta
            val maintenanceOverdue = islandRepository.getIslandsRequiringMaintenance(timestamp)
                .getOrElse { return Result.failure(it) }
            categories["maintenance_overdue"] = maintenanceOverdue

            // Isole con garanzia in scadenza (prossimi 30 giorni)
            val warrantyExpiringSoon = islandRepository.getIslandsUnderWarranty(timestamp)
                .getOrElse { return Result.failure(it) }
                .filter { island ->
                    island.warrantyExpiration?.let { warranty ->
                        val thirtyDaysFromNow = timestamp + (30.days)
                        warranty <= thirtyDaysFromNow
                    } == true
                }
            categories["warranty_expiring"] = warrantyExpiringSoon

            // Isole con ore operative elevate (>8000 ore)
            val allIslands = islandRepository.getActiveIslands()
                .getOrElse { return Result.failure(it) }

            val highOperatingHours = allIslands.filter { island ->
                island.operatingHours > 8000
            }
            categories["high_operating_hours"] = highOperatingHours

            // Isole con cicli elevati (>500k cicli)
            val highCycles = allIslands.filter { island ->
                island.cycleCount > 500000L
            }
            categories["high_cycle_count"] = highCycles

            Result.success(categories.toMap())

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Statistiche di ricerca per analytics
     *
     * @return Result con mappa statistica -> valore
     */
    suspend fun getSearchStatistics(): Result<Map<String, Any>> {
        return try {
            val allIslands = islandRepository.getActiveIslands()
                .getOrElse { return Result.failure(it) }

            val stats = mutableMapOf<String, Any>()

            // Conteggi per tipo
            val typeStats = allIslands.groupBy { it.islandType }
                .mapValues { it.value.size }
            stats["by_type"] = typeStats

            // Statistiche ore operative
            if (allIslands.isNotEmpty()) {
                stats["avg_operating_hours"] = allIslands.map { it.operatingHours }.average().toInt()
                stats["max_operating_hours"] = allIslands.maxOf { it.operatingHours }
                stats["total_operating_hours"] = allIslands.sumOf { it.operatingHours }
            }

            // Statistiche cicli
            if (allIslands.isNotEmpty()) {
                stats["avg_cycle_count"] = allIslands.map { it.cycleCount }.average().toLong()
                stats["max_cycle_count"] = allIslands.maxOf { it.cycleCount }
                stats["total_cycle_count"] = allIslands.sumOf { it.cycleCount }
            }

            // Stato garanzia
            val currentTime = Clock.System.now()
            val underWarranty = allIslands.count { island ->
                island.warrantyExpiration?.let { it > currentTime } == true
            }
            stats["under_warranty"] = underWarranty
            stats["warranty_expired"] = allIslands.size - underWarranty

            // Stato manutenzione
            val maintenanceOverdue = allIslands.count { island ->
                island.nextScheduledMaintenance?.let { it <= currentTime } == true
            }
            stats["maintenance_overdue"] = maintenanceOverdue
            stats["maintenance_up_to_date"] = allIslands.size - maintenanceOverdue

            Result.success(stats.toMap())

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ricerca isole per pattern serial number
     *
     * @param pattern Pattern di ricerca (supporta wildcards)
     * @return Result con lista isole corrispondenti
     */
    suspend fun searchBySerialPattern(pattern: String): Result<List<Island>> {
        return try {
            if (pattern.isBlank()) {
                return Result.failure(IllegalArgumentException("Pattern non può essere vuoto"))
            }

            // Converti pattern in regex (supporta * come wildcard)
            val regexPattern = pattern.replace("*", ".*")
            val regex = Regex(regexPattern, RegexOption.IGNORE_CASE)

            val allIslands = islandRepository.getActiveIslands()
                .getOrElse { return Result.failure(it) }

            val matchingIslands = allIslands.filter { island ->
                regex.matches(island.serialNumber)
            }

            Result.success(matchingIslands.sortedBy { it.serialNumber })

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Data class per criteri di ricerca avanzata isole
 */
data class IslandSearchCriteria(
    val textQuery: String? = null,
    val islandType: IslandType? = null,
    val facilityId: String? = null,
    val isActive: Boolean? = null,
    val underWarranty: Boolean? = null,
    val maintenanceOverdue: Boolean? = null,
    val operatingHoursMin: Int? = null,
    val operatingHoursMax: Int? = null,
    val cycleCountMin: Long? = null,
    val cycleCountMax: Long? = null,
    val model: String? = null
)