package net.calvuz.qreport.client.island.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandType

/**
 * Repository interface per la gestione delle isole robotizzate
 *
 * Definisce il contratto per l'accesso ai dati delle isole robotizzate
 * associate agli stabilimenti dei clienti
 */
interface IslandRepository {

    // ===== BASIC CRUD =====
 suspend fun getAllIslands(): Result<List<Island>>

    /**
     * Recupera solo le isole attive
     */
    suspend fun getActiveIslands(): Result<List<Island>>

    /**
     * Recupera un'isola per ID
     */
    suspend fun getIslandById(id: String): Result<Island?>

    suspend fun getIslandsByIds(ids: List<String>): Result<List<Island>>

   /**
     * Crea una nuova isola
     */
    suspend fun createIsland(island: Island): Result<Unit>

    /**
     * Aggiorna un'isola esistente
     */
    suspend fun updateIsland(island: Island): Result<Unit>

    /**
     * Elimina un'isola (soft delete)
     */
    suspend fun deleteIsland(id: String): Result<Unit>

    // ===== FACILITY RELATED =====

    /**
     * Recupera isole per stabilimento
     */
    suspend fun getIslandsByFacility(facilityId: String): Result<List<Island>>

    /**
     * Osserva isole per stabilimento (Flow reattivo)
     */
    fun getIslandsByFacilityFlow(facilityId: String): Flow<List<Island>>

    /**
     * Recupera solo isole attive per stabilimento
     */
    suspend fun getActiveIslandsByFacility(facilityId: String): Result<List<Island>>

    // ===== SEARCH & FILTER =====

    /**
     * Recupera isole per tipo
     */
    suspend fun getIslandsByType(islandType: IslandType): Result<List<Island>>

    /**
     * Recupera isola per numero seriale
     */
    suspend fun getIslandBySerialNumber(serialNumber: String): Result<Island?>

    /**
     * Cerca isole per nome, seriale, modello, etc.
     */
    suspend fun searchIslands(query: String): Result<List<Island>>

    // ===== FLOW OPERATIONS (REACTIVE) =====

    /**
     * Osserva tutte le isole attive (Flow reattivo)
     */
    fun getAllActiveIslandsFlow(): Flow<List<Island>>

    /**
     * Osserva un'isola per ID (Flow reattivo)
     */
    fun getIslandByIdFlow(id: String): Flow<Island?>

    // ===== VALIDATION =====

    /**
     * Verifica se il numero seriale è già utilizzato
     */
    suspend fun isSerialNumberTaken(serialNumber: String, excludeId: String = ""): Result<Boolean>

    // ===== MAINTENANCE OPERATIONS =====

    /**
     * Recupera isole che necessitano manutenzione
     */
    suspend fun getIslandsRequiringMaintenance(currentTime: Instant? = null): Result<List<Island>>

    /**
     * Recupera isole sotto garanzia
     */
    suspend fun getIslandsUnderWarranty(currentTime: Instant? = null): Result<List<Island>>

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
    suspend fun getIslandsByClient(clientId: String): Result<List<Island>>

    /**
     * Conta isole per cliente
     */
    suspend fun getIslandsCountByClient(clientId: String): Result<Int>

    // ===== BULK OPERATIONS =====

    /**
     * Crea multiple isole
     */
    suspend fun createIslands(islands: List<Island>): Result<Unit>

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