package net.calvuz.qreport.checkup.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.data.local.entity.CheckUpMaintenanceLogAssociationEntity

@Dao
interface CheckUpMaintenanceLogAssociationDao {

    // ===== CRUD BASE =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(association: CheckUpMaintenanceLogAssociationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(associations: List<CheckUpMaintenanceLogAssociationEntity>)

    @Delete
    suspend fun delete(association: CheckUpMaintenanceLogAssociationEntity)

    // ===== DELETE BY RELATION =====

    @Query("DELETE FROM checkup_maintenance_log_associations WHERE checkup_id = :checkupId")
    suspend fun deleteByCheckUpId(checkupId: String): Int

    @Query("DELETE FROM checkup_maintenance_log_associations WHERE maintenance_log_id = :logId")
    suspend fun deleteByLogId(logId: String): Int

    @Query("DELETE FROM checkup_maintenance_log_associations WHERE checkup_id = :checkupId AND maintenance_log_id = :logId")
    suspend fun deleteAssociation(checkupId: String, logId: String): Int

    // ===== SINGLE QUERIES =====

    @Query("SELECT * FROM checkup_maintenance_log_associations WHERE id = :id")
    suspend fun getById(id: String): CheckUpMaintenanceLogAssociationEntity?

    @Query("SELECT * FROM checkup_maintenance_log_associations WHERE checkup_id = :checkupId AND maintenance_log_id = :logId")
    suspend fun getAssociation(checkupId: String, logId: String): CheckUpMaintenanceLogAssociationEntity?

    // ===== LIST QUERIES =====

    @Query("SELECT * FROM checkup_maintenance_log_associations WHERE checkup_id = :checkupId AND is_deleted = 0 ORDER BY created_at ASC")
    suspend fun getByCheckUpId(checkupId: String): List<CheckUpMaintenanceLogAssociationEntity>

    @Query("SELECT * FROM checkup_maintenance_log_associations WHERE checkup_id = :checkupId AND is_deleted = 0 ORDER BY created_at ASC")
    fun observeByCheckUpId(checkupId: String): Flow<List<CheckUpMaintenanceLogAssociationEntity>>

    @Query("SELECT * FROM checkup_maintenance_log_associations WHERE maintenance_log_id = :logId AND is_deleted = 0 ORDER BY created_at DESC")
    suspend fun getByLogId(logId: String): List<CheckUpMaintenanceLogAssociationEntity>

    // ===== IDS EXTRACTION =====

    @Query("SELECT maintenance_log_id FROM checkup_maintenance_log_associations WHERE checkup_id = :checkupId AND is_deleted = 0")
    suspend fun getLogIdsByCheckUp(checkupId: String): List<String>

    @Query("SELECT checkup_id FROM checkup_maintenance_log_associations WHERE maintenance_log_id = :logId AND is_deleted = 0")
    suspend fun getCheckUpIdsByLog(logId: String): List<String>

    // ===== VALIDATION QUERIES =====

    @Query("SELECT EXISTS(SELECT 1 FROM checkup_maintenance_log_associations WHERE checkup_id = :checkupId AND maintenance_log_id = :logId AND is_deleted = 0)")
    suspend fun exists(checkupId: String, logId: String): Boolean

    @Query("SELECT COUNT(*) FROM checkup_maintenance_log_associations WHERE checkup_id = :checkupId AND is_deleted = 0")
    suspend fun getLogCount(checkupId: String): Int

    // ============================================================
    // BACKUP METHODS
    // ============================================================

    @Query("SELECT * FROM checkup_maintenance_log_associations ORDER BY created_at ASC")
    suspend fun getAllForBackup(): List<CheckUpMaintenanceLogAssociationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(associations: List<CheckUpMaintenanceLogAssociationEntity>)

    @Query("DELETE FROM checkup_maintenance_log_associations")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM checkup_maintenance_log_associations")
    suspend fun count(): Int
}
