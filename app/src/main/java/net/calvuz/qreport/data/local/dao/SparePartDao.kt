package net.calvuz.qreport.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.data.local.entity.SparePartEntity

@Dao
interface SparePartDao {

    @Query("SELECT * FROM spare_parts WHERE checkup_id = :checkUpId ORDER BY added_at DESC")
    fun getSparePartsByCheckUpFlow(checkUpId: String): Flow<List<SparePartEntity>>

    @Query("SELECT * FROM spare_parts WHERE checkup_id = :checkUpId ORDER BY added_at DESC")
    suspend fun getSparePartsByCheckUp(checkUpId: String): List<SparePartEntity>

    @Query("SELECT * FROM spare_parts WHERE id = :id")
    suspend fun getSparePartById(id: String): SparePartEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSparePart(sparePart: SparePartEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpareParts(spareParts: List<SparePartEntity>)

    @Update
    suspend fun updateSparePart(sparePart: SparePartEntity)

    @Delete
    suspend fun deleteSparePart(sparePart: SparePartEntity)

    @Query("DELETE FROM spare_parts WHERE id = :id")
    suspend fun deleteSparePartById(id: String)

    @Query("DELETE FROM spare_parts WHERE checkup_id = :checkUpId")
    suspend fun deleteSparePartsByCheckUpId(checkUpId: String)

    // ============================================================
    // METODI PER STATISTICHE (richiesti dal Repository)
    // ============================================================

    @Query("SELECT COUNT(*) FROM spare_parts WHERE checkup_id = :checkUpId")
    suspend fun getSparePartsCount(checkUpId: String): Int

    @Query("SELECT COUNT(*) FROM spare_parts WHERE checkup_id = :checkUpId AND urgency = :urgency")
    suspend fun getSparePartsCountByUrgency(checkUpId: String, urgency: String): Int

    @Query("SELECT COUNT(*) FROM spare_parts WHERE checkup_id = :checkUpId AND category = :category")
    suspend fun getSparePartsCountByCategory(checkUpId: String, category: String): Int

    @Query("SELECT SUM(estimated_cost * quantity) FROM spare_parts WHERE checkup_id = :checkUpId AND estimated_cost IS NOT NULL")
    suspend fun getTotalEstimatedCost(checkUpId: String): Double?

    // ============================================================
    // RICERCHE E FILTRI
    // ============================================================

    @Query("SELECT * FROM spare_parts WHERE checkup_id = :checkUpId AND urgency = :urgency ORDER BY added_at DESC")
    fun getSparePartsByUrgency(checkUpId: String, urgency: String): Flow<List<SparePartEntity>>

    @Query("SELECT * FROM spare_parts WHERE checkup_id = :checkUpId AND category = :category ORDER BY added_at DESC")
    fun getSparePartsByCategory(checkUpId: String, category: String): Flow<List<SparePartEntity>>

    @Query("SELECT DISTINCT urgency FROM spare_parts WHERE checkup_id = :checkUpId")
    suspend fun getUrgencyLevelsForCheckUp(checkUpId: String): List<String>

    @Query("SELECT DISTINCT category FROM spare_parts WHERE checkup_id = :checkUpId")
    suspend fun getCategoriesForCheckUp(checkUpId: String): List<String>

    // ============================================================
    // AGGIORNAMENTI
    // ============================================================

    @Query("UPDATE spare_parts SET urgency = :urgency WHERE id = :sparePartId")
    suspend fun updateSparePartUrgency(sparePartId: String, urgency: String)

    @Query("UPDATE spare_parts SET estimated_cost = :cost WHERE id = :sparePartId")
    suspend fun updateSparePartCost(sparePartId: String, cost: Double?)

    @Query("UPDATE spare_parts SET quantity = :quantity WHERE id = :sparePartId")
    suspend fun updateSparePartQuantity(sparePartId: String, quantity: Int)

    @Query("UPDATE spare_parts SET notes = :notes WHERE id = :sparePartId")
    suspend fun updateSparePartNotes(sparePartId: String, notes: String)

    // ============================================================
    // STATISTICHE GLOBALI
    // ============================================================

    @Query("SELECT COUNT(*) FROM spare_parts")
    suspend fun getTotalSparePartsCount(): Int

    @Query("SELECT COUNT(*) FROM spare_parts WHERE urgency = 'IMMEDIATE'")
    suspend fun getImmediateUrgencyCount(): Int

    @Query("SELECT COUNT(*) FROM spare_parts WHERE urgency = 'HIGH'")
    suspend fun getHighUrgencyCount(): Int

    // Cleanup per spare parts orfane
    @Query("""
        DELETE FROM spare_parts WHERE checkup_id NOT IN (
            SELECT id FROM checkups
        )
    """)
    suspend fun deleteOrphanedSpareParts()

    // ============================================================
    // BACKUP METHODS
    // ============================================================

    @Query("SELECT * FROM spare_parts ORDER BY added_at ASC")
    suspend fun getAllForBackup(): List<SparePartEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(spareParts: List<SparePartEntity>)

    @Query("DELETE FROM spare_parts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM spare_parts")
    suspend fun count(): Int
}