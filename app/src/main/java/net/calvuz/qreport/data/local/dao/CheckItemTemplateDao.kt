package net.calvuz.qreport.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import net.calvuz.qreport.data.local.entity.CheckItemTemplateEntity

@Dao
interface CheckItemTemplateDao {

    @Query("SELECT * FROM check_item_templates ORDER BY order_index ASC")
    suspend fun getAllTemplates(): List<CheckItemTemplateEntity>

    @Query("SELECT * FROM check_item_templates WHERE module_type = :moduleType ORDER BY order_index ASC")
    suspend fun getTemplatesByModule(moduleType: String): List<CheckItemTemplateEntity>

    @Query("SELECT * FROM check_item_templates WHERE id = :id")
    suspend fun getTemplateById(id: String): CheckItemTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: CheckItemTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<CheckItemTemplateEntity>)

    @Update
    suspend fun updateTemplate(template: CheckItemTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: CheckItemTemplateEntity)

    @Query("DELETE FROM check_item_templates")
    suspend fun deleteAllTemplates()

    @Query("SELECT COUNT(*) FROM check_item_templates")
    suspend fun getTemplateCount(): Int

    @Query("SELECT DISTINCT module_type FROM check_item_templates ORDER BY module_type")
    suspend fun getModuleTypes(): List<String>
}