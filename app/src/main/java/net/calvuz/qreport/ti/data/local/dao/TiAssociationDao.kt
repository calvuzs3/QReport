package net.calvuz.qreport.ti.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.ti.data.local.entity.TiIslandAssociationEntity

@Dao
interface TiAssociationDao {

    // ===== CRUD BASE =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(association: TiIslandAssociationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(associations: List<TiIslandAssociationEntity>)

    @Delete
    suspend fun delete(association: TiIslandAssociationEntity)

    // ===== DELETE BY RELATION =====

    @Query("DELETE FROM ti_island_associations WHERE intervention_id = :interventionId")
    suspend fun deleteByInterventionId(interventionId: String): Int

    @Query("DELETE FROM ti_island_associations WHERE island_id = :islandId")
    suspend fun deleteByIslandId(islandId: String): Int

    @Query("DELETE FROM ti_island_associations WHERE intervention_id = :interventionId AND island_id = :islandId")
    suspend fun deleteAssociation(interventionId: String, islandId: String): Int

    // ===== SINGLE QUERIES =====

    @Query("SELECT * FROM ti_island_associations WHERE id = :id")
    suspend fun getById(id: String): TiIslandAssociationEntity?

    @Query("SELECT * FROM ti_island_associations WHERE intervention_id = :interventionId AND island_id = :islandId")
    suspend fun getAssociation(interventionId: String, islandId: String): TiIslandAssociationEntity?

    // ===== LIST QUERIES =====

    @Query("SELECT * FROM ti_island_associations WHERE intervention_id = :interventionId AND is_deleted = 0 ORDER BY created_at ASC")
    suspend fun getByInterventionId(interventionId: String): List<TiIslandAssociationEntity>

    @Query("SELECT * FROM ti_island_associations WHERE intervention_id = :interventionId AND is_deleted = 0 ORDER BY created_at ASC")
    fun observeByInterventionId(interventionId: String): Flow<List<TiIslandAssociationEntity>>

    @Query("SELECT * FROM ti_island_associations WHERE island_id = :islandId AND is_deleted = 0 ORDER BY created_at DESC")
    suspend fun getByIslandId(islandId: String): List<TiIslandAssociationEntity>

    @Query("SELECT * FROM ti_island_associations WHERE island_id = :islandId AND is_deleted = 0 ORDER BY created_at DESC")
    fun observeByIslandId(islandId: String): Flow<List<TiIslandAssociationEntity>>

    // ===== IDS EXTRACTION =====

    @Query("SELECT island_id FROM ti_island_associations WHERE intervention_id = :interventionId AND is_deleted = 0")
    suspend fun getIslandIdsByIntervention(interventionId: String): List<String>

    @Query("SELECT intervention_id FROM ti_island_associations WHERE island_id = :islandId AND is_deleted = 0")
    suspend fun getInterventionIdsByIsland(islandId: String): List<String>

    // ===== VALIDATION QUERIES =====

    @Query("SELECT EXISTS(SELECT 1 FROM ti_island_associations WHERE intervention_id = :interventionId AND island_id = :islandId AND is_deleted = 0)")
    suspend fun exists(interventionId: String, islandId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM ti_island_associations WHERE intervention_id = :interventionId AND is_deleted = 0)")
    suspend fun hasAssociations(interventionId: String): Boolean

    @Query("SELECT COUNT(*) FROM ti_island_associations WHERE intervention_id = :interventionId AND is_deleted = 0")
    suspend fun getAssociationCount(interventionId: String): Int

    // ===== STATISTICS =====

    @Query("SELECT COUNT(*) FROM ti_island_associations WHERE island_id = :islandId AND is_deleted = 0")
    suspend fun getTiCountForIsland(islandId: String): Int

    @Query("""
        SELECT COUNT(DISTINCT tia.intervention_id)
        FROM ti_island_associations tia
        INNER JOIN facility_islands fi ON tia.island_id = fi.id
        INNER JOIN facilities f ON fi.facility_id = f.id
        WHERE f.client_id = :clientId AND tia.is_deleted = 0
    """)
    suspend fun getTiCountForClient(clientId: String): Int

    // ===== ADVANCED QUERIES =====

    @Query("""
        SELECT tia.*
        FROM ti_island_associations tia
        INNER JOIN technical_interventions ti ON tia.intervention_id = ti.id
        WHERE tia.island_id = :islandId AND tia.is_deleted = 0
        ORDER BY ti.created_at DESC
        LIMIT :limit
    """)
    suspend fun getRecentAssociationsForIsland(islandId: String, limit: Int = 10): List<TiIslandAssociationEntity>

    @Query("""
        SELECT tia.*
        FROM ti_island_associations tia
        INNER JOIN facility_islands fi ON tia.island_id = fi.id
        INNER JOIN facilities f ON fi.facility_id = f.id
        INNER JOIN technical_interventions ti ON tia.intervention_id = ti.id
        WHERE f.client_id = :clientId AND tia.is_deleted = 0
        ORDER BY ti.created_at DESC
        LIMIT :limit
    """)
    suspend fun getRecentAssociationsForClient(clientId: String, limit: Int = 20): List<TiIslandAssociationEntity>

    // ============================================================
    // BACKUP METHODS
    // ============================================================

    @Query("SELECT * FROM ti_island_associations ORDER BY created_at ASC")
    suspend fun getAllForBackup(): List<TiIslandAssociationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(associations: List<TiIslandAssociationEntity>)

    @Query("DELETE FROM ti_island_associations")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM ti_island_associations")
    suspend fun count(): Int
}
