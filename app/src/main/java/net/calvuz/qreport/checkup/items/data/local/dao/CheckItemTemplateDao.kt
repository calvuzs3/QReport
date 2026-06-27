package net.calvuz.qreport.checkup.items.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.items.data.local.entity.CheckItemTemplateEntity

@Dao
interface CheckItemTemplateDao {

    @Query("SELECT * FROM check_item_templates WHERE is_active = 1 ORDER BY order_index ASC")
    fun observeActiveTemplates(): Flow<List<CheckItemTemplateEntity>>

    @Query("SELECT * FROM check_item_templates ORDER BY order_index ASC")
    fun observeAllTemplates(): Flow<List<CheckItemTemplateEntity>>

    @Query("SELECT * FROM check_item_templates ORDER BY order_index ASC")
    suspend fun getAllTemplates(): List<CheckItemTemplateEntity>

    @Query("SELECT * FROM check_item_templates WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CheckItemTemplateEntity?

    /** Active templates belonging to one of the given modules, for checkup creation. */
    @Query(
        """
        SELECT * FROM check_item_templates
        WHERE is_active = 1 AND module_type_id IN (:moduleTypeIds)
        ORDER BY order_index ASC
        """
    )
    suspend fun getTemplatesForModuleTypes(moduleTypeIds: List<String>): List<CheckItemTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: CheckItemTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(template: CheckItemTemplateEntity)

    @Query("UPDATE check_item_templates SET is_active = 0, updated_at = :ts WHERE id = :id")
    suspend fun deactivate(id: String, ts: Long)

    @Query("UPDATE check_item_templates SET is_active = 1, updated_at = :ts WHERE id = :id")
    suspend fun restore(id: String, ts: Long)

    // ===== SYNC =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(templates: List<CheckItemTemplateEntity>)

    @Query("""
        SELECT * FROM check_item_templates
        WHERE updated_at > COALESCE(synced_at, 0)
        ORDER BY updated_at ASC
    """)
    suspend fun getPendingSync(): List<CheckItemTemplateEntity>

    @Query("UPDATE check_item_templates SET synced_at = :now WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, now: Long)
}
