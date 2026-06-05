package net.calvuz.qreport.client.island.domain.repository

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandType

interface IslandRepository {

    // ===== BASIC CRUD =====
    suspend fun getAllIslands(): Result<List<Island>>
    suspend fun getAllActiveIslands(): Result<List<Island>>
    suspend fun getIslandById(id: String): Result<Island?>
    suspend fun getIslandsByIds(ids: List<String>): Result<List<Island>>
    suspend fun createIsland(island: Island): Result<Unit>
    suspend fun updateIsland(island: Island): Result<Unit>
    suspend fun deleteIsland(island: Island): Result<Unit>

    // ===== DELETE — TWO-STAGE =====

    @Transaction
    suspend fun deactivateIsland(id: String): Result<Unit>

    // ===== FACILITY RELATED =====

    suspend fun getIslandsByFacility(facilityId: String): Result<List<Island>>
    fun getAllIslandsByFacilityFlow(facilityId: String): Flow<List<Island>>
    fun getAllActiveIslandsByFacilityFlow(facilityId: String): Flow<List<Island>>
    suspend fun getActiveIslandsByFacility(facilityId: String): Result<List<Island>>

    // ===== SEARCH & FILTER =====

    suspend fun getIslandsByType(islandType: IslandType): Result<List<Island>>
    suspend fun getIslandBySerialNumber(serialNumber: String): Result<Island?>
    suspend fun searchIslands(query: String): Result<List<Island>>

    // ===== FLOW OPERATIONS (REACTIVE) =====

    fun getAllIslandsFlow(): Flow<List<Island>>
    fun getAllActiveIslandsFlow(): Flow<List<Island>>
    fun getIslandByIdFlow(id: String): Flow<Island?>

    // ===== VALIDATION =====

    suspend fun isSerialNumberTaken(serialNumber: String, excludeId: String = ""): Result<Boolean>

    // ===== MAINTENANCE OPERATIONS =====

    suspend fun getIslandsRequiringMaintenance(currentTime: Instant? = null): Result<List<Island>>
    suspend fun getIslandsUnderWarranty(currentTime: Instant? = null): Result<List<Island>>
    suspend fun updateMaintenanceDate(islandId: String, maintenanceDate: Instant): Result<Unit>
    suspend fun updateOperatingHours(islandId: String, operatingHours: Int): Result<Unit>
    suspend fun updateCycleCount(islandId: String, cycleCount: Long): Result<Unit>

    // ===== STATISTICS =====

    suspend fun getActiveIslandsCount(): Result<Int>
    suspend fun getIslandsCountByFacility(facilityId: String): Result<Int>
    suspend fun getIslandsCountByType(islandType: IslandType): Result<Int>
    suspend fun getIslandTypeStats(): Result<Map<IslandType, Int>>
    suspend fun getMaintenanceStats(): Result<Map<String, Int>>

    // ===== CLIENT AGGREGATION =====

    suspend fun getIslandsByClient(clientId: String): Result<List<Island>>
    suspend fun getIslandsCountByClient(clientId: String): Result<Int>

    // ===== BULK OPERATIONS =====

    suspend fun createIslands(islands: List<Island>): Result<Unit>
    suspend fun bulkUpdateMaintenanceDates(updates: Map<String, Instant>): Result<Unit>
    suspend fun markIslandDeleted(id: String): Result<Unit>
}