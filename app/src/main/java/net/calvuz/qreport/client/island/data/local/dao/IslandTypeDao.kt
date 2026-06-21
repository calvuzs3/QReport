package net.calvuz.qreport.client.island.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.island.data.local.entity.IslandTypeEntity

@Dao
interface IslandTypeDao {

    @Query("SELECT * FROM island_types WHERE is_active = 1 ORDER BY sort_order ASC")
    fun observeActiveIslandTypes(): Flow<List<IslandTypeEntity>>

    @Query("SELECT * FROM island_types WHERE is_active = 1 ORDER BY sort_order ASC")
    suspend fun getActiveIslandTypes(): List<IslandTypeEntity>

    /** All non-deleted types (active and inactive), for the management screen. */
    @Query("SELECT * FROM island_types WHERE is_deleted = 0 ORDER BY sort_order ASC")
    fun observeAllIslandTypes(): Flow<List<IslandTypeEntity>>

    @Query("SELECT * FROM island_types WHERE is_deleted = 0 ORDER BY sort_order ASC")
    suspend fun getAllIslandTypes(): List<IslandTypeEntity>

    @Query("SELECT * FROM island_types WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): IslandTypeEntity?

    @Query("SELECT * FROM island_types WHERE label = :label LIMIT 1")
    suspend fun getByLabel(label: String): IslandTypeEntity?

    @Query("SELECT * FROM island_types WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): IslandTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(types: List<IslandTypeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(type: IslandTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(type: IslandTypeEntity)

    @Query("UPDATE island_types SET is_active = 0, updated_at = :ts WHERE id = :id")
    suspend fun deactivate(id: String, ts: Long)

    @Query("UPDATE island_types SET is_active = 1, updated_at = :ts WHERE id = :id")
    suspend fun restore(id: String, ts: Long)

    @Query("DELETE FROM island_types")
    suspend fun deleteAll()

    // ===== SYNC =====

    @Query("""
        SELECT * FROM island_types
        WHERE updated_at > COALESCE(synced_at, 0)
        ORDER BY updated_at ASC
    """)
    suspend fun getPendingSync(): List<IslandTypeEntity>

    @Query("UPDATE island_types SET synced_at = :now WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, now: Long)
}
