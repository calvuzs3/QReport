package net.calvuz.qreport.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.model.island.IslandType

/**
 * Repository interface per la gestione delle isole robotizzate
 *
 * Definisce il contratto per l'accesso ai dati delle isole robotizzate
 * associate agli stabilimenti dei clienti
 */
interface FacilityIslandRepository {

    // ===== BASIC CRUD =====
 suspend fun getAllIslands(): Result<List<FacilityIsland>>

    /**
     * Recupera solo le isole attive
     */
    suspend fun getActiveIslands(): Result<List<FacilityIsland>>

    /**
     * Recupera un'isola per ID
     */
    suspend fun getIslandById(id: String): Result<FacilityIsland?>

    /**
     * Crea una nuova isola
     */
    suspend fun createIsland(island: FacilityIsland): Result<Unit>

    /**
     * Aggiorna un'isola esistente
     */
    suspend fun updateIsland(island: FacilityIsland): Result<Unit>

    /**
     * Elimina un'isola (soft delete)
     */
    suspend fun deleteIsland(id: String): Result<Unit>

    // ===== FACILITY RELATED =====

    /**
     * Recupera isole per stabilimento
     */
    suspend fun getIslandsByFacility(facilityId: String): Result<List<FacilityIsland>>

    /**
     * Osserva isole per stabilimento (Flow reattivo)
     */
    fun getIslandsByFacilityFlow(facilityId: String): Flow<List<FacilityIsland>>

    /**
     * Recupera solo isole attive per stabilimento
     */
    suspend fun getActiveIslandsByFacility(facilityId: String): Result<List<FacilityIsland>>

    // ===== SEARCH & FILTER =====

    /**
     * Recupera isole per tipo
     */
    suspend fun getIslandsByType(islandType: IslandType): Result<List<FacilityIsland>>

    /**
     * Recupera isola per numero seriale
     */
    suspend fun getIslandBySerialNumber(serialNumber: String): Result<FacilityIsland?>

    /**
     * Cerca isole per nome, seriale, modello, etc.
     */
    suspend fun searchIslands(query: String): Result<List<FacilityIsland>>

    // ===== FLOW OPERATIONS (REACTIVE) =====

    /**
     * Osserva tutte le isole attive (Flow reattivo)
     */
    fun getAllActiveIslandsFlow(): Flow<List<FacilityIsland>>

    /**
     * Osserva un'isola per ID (Flow reattivo)
     */
    fun getIslandByIdFlow(id: String): Flow<FacilityIsland?>

    // ===== VALIDATION =====

    /**
     * Verifica se il numero seriale è già utilizzato
     */
    suspend fun isSerialNumberTaken(serialNumber: String, excludeId: String = ""): Result<Boolean>

    // ===== MAINTENANCE OPERATIONS =====

    /**
     * Recupera isole che necessitano manutenzione
     */
    suspend fun getIslandsRequiringMaintenance(currentTime: Instant? = null): Result<List<FacilityIsland>>

    /**
     * Recupera isole sotto garanzia
     */
    suspend fun getIslandsUnderWarranty(currentTime: Instant? = null): Result<List<FacilityIsland>>

    /**
     * Aggiorna data manutenzione
     */
    suspend fun updateMaintenanceDate(islandId: String, maintenanceDate: Instant): Result<Unit>

    /**
     * Aggiorna ore operativo
     */
    suspend fun updateOperatingHours(islandId: String, operatingHours: Int): Result<Unit>

    /**
     * Aggiorna contatore cicli
     */
    suspend fun updateCycleCount(islandId: String, cycleCount: Long): Result<Unit>

    // ===== STATISTICS =====

    /**
     * Conta isole attive
     */
    suspend fun getActiveIslandsCount(): Result<Int>

    /**
     * Conta isole per stabilimento
     */
    suspend fun getIslandsCountByFacility(facilityId: String): Result<Int>

    /**
     * Conta isole per tipo
     */
    suspend fun getIslandsCountByType(islandType: IslandType): Result<Int>

    /**
     * Statistiche per tipo di isola
     */
    suspend fun getIslandTypeStats(): Result<Map<IslandType, Int>>

    /**
     * Statistiche manutenzione
     */
    suspend fun getMaintenanceStats(): Result<Map<String, Int>>

    // ===== CLIENT AGGREGATION =====

    /**
     * Recupera tutte le isole di un cliente (across all facilities)
     */
    suspend fun getIslandsByClient(clientId: String): Result<List<FacilityIsland>>

    /**
     * Conta isole per cliente
     */
    suspend fun getIslandsCountByClient(clientId: String): Result<Int>

    // ===== BULK OPERATIONS =====

    /**
     * Crea multiple isole
     */
    suspend fun createIslands(islands: List<FacilityIsland>): Result<Unit>

    /**
     * Aggiornamento bulk date manutenzione
     */
    suspend fun bulkUpdateMaintenanceDates(updates: Map<String, Instant>): Result<Unit>

    // ===== MAINTENANCE =====

    /**
     * Tocca timestamp dell'isola
     */
    suspend fun touchIsland(id: String): Result<Unit>
}