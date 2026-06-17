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

    @Query("SELECT * FROM island_types WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): IslandTypeEntity?

    @Query("SELECT * FROM island_types WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): IslandTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(types: List<IslandTypeEntity>)

    @Query("DELETE FROM island_types")
    suspend fun deleteAll()
}
