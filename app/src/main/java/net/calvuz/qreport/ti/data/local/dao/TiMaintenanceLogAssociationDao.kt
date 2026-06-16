package net.calvuz.qreport.ti.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.ti.data.local.entity.TiMaintenanceLogAssociationEntity

@Dao
interface TiMaintenanceLogAssociationDao {

    // ===== CRUD BASE =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(association: TiMaintenanceLogAssociationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(associations: List<TiMaintenanceLogAssociationEntity>)

    @Delete
    suspend fun delete(association: TiMaintenanceLogAssociationEntity)

    // ===== DELETE BY RELATION =====

    @Query("DELETE FROM ti_maintenance_log_associations WHERE intervention_id = :interventionId")
    suspend fun deleteByInterventionId(interventionId: String): Int

    @Query("DELETE FROM ti_maintenance_log_associations WHERE maintenance_log_id = :logId")
    suspend fun deleteByLogId(logId: String): Int

    @Query("DELETE FROM ti_maintenance_log_associations WHERE intervention_id = :interventionId AND maintenance_log_id = :logId")
    suspend fun deleteAssociation(interventionId: String, logId: String): Int

    // ===== SINGLE QUERIES =====

    @Query("SELECT * FROM ti_maintenance_log_associations WHERE id = :id")
    suspend fun getById(id: String): TiMaintenanceLogAssociationEntity?

    @Query("SELECT * FROM ti_maintenance_log_associations WHERE intervention_id = :interventionId AND maintenance_log_id = :logId")
    suspend fun getAssociation(interventionId: String, logId: String): TiMaintenanceLogAssociationEntity?

    // ===== LIST QUERIES =====

    @Query("SELECT * FROM ti_maintenance_log_associations WHERE intervention_id = :interventionId AND is_deleted = 0 ORDER BY created_at ASC")
    suspend fun getByInterventionId(interventionId: String): List<TiMaintenanceLogAssociationEntity>

    @Query("SELECT * FROM ti_maintenance_log_associations WHERE intervention_id = :interventionId AND is_deleted = 0 ORDER BY created_at ASC")
    fun observeByInterventionId(interventionId: String): Flow<List<TiMaintenanceLogAssociationEntity>>

    @Query("SELECT * FROM ti_maintenance_log_associations WHERE maintenance_log_id = :logId AND is_deleted = 0 ORDER BY created_at DESC")
    suspend fun getByLogId(logId: String): List<TiMaintenanceLogAssociationEntity>

    // ===== IDS EXTRACTION =====

    @Query("SELECT maintenance_log_id FROM ti_maintenance_log_associations WHERE intervention_id = :interventionId AND is_deleted = 0")
    suspend fun getLogIdsByIntervention(interventionId: String): List<String>

    @Query("SELECT intervention_id FROM ti_maintenance_log_associations WHERE maintenance_log_id = :logId AND is_deleted = 0")
    suspend fun getInterventionIdsByLog(logId: String): List<String>

    // ===== VALIDATION QUERIES =====

    @Query("SELECT EXISTS(SELECT 1 FROM ti_maintenance_log_associations WHERE intervention_id = :interventionId AND maintenance_log_id = :logId AND is_deleted = 0)")
    suspend fun exists(interventionId: String, logId: String): Boolean

    @Query("SELECT COUNT(*) FROM ti_maintenance_log_associations WHERE intervention_id = :interventionId AND is_deleted = 0")
    suspend fun getLogCount(interventionId: String): Int

    // ============================================================
    // BACKUP METHODS
    // ============================================================

    @Query("SELECT * FROM ti_maintenance_log_associations ORDER BY created_at ASC")
    suspend fun getAllForBackup(): List<TiMaintenanceLogAssociationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(associations: List<TiMaintenanceLogAssociationEntity>)

    @Query("DELETE FROM ti_maintenance_log_associations")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM ti_maintenance_log_associations")
    suspend fun count(): Int
}
