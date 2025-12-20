package net.calvuz.qreport.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.data.local.entity.CheckUpIslandAssociationEntity

@Dao
interface CheckUpAssociationDao {

    // ===== CRUD BASE =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(association: CheckUpIslandAssociationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(associations: List<CheckUpIslandAssociationEntity>)

    @Update
    suspend fun update(association: CheckUpIslandAssociationEntity)

    @Delete
    suspend fun delete(association: CheckUpIslandAssociationEntity)

    // ===== DELETE BY RELATION =====

    @Query("DELETE FROM checkup_island_associations WHERE checkup_id = :checkupId")
    suspend fun deleteByCheckUpId(checkupId: String): Int

    @Query("DELETE FROM checkup_island_associations WHERE island_id = :islandId")
    suspend fun deleteByIslandId(islandId: String): Int

    @Query("DELETE FROM checkup_island_associations WHERE checkup_id = :checkupId AND island_id = :islandId")
    suspend fun deleteAssociation(checkupId: String, islandId: String): Int

    // ===== SINGLE QUERIES =====

    @Query("SELECT * FROM checkup_island_associations WHERE id = :id")
    suspend fun getById(id: String): CheckUpIslandAssociationEntity?

    @Query("SELECT * FROM checkup_island_associations WHERE checkup_id = :checkupId AND island_id = :islandId")
    suspend fun getAssociation(checkupId: String, islandId: String): CheckUpIslandAssociationEntity?

    // ===== LIST QUERIES =====

    @Query("SELECT * FROM checkup_island_associations WHERE checkup_id = :checkupId ORDER BY created_at ASC")
    suspend fun getByCheckUpId(checkupId: String): List<CheckUpIslandAssociationEntity>

    @Query("SELECT * FROM checkup_island_associations WHERE checkup_id = :checkupId ORDER BY created_at ASC")
    fun observeByCheckUpId(checkupId: String): Flow<List<CheckUpIslandAssociationEntity>>

    @Query("SELECT * FROM checkup_island_associations WHERE island_id = :islandId ORDER BY created_at DESC")
    suspend fun getByIslandId(islandId: String): List<CheckUpIslandAssociationEntity>

    @Query("SELECT * FROM checkup_island_associations WHERE island_id = :islandId ORDER BY created_at DESC")
    fun observeByIslandId(islandId: String): Flow<List<CheckUpIslandAssociationEntity>>

    // ===== IDS EXTRACTION =====

    @Query("SELECT island_id FROM checkup_island_associations WHERE checkup_id = :checkupId")
    suspend fun getIslandIdsByCheckUp(checkupId: String): List<String>

    @Query("SELECT checkup_id FROM checkup_island_associations WHERE island_id = :islandId")
    suspend fun getCheckUpIdsByIsland(islandId: String): List<String>

    // ===== VALIDATION QUERIES =====

    @Query("SELECT EXISTS(SELECT 1 FROM checkup_island_associations WHERE checkup_id = :checkupId AND island_id = :islandId)")
    suspend fun exists(checkupId: String, islandId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM checkup_island_associations WHERE checkup_id = :checkupId)")
    suspend fun hasAssociations(checkupId: String): Boolean

    @Query("SELECT COUNT(*) FROM checkup_island_associations WHERE checkup_id = :checkupId")
    suspend fun getAssociationCount(checkupId: String): Int

    // ===== STATISTICS =====

    @Query("SELECT COUNT(*) FROM checkup_island_associations WHERE island_id = :islandId")
    suspend fun getCheckUpCountForIsland(islandId: String): Int

    @Query("""
        SELECT COUNT(DISTINCT cia.checkup_id)
        FROM checkup_island_associations cia
        INNER JOIN facility_islands fi ON cia.island_id = fi.id
        INNER JOIN facilities f ON fi.facility_id = f.id
        WHERE f.client_id = :clientId
    """)
    suspend fun getCheckUpCountForClient(clientId: String): Int

    // ===== ADVANCED QUERIES =====

    // CheckUp senza associazioni (generici)
    @Query("""
        SELECT cur.id
        FROM checkups cur
        LEFT JOIN checkup_island_associations cia ON cur.id = cia.checkup_id
        WHERE cia.checkup_id IS NULL
        ORDER BY cur.created_at DESC
    """)
    suspend fun getUnassociatedCheckUpIds(): List<String>

    @Query("""
        SELECT cur.id
        FROM checkups cur
        LEFT JOIN checkup_island_associations cia ON cur.id = cia.checkup_id
        WHERE cia.checkup_id IS NULL
        ORDER BY cur.created_at DESC
    """)
    fun observeUnassociatedCheckUpIds(): Flow<List<String>>

    // CheckUp recenti per isola
    @Query("""
        SELECT cia.*
        FROM checkup_island_associations cia
        INNER JOIN checkups cur ON cia.checkup_id = cur.id
        WHERE cia.island_id = :islandId
        ORDER BY cur.created_at DESC
        LIMIT :limit
    """)
    suspend fun getRecentAssociationsForIsland(islandId: String, limit: Int = 10): List<CheckUpIslandAssociationEntity>

    // CheckUp per cliente (attraverso facilities e islands)
    @Query("""
        SELECT cia.*
        FROM checkup_island_associations cia
        INNER JOIN facility_islands fi ON cia.island_id = fi.id
        INNER JOIN facilities f ON fi.facility_id = f.id
        INNER JOIN checkups cur ON cia.checkup_id = cur.id
        WHERE f.client_id = :clientId
        ORDER BY cur.created_at DESC
        LIMIT :limit
    """)
    suspend fun getRecentAssociationsForClient(clientId: String, limit: Int = 20): List<CheckUpIslandAssociationEntity>
}