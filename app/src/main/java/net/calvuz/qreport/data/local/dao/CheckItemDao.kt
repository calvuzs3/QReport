package net.calvuz.qreport.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import net.calvuz.qreport.data.local.entity.CheckItemEntity

@Dao
interface CheckItemDao {

    @Query("SELECT * FROM check_items WHERE checkup_id = :checkUpId ORDER BY order_index ASC")
    fun getCheckItemsByCheckUpFlow(checkUpId: String): Flow<List<CheckItemEntity>>

    @Query("SELECT * FROM check_items WHERE checkup_id = :checkUpId ORDER BY order_index ASC")
    suspend fun getCheckItemsByCheckUp(checkUpId: String): List<CheckItemEntity>

    @Query("SELECT * FROM check_items WHERE id = :id")
    suspend fun getCheckItemById(id: String): CheckItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckItem(checkItem: CheckItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckItems(checkItems: List<CheckItemEntity>)

    @Update
    suspend fun updateCheckItem(checkItem: CheckItemEntity)

    @Delete
    suspend fun deleteCheckItem(checkItem: CheckItemEntity)

    @Query("DELETE FROM check_items WHERE checkup_id = :checkUpId")
    suspend fun deleteCheckItemsByCheckUpId(checkUpId: String)

    // ============================================================
    // METODI PER STATISTICHE (richiesti dal Repository)
    // ============================================================

    @Query("SELECT COUNT(*) FROM check_items WHERE checkup_id = :checkUpId")
    suspend fun getTotalItemsCount(checkUpId: String): Int

    @Query("SELECT COUNT(*) FROM check_items WHERE checkup_id = :checkUpId AND status IN ('OK', 'NOK', 'NA')")
    suspend fun getCompletedItemsCount(checkUpId: String): Int

    @Query("SELECT COUNT(*) FROM check_items WHERE checkup_id = :checkUpId AND status = :status")
    suspend fun getItemsCountByStatus(checkUpId: String, status: String): Int

    @Query("SELECT COUNT(*) FROM check_items WHERE checkup_id = :checkUpId AND status = 'NOK' AND criticality = :criticality")
    suspend fun getCriticalIssuesCount(checkUpId: String, criticality: String): Int

    // ============================================================
    // METODI PER MODULI (richiesti dal Repository)
    // ============================================================

    @Query("SELECT COUNT(*) FROM check_items WHERE checkup_id = :checkUpId AND module_type = :moduleType")
    suspend fun getItemsCountByModule(checkUpId: String, moduleType: String): Int?

    @Query("SELECT COUNT(*) FROM check_items WHERE checkup_id = :checkUpId AND module_type = :moduleType AND status IN ('OK', 'NOK', 'NA')")
    suspend fun getCompletedItemsCountByModule(checkUpId: String, moduleType: String): Int?

    @Query("SELECT COUNT(*) FROM check_items WHERE checkup_id = :checkUpId AND module_type = :moduleType AND status = 'NOK' AND criticality = :criticality")
    suspend fun getCriticalIssuesCountByModule(checkUpId: String, moduleType: String, criticality: String = "CRITICAL"): Int?

    // ============================================================
    // AGGIORNAMENTI STATO
    // ============================================================

    @Query("UPDATE check_items SET status = :status, checked_at = :checkedAt WHERE id = :itemId")
    suspend fun updateCheckItemStatus(itemId: String, status: String, checkedAt: Instant?)

    @Query("UPDATE check_items SET notes = :notes WHERE id = :itemId")
    suspend fun updateCheckItemNotes(itemId: String, notes: String)

    // ============================================================
    // RICERCHE E FILTRI
    // ============================================================

    @Query("SELECT * FROM check_items WHERE checkup_id = :checkUpId AND status = :status ORDER BY order_index ASC")
    fun getCheckItemsByStatus(checkUpId: String, status: String): Flow<List<CheckItemEntity>>

    @Query("SELECT * FROM check_items WHERE checkup_id = :checkUpId AND module_type = :moduleType ORDER BY order_index ASC")
    fun getCheckItemsByModule(checkUpId: String, moduleType: String): Flow<List<CheckItemEntity>>

    @Query("SELECT DISTINCT module_type FROM check_items WHERE checkup_id = :checkUpId")
    suspend fun getModuleTypesForCheckUp(checkUpId: String): List<String>
}