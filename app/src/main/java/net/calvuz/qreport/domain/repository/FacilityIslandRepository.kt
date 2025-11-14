package net.calvuz.qreport.domain.repository

import net.calvuz.qreport.domain.model.client.FacilityIsland
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.domain.model.island.IslandType

/**
 * Repository interface per gestione isole robotizzate per stabilimenti
 * Definisce il contratto per accesso ai dati delle isole POLY
 * Implementazione nel data layer
 */
interface FacilityIslandRepository {

    // ===== CRUD OPERATIONS =====

    suspend fun getAllIslands(): Result<List<FacilityIsland>>
    suspend fun getActiveIslands(): Result<List<FacilityIsland>>
    suspend fun getIslandById(id: String): Result<FacilityIsland?>
    suspend fun createIsland(island: FacilityIsland): Result<Unit>
    suspend fun updateIsland(island: FacilityIsland): Result<Unit>
    suspend fun deleteIsland(id: String): Result<Unit>

    // ===== FACILITY RELATED =====

    suspend fun getIslandsByFacility(facilityId: String): Result<List<FacilityIsland>>
    fun getIslandsByFacilityFlow(facilityId: String): Flow<List<FacilityIsland>>
    suspend fun getActiveIslandsByFacility(facilityId: String): Result<List<FacilityIsland>>

    // ===== SEARCH & FILTER =====

    suspend fun getIslandsByType(islandType: IslandType): Result<List<FacilityIsland>>
    suspend fun getIslandBySerialNumber(serialNumber: String): Result<FacilityIsland?>
    suspend fun searchIslands(query: String): Result<List<FacilityIsland>>

    // ===== FLOW OPERATIONS (REACTIVE) =====

    suspend fun getAllActiveIslandsFlow(): Flow<List<FacilityIsland>>
    fun getIslandByIdFlow(id: String): Flow<FacilityIsland?>

    // ===== VALIDATION =====

    suspend fun isSerialNumberTaken(serialNumber: String, excludeId: String = ""): Result<Boolean>

    // ===== MAINTENANCE OPERATIONS =====

    suspend fun getIslandsRequiringMaintenance(currentTime: Long? = null): Result<List<FacilityIsland>>
    suspend fun getIslandsUnderWarranty(currentTime: Long? = null): Result<List<FacilityIsland>>
    suspend fun updateMaintenanceDate(islandId: String, maintenanceDate: Long): Result<Unit>
    suspend fun updateOperatingHours(islandId: String, operatingHours: Int): Result<Unit>
    suspend fun updateCycleCount(islandId: String, cycleCount: Long): Result<Unit>

    // ===== STATISTICS =====

    suspend fun getActiveIslandsCount(): Result<Int>
    suspend fun getIslandsCountByFacility(facilityId: String): Result<Int>
    suspend fun getIslandsCountByType(islandType: IslandType): Result<Int>
    suspend fun getIslandTypeStats(): Result<Map<IslandType, Int>>
    suspend fun getMaintenanceStats(): Result<Map<String, Int>>

    // ===== BULK OPERATIONS =====

    suspend fun createIslands(islands: List<FacilityIsland>): Result<Unit>
    suspend fun bulkUpdateMaintenanceDates(updates: Map<String, Long>): Result<Unit>

    // ===== CLIENT AGGREGATION =====

    suspend fun getIslandsByClient(clientId: String): Result<List<FacilityIsland>>
    suspend fun getIslandsCountByClient(clientId: String): Result<Int>

    // ===== MAINTENANCE =====

    suspend fun touchIsland(id: String): Result<Unit>
}