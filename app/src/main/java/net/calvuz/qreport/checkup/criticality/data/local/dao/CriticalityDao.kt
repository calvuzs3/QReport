package net.calvuz.qreport.checkup.criticality.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.criticality.data.local.entity.CriticalityEntity

@Dao
interface CriticalityDao {

    @Query("SELECT * FROM criticality_levels WHERE is_active = 1 ORDER BY sort_order ASC")
    fun observeActiveCriticalityLevels(): Flow<List<CriticalityEntity>>

    @Query("SELECT * FROM criticality_levels ORDER BY sort_order ASC")
    fun observeAllCriticalityLevels(): Flow<List<CriticalityEntity>>

    @Query("SELECT * FROM criticality_levels ORDER BY sort_order ASC")
    suspend fun getAllCriticalityLevels(): List<CriticalityEntity>

    @Query("SELECT * FROM criticality_levels WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): CriticalityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(level: CriticalityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(level: CriticalityEntity)

    @Query("UPDATE criticality_levels SET is_active = 0, updated_at = :ts WHERE id = :id")
    suspend fun deactivate(id: String, ts: Long)

    @Query("UPDATE criticality_levels SET is_active = 1, updated_at = :ts WHERE id = :id")
    suspend fun restore(id: String, ts: Long)

    // ===== SYNC =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(levels: List<CriticalityEntity>)

    @Query("""
        SELECT * FROM criticality_levels
        WHERE updated_at > COALESCE(synced_at, 0)
        ORDER BY updated_at ASC
    """)
    suspend fun getPendingSync(): List<CriticalityEntity>

    @Query("UPDATE criticality_levels SET synced_at = :now WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, now: Long)
}
