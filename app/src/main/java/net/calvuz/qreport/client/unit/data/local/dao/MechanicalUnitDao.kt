package net.calvuz.qreport.client.unit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.unit.data.local.entity.MechanicalUnitEntity

@Dao
interface MechanicalUnitDao {

    @Query(
        """
        SELECT * FROM mechanical_units
        WHERE island_id = :islandId AND is_active = 1
        ORDER BY unit_type ASC, name ASC
    """
    )
    fun getForIslandFlow(islandId: String): Flow<List<MechanicalUnitEntity>>

    @Query("SELECT * FROM mechanical_units WHERE id = :id")
    suspend fun getById(id: String): MechanicalUnitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unit: MechanicalUnitEntity)

    @Update
    suspend fun update(unit: MechanicalUnitEntity)

    @Query("UPDATE mechanical_units SET is_active = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long)

    @Query("SELECT * FROM mechanical_units WHERE island_id = :islandId AND is_active = 1 ORDER BY name ASC, serial_number ASC")
    suspend fun getMechanicalUnitsForIsland(islandId: String): List<MechanicalUnitEntity>

    // ============================================================
    // BACKUP METHODS
    // ============================================================

    @Query("SELECT * FROM mechanical_units ORDER BY created_at ASC")
    suspend fun getAllForBackup(): List<MechanicalUnitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(mechanicalUnits: List<MechanicalUnitEntity>)

    @Query("DELETE FROM mechanical_units")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM mechanical_units")
    suspend fun count(): Int

}