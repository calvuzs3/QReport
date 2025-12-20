package net.calvuz.qreport.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.data.local.entity.FacilityIslandEntity

/**
 * DAO per gestione isole robotizzate per stabilimento
 * Definisce tutte le operazioni CRUD e query complesse per FacilityIslandEntity
 *
 * ✅ FIXED: Tutte le query e data classes corrette per compilazione
 */
@Dao
interface FacilityIslandDao {

    // ===== BASIC CRUD =====

    @Query("SELECT * FROM facility_islands WHERE id = :id")
    suspend fun getIslandById(id: String): FacilityIslandEntity?

    @Query("SELECT * FROM facility_islands WHERE id IN (:ids) AND is_active = 1")
    suspend fun getIslandsByIds(ids: List<String>): List<FacilityIslandEntity>

    @Query("SELECT * FROM facility_islands WHERE id = :id")
    fun getIslandByIdFlow(id: String): Flow<FacilityIslandEntity?>

    @Query("SELECT * FROM facility_islands WHERE facility_id = :facilityId AND is_active = 1 ORDER BY custom_name ASC, serial_number ASC")
    suspend fun getIslandsForFacility(facilityId: String): List<FacilityIslandEntity>

    @Query("SELECT * FROM facility_islands WHERE facility_id = :facilityId AND is_active = 1 ORDER BY custom_name ASC, serial_number ASC")
    fun getIslandsForFacilityFlow(facilityId: String): Flow<List<FacilityIslandEntity>>

    @Query("SELECT * FROM facility_islands WHERE serial_number = :serialNumber")
    suspend fun getIslandBySerialNumber(serialNumber: String): FacilityIslandEntity?

    @Query("SELECT * FROM facility_islands WHERE is_active = 1 ORDER BY serial_number ASC")
    suspend fun getAllActiveIslands(): List<FacilityIslandEntity>

    @Query("SELECT * FROM facility_islands WHERE is_active = 1 ORDER BY serial_number ASC")
    fun getAllActiveIslandsFlow(): Flow<List<FacilityIslandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIsland(island: FacilityIslandEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIslands(islands: List<FacilityIslandEntity>)

    @Update
    suspend fun updateIsland(island: FacilityIslandEntity)

    @Delete
    suspend fun deleteIsland(island: FacilityIslandEntity)

    @Query("UPDATE facility_islands SET is_active = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun softDeleteIsland(id: String, timestamp: Long = System.currentTimeMillis())

    // ===== SEARCH & FILTER =====

    @Query("""
        SELECT fi.* FROM facility_islands fi
        INNER JOIN facilities f ON fi.facility_id = f.id
        INNER JOIN clients c ON f.client_id = c.id
        WHERE fi.is_active = 1 AND f.is_active = 1 AND c.is_active = 1
        AND (fi.serial_number LIKE '%' || :query || '%' 
             OR fi.custom_name LIKE '%' || :query || '%'
             OR fi.model LIKE '%' || :query || '%'
             OR fi.location LIKE '%' || :query || '%'
             OR c.company_name LIKE '%' || :query || '%'
             OR f.name LIKE '%' || :query || '%')
        ORDER BY fi.serial_number ASC
    """)
    suspend fun searchIslands(query: String): List<FacilityIslandEntity>

    @Query("SELECT * FROM facility_islands WHERE island_type = :islandType AND is_active = 1 ORDER BY serial_number ASC")
    suspend fun getIslandsByType(islandType: String): List<FacilityIslandEntity>

    @Query("SELECT DISTINCT island_type FROM facility_islands WHERE is_active = 1 ORDER BY island_type ASC")
    suspend fun getAllIslandTypes(): List<String>

    @Query("SELECT DISTINCT model FROM facility_islands WHERE model IS NOT NULL AND is_active = 1 ORDER BY model ASC")
    suspend fun getAllModels(): List<String>

    // ===== CLIENT-BASED QUERIES =====

    @Query("""
        SELECT fi.* FROM facility_islands fi
        INNER JOIN facilities f ON fi.facility_id = f.id
        WHERE f.client_id = :clientId AND fi.is_active = 1 AND f.is_active = 1
        ORDER BY f.name ASC, fi.serial_number ASC
    """)
    suspend fun getIslandsForClient(clientId: String): List<FacilityIslandEntity>

    @Query("""
        SELECT fi.* FROM facility_islands fi
        INNER JOIN facilities f ON fi.facility_id = f.id
        WHERE f.client_id = :clientId AND fi.island_type = :islandType AND fi.is_active = 1 AND f.is_active = 1
        ORDER BY f.name ASC, fi.serial_number ASC
    """)
    suspend fun getIslandsByTypeForClient(clientId: String, islandType: String): List<FacilityIslandEntity>

    // ===== MAINTENANCE QUERIES =====

    @Query("""
        SELECT * FROM facility_islands 
        WHERE is_active = 1 
        AND next_scheduled_maintenance IS NOT NULL
        AND next_scheduled_maintenance <= :currentTimestamp
        ORDER BY next_scheduled_maintenance ASC
    """)
    suspend fun getIslandsDueMaintenance(currentTimestamp: Long): List<FacilityIslandEntity>

    @Query("""
        SELECT * FROM facility_islands 
        WHERE is_active = 1 
        AND next_scheduled_maintenance IS NOT NULL
        AND next_scheduled_maintenance BETWEEN :startTimestamp AND :endTimestamp
        ORDER BY next_scheduled_maintenance ASC
    """)
    suspend fun getIslandsMaintenanceInPeriod(startTimestamp: Long, endTimestamp: Long): List<FacilityIslandEntity>

    @Query("""
        SELECT * FROM facility_islands 
        WHERE facility_id = :facilityId AND is_active = 1 
        AND next_scheduled_maintenance IS NOT NULL
        AND next_scheduled_maintenance <= :currentTimestamp
        ORDER BY next_scheduled_maintenance ASC
    """)
    suspend fun getIslandsDueMaintenanceForFacility(facilityId: String, currentTimestamp: Long): List<FacilityIslandEntity>

    @Query("UPDATE facility_islands SET last_maintenance_date = :maintenanceDate, updated_at = :timestamp WHERE id = :islandId")
    suspend fun updateLastMaintenanceDate(islandId: String, maintenanceDate: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE facility_islands SET next_scheduled_maintenance = :nextMaintenance, updated_at = :timestamp WHERE id = :islandId")
    suspend fun updateNextScheduledMaintenance(islandId: String, nextMaintenance: Long?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE facility_islands SET operating_hours = :operatingHours, updated_at = :timestamp WHERE id = :islandId")
    suspend fun updateOperatingHours(islandId: String, operatingHours: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE facility_islands SET cycle_count = :cycleCount, updated_at = :timestamp WHERE id = :islandId")
    suspend fun updateCycleCount(islandId: String, cycleCount: Long, timestamp: Long = System.currentTimeMillis())

    // ===== WARRANTY QUERIES =====

    @Query("""
        SELECT * FROM facility_islands 
        WHERE is_active = 1 
        AND warranty_expiration IS NOT NULL
        AND warranty_expiration > :currentTimestamp
        ORDER BY warranty_expiration ASC
    """)
    suspend fun getIslandsUnderWarranty(currentTimestamp: Long): List<FacilityIslandEntity>

    @Query("""
        SELECT * FROM facility_islands 
        WHERE is_active = 1 
        AND warranty_expiration IS NOT NULL
        AND warranty_expiration <= :currentTimestamp
        ORDER BY warranty_expiration DESC
    """)
    suspend fun getIslandsExpiredWarranty(currentTimestamp: Long): List<FacilityIslandEntity>

    @Query("""
        SELECT * FROM facility_islands 
        WHERE is_active = 1 
        AND warranty_expiration IS NOT NULL
        AND warranty_expiration BETWEEN :currentTimestamp AND :warningTimestamp
        ORDER BY warranty_expiration ASC
    """)
    suspend fun getIslandsWarrantyExpiringSoon(currentTimestamp: Long, warningTimestamp: Long): List<FacilityIslandEntity>

    // ===== STATISTICS =====

    @Query("SELECT COUNT(*) FROM facility_islands WHERE facility_id = :facilityId AND is_active = 1")
    suspend fun getIslandsCountForFacility(facilityId: String): Int

    @Query("SELECT COUNT(*) FROM facility_islands WHERE is_active = 1")
    suspend fun getActiveIslandsCount(): Int

    @Query("SELECT COUNT(*) FROM facility_islands WHERE island_type = :islandType AND is_active = 1")
    suspend fun getIslandsCountByType(islandType: String): Int

    @Query("""
        SELECT COUNT(*) FROM facility_islands fi
        INNER JOIN facilities f ON fi.facility_id = f.id
        WHERE f.client_id = :clientId AND fi.is_active = 1 AND f.is_active = 1
    """)
    suspend fun getIslandsCountForClient(clientId: String): Int

    @Query("SELECT AVG(operating_hours) FROM facility_islands WHERE island_type = :islandType AND is_active = 1")
    suspend fun getAverageOperatingHoursByType(islandType: String): Double?

    @Query("SELECT SUM(cycle_count) FROM facility_islands WHERE facility_id = :facilityId AND is_active = 1")
    suspend fun getTotalCycleCountForFacility(facilityId: String): Long?

    // ===== VALIDATION =====

    @Query("SELECT COUNT(*) > 0 FROM facility_islands WHERE serial_number = :serialNumber AND id != :excludeId")
    suspend fun isSerialNumberTaken(serialNumber: String, excludeId: String = ""): Boolean

    @Query("SELECT COUNT(*) > 0 FROM facility_islands WHERE facility_id = :facilityId AND custom_name = :customName AND id != :excludeId AND is_active = 1")
    suspend fun isCustomNameTakenForFacility(facilityId: String, customName: String, excludeId: String = ""): Boolean

    // ===== COMPLEX QUERIES =====

    @Query("""
        SELECT fi.id,
               fi.facility_id as facilityId,
               fi.island_type as islandType,
               fi.serial_number as serialNumber,
               fi.model,
               fi.installation_date as installationDate,
               fi.warranty_expiration as warrantyExpiration,
               fi.is_active as isActive,
               fi.operating_hours as operatingHours,
               fi.cycle_count as cycleCount,
               fi.last_maintenance_date as lastMaintenanceDate,
               fi.next_scheduled_maintenance as nextScheduledMaintenance,
               fi.custom_name as customName,
               fi.location,
               fi.notes,
               fi.created_at as createdAt,
               fi.updated_at as updatedAt,
               f.name as facilityName,
               c.company_name as companyName
        FROM facility_islands fi
        INNER JOIN facilities f ON fi.facility_id = f.id
        INNER JOIN clients c ON f.client_id = c.id
        WHERE fi.is_active = 1 AND f.is_active = 1 AND c.is_active = 1
        ORDER BY c.company_name ASC, f.name ASC, fi.serial_number ASC
    """)
    suspend fun getIslandsWithFacilityAndClient(): List<IslandWithFacilityAndClientResult>

    @Query("""
        SELECT island_type, COUNT(*) as count
        FROM facility_islands 
        WHERE is_active = 1 
        GROUP BY island_type
        ORDER BY count DESC, island_type ASC
    """)
    suspend fun getIslandTypeStatistics(): List<IslandTypeStatistics>

    // ===== MAINTENANCE =====

    @Query("UPDATE facility_islands SET updated_at = :timestamp WHERE id = :id")
    suspend fun touchIsland(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM facility_islands WHERE is_active = 0 AND updated_at < :cutoffTimestamp")
    suspend fun permanentlyDeleteInactiveIslands(cutoffTimestamp: Long): Int

    // ===== BATCH OPERATIONS =====

    @Query("UPDATE facility_islands SET location = :newLocation, updated_at = :timestamp WHERE facility_id = :facilityId AND location = :oldLocation")
    suspend fun updateLocationForFacility(facilityId: String, oldLocation: String, newLocation: String, timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun performMaintenanceUpdate(islandId: String, currentTimestamp: Long, nextMaintenanceTimestamp: Long?) {
        updateLastMaintenanceDate(islandId, currentTimestamp)
        updateNextScheduledMaintenance(islandId, nextMaintenanceTimestamp)
        touchIsland(islandId, currentTimestamp)
    }
}

/**
 * Result class per query con facility e client - ✅ CORRETTA con alias
 */
data class IslandWithFacilityAndClientResult(
    val id: String,
    val facilityId: String,
    val islandType: String,
    val serialNumber: String,
    val model: String?,
    val installationDate: Long?,
    val warrantyExpiration: Long?,
    val isActive: Boolean,
    val operatingHours: Int,
    val cycleCount: Long,
    val lastMaintenanceDate: Long?,
    val nextScheduledMaintenance: Long?,
    val customName: String?,
    val location: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val facilityName: String,
    val companyName: String
)

/**
 * Statistiche per tipo di isola - ✅ CORRETTA
 */
data class IslandTypeStatistics(
    val island_type: String,  // ✅ Snake case per match SQL
    val count: Int
)