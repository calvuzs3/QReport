package net.calvuz.qreport.checkup.modules.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.modules.data.local.entity.ModuleTypeEntity
import net.calvuz.qreport.checkup.modules.data.local.entity.ModuleTypeIslandTypeCrossRef

@Dao
interface ModuleTypeDao {

    @Query("SELECT * FROM module_types WHERE is_active = 1 ORDER BY sort_order ASC")
    fun observeActiveModuleTypes(): Flow<List<ModuleTypeEntity>>

    @Query("SELECT * FROM module_types ORDER BY sort_order ASC")
    fun observeAllModuleTypes(): Flow<List<ModuleTypeEntity>>

    @Query("SELECT * FROM module_types ORDER BY sort_order ASC")
    suspend fun getAllModuleTypes(): List<ModuleTypeEntity>

    @Query("SELECT * FROM module_types WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): ModuleTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(type: ModuleTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(type: ModuleTypeEntity)

    @Query("UPDATE module_types SET is_active = 0, updated_at = :ts WHERE id = :id")
    suspend fun deactivate(id: String, ts: Long)

    @Query("UPDATE module_types SET is_active = 1, updated_at = :ts WHERE id = :id")
    suspend fun restore(id: String, ts: Long)

    /** Which modules a given island type pulls into a new checkup's checklist. */
    @Query("SELECT module_type_id FROM module_type_island_types WHERE island_type_id = :islandTypeId")
    suspend fun getModuleTypeIdsForIslandType(islandTypeId: String): List<String>

    @Query("SELECT * FROM module_type_island_types")
    fun observeAllModuleIslandLinks(): Flow<List<ModuleTypeIslandTypeCrossRef>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertModuleIslandLinks(links: List<ModuleTypeIslandTypeCrossRef>)

    @Query("DELETE FROM module_type_island_types WHERE island_type_id = :islandTypeId")
    suspend fun deleteModuleIslandLinksForIslandType(islandTypeId: String)

    @Transaction
    suspend fun replaceModuleIslandLinks(islandTypeId: String, moduleTypeIds: List<String>) {
        deleteModuleIslandLinksForIslandType(islandTypeId)
        insertModuleIslandLinks(moduleTypeIds.map { ModuleTypeIslandTypeCrossRef(islandTypeId, it) })
    }

    // ===== SYNC =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(types: List<ModuleTypeEntity>)

    @Query("""
        SELECT * FROM module_types
        WHERE updated_at > COALESCE(synced_at, 0)
        ORDER BY updated_at ASC
    """)
    suspend fun getPendingSync(): List<ModuleTypeEntity>

    @Query("UPDATE module_types SET synced_at = :now WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, now: Long)
}
