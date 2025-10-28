package net.calvuz.qreport.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import net.calvuz.qreport.data.local.entity.CheckUpEntity
import net.calvuz.qreport.data.local.entity.CheckUpWithDetails

@Dao
interface CheckUpDao {

    @Query("SELECT * FROM checkups ORDER BY updated_at DESC")
    fun getAllCheckUpsFlow(): Flow<List<CheckUpEntity>>

    @Query("SELECT * FROM checkups WHERE status = :status ORDER BY updated_at DESC")
    fun getCheckUpsByStatusFlow(status: String): Flow<List<CheckUpEntity>>

    @Query("SELECT * FROM checkups WHERE id = :id")
    suspend fun getCheckUpById(id: String): CheckUpEntity?

    @Query("SELECT * FROM checkups WHERE id = :id")
    fun getCheckUpByIdFlow(id: String): Flow<CheckUpEntity?>

    @Transaction
    @Query("SELECT * FROM checkups WHERE id = :id")
    suspend fun getCheckUpWithDetails(id: String): CheckUpWithDetails?

    @Transaction
    @Query("SELECT * FROM checkups ORDER BY updated_at DESC")
    fun getAllCheckUpsWithDetailsFlow(): Flow<List<CheckUpWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckUp(checkUp: CheckUpEntity): Long

    @Update
    suspend fun updateCheckUp(checkUp: CheckUpEntity)

    @Delete
    suspend fun deleteCheckUp(checkUp: CheckUpEntity)

    @Query("DELETE FROM checkups WHERE id = :id")
    suspend fun deleteCheckUpById(id: String)

    @Query("UPDATE checkups SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateCheckUpStatus(id: String, status: String, updatedAt: Instant)

    @Query("UPDATE checkups SET completed_at = :completedAt, status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun completeCheckUp(id: String, completedAt: Instant, status: String, updatedAt: Instant)

    // Statistiche e ricerche
    @Query("SELECT COUNT(*) FROM checkups WHERE status = :status")
    suspend fun countCheckUpsByStatus(status: String): Int

    @Query("SELECT * FROM checkups WHERE client_company_name LIKE '%' || :query || '%' OR island_serial_number LIKE '%' || :query || '%' ORDER BY updated_at DESC")
    fun searchCheckUps(query: String): Flow<List<CheckUpEntity>>
}

