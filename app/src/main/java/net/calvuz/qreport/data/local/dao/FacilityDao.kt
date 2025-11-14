package net.calvuz.qreport.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.data.local.entity.FacilityEntity

/**
 * DAO per gestione stabilimenti
 * Definisce tutte le operazioni CRUD e query complesse per FacilityEntity
 */
@Dao
interface FacilityDao {

    // ===== BASIC CRUD =====

    @Query("SELECT * FROM facilities WHERE id = :id")
    suspend fun getFacilityById(id: String): FacilityEntity?

    @Query("SELECT * FROM facilities WHERE id = :id")
    fun getFacilityByIdFlow(id: String): Flow<FacilityEntity?>

    @Query("SELECT * FROM facilities WHERE client_id = :clientId AND is_active = 1 ORDER BY is_primary DESC, name ASC")
    suspend fun getFacilitiesForClient(clientId: String): List<FacilityEntity>

    @Query("SELECT * FROM facilities WHERE client_id = :clientId AND is_active = 1 ORDER BY is_primary DESC, name ASC")
    fun getFacilitiesForClientFlow(clientId: String): Flow<List<FacilityEntity>>

    @Query("SELECT * FROM facilities WHERE is_active = 1 ORDER BY name ASC")
    suspend fun getAllActiveFacilities(): List<FacilityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacility(facility: FacilityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacilities(facilities: List<FacilityEntity>)

    @Update
    suspend fun updateFacility(facility: FacilityEntity)

    @Delete
    suspend fun deleteFacility(facility: FacilityEntity)

    @Query("UPDATE facilities SET is_active = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun softDeleteFacility(id: String, timestamp: Long = System.currentTimeMillis())

    // ===== PRIMARY FACILITY MANAGEMENT =====

    @Query("SELECT * FROM facilities WHERE client_id = :clientId AND is_primary = 1 AND is_active = 1")
    suspend fun getPrimaryFacility(clientId: String): FacilityEntity?

    @Transaction
    suspend fun setPrimaryFacility(clientId: String, facilityId: String) {
        // Rimuovi flag primary da tutte le altre facilities del cliente
        clearPrimaryFacility(clientId)
        // Imposta come primary la facility specificata
        setPrimaryFlag(facilityId, true, System.currentTimeMillis())
    }

    @Query("UPDATE facilities SET is_primary = 0, updated_at = :timestamp WHERE client_id = :clientId")
    suspend fun clearPrimaryFacility(clientId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE facilities SET is_primary = :isPrimary, updated_at = :timestamp WHERE id = :facilityId")
    suspend fun setPrimaryFlag(facilityId: String, isPrimary: Boolean, timestamp: Long)

    // ===== SEARCH & FILTER =====

    @Query("""
        SELECT * FROM facilities f
        INNER JOIN clients c ON f.client_id = c.id
        WHERE f.is_active = 1 AND c.is_active = 1
        AND (f.name LIKE '%' || :query || '%' 
             OR f.code LIKE '%' || :query || '%'
             OR c.company_name LIKE '%' || :query || '%')
        ORDER BY f.name ASC
    """)
    suspend fun searchFacilities(query: String): List<FacilityEntity>

    @Query("SELECT * FROM facilities WHERE facility_type = :facilityType AND is_active = 1 ORDER BY name ASC")
    suspend fun getFacilitiesByType(facilityType: String): List<FacilityEntity>

    @Query("SELECT DISTINCT facility_type FROM facilities WHERE is_active = 1 ORDER BY facility_type ASC")
    suspend fun getAllFacilityTypes(): List<String>

    // ===== STATISTICS =====

    @Query("SELECT COUNT(*) FROM facilities WHERE client_id = :clientId AND is_active = 1")
    suspend fun getFacilitiesCountForClient(clientId: String): Int

    @Query("SELECT COUNT(*) FROM facilities WHERE is_active = 1")
    suspend fun getActiveFacilitiesCount(): Int

    @Query("SELECT COUNT(*) FROM facilities WHERE facility_type = :facilityType AND is_active = 1")
    suspend fun getFacilitiesCountByType(facilityType: String): Int

    @Query("""
        SELECT COUNT(*) FROM facility_islands fi
        INNER JOIN facilities f ON fi.facility_id = f.id
        WHERE f.id = :facilityId AND f.is_active = 1 AND fi.is_active = 1
    """)
    suspend fun getIslandsCountForFacility(facilityId: String): Int

    // ===== VALIDATION =====

    @Query("SELECT COUNT(*) > 0 FROM facilities WHERE client_id = :clientId AND name = :name AND id != :excludeId AND is_active = 1")
    suspend fun isFacilityNameTakenForClient(clientId: String, name: String, excludeId: String = ""): Boolean

    @Query("SELECT COUNT(*) > 0 FROM facilities WHERE client_id = :clientId AND code = :code AND id != :excludeId AND is_active = 1")
    suspend fun isFacilityCodeTakenForClient(clientId: String, code: String, excludeId: String = ""): Boolean

    // ===== COMPLEX QUERIES =====

    @Query("""
        SELECT f.*, COUNT(fi.id) as islands_count
        FROM facilities f
        LEFT JOIN facility_islands fi ON f.id = fi.facility_id AND fi.is_active = 1
        WHERE f.client_id = :clientId AND f.is_active = 1
        GROUP BY f.id
        ORDER BY f.is_primary DESC, f.name ASC
    """)
    suspend fun getFacilitiesWithIslandCount(clientId: String): List<FacilityWithIslandCountResult>

    @Query("""
        SELECT f.* FROM facilities f
        WHERE f.is_active = 1
        AND EXISTS (
            SELECT 1 FROM facility_islands fi 
            WHERE fi.facility_id = f.id AND fi.is_active = 1
        )
        ORDER BY f.name ASC
    """)
    suspend fun getFacilitiesWithIslands(): List<FacilityEntity>

    @Query("""
        SELECT f.* FROM facilities f
        WHERE f.is_active = 1
        AND NOT EXISTS (
            SELECT 1 FROM facility_islands fi 
            WHERE fi.facility_id = f.id AND fi.is_active = 1
        )
        ORDER BY f.name ASC
    """)
    suspend fun getFacilitiesWithoutIslands(): List<FacilityEntity>

    // ===== GEO QUERIES (per future features) =====

    @Query("""
        SELECT * FROM facilities 
        WHERE is_active = 1 
        AND address_json LIKE '%' || :city || '%'
        ORDER BY name ASC
    """)
    suspend fun getFacilitiesByCity(city: String): List<FacilityEntity>

    @Query("""
        SELECT * FROM facilities 
        WHERE is_active = 1 
        AND address_json LIKE '%' || :province || '%'
        ORDER BY name ASC
    """)
    suspend fun getFacilitiesByProvince(province: String): List<FacilityEntity>

    // ===== MAINTENANCE =====

    @Query("UPDATE facilities SET updated_at = :timestamp WHERE id = :id")
    suspend fun touchFacility(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM facilities WHERE is_active = 0 AND updated_at < :cutoffTimestamp")
    suspend fun permanentlyDeleteInactiveFacilities(cutoffTimestamp: Long): Int
}

/**
 * Result class per query con conteggio isole
 */
data class FacilityWithIslandCountResult(
    val id: String,
    val clientId: String,
    val name: String,
    val code: String?,
    val description: String?,
    val facilityType: String,
    val addressJson: String,
    val isPrimary: Boolean,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val islandsCount: Int
)