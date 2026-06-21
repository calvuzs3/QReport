package net.calvuz.qreport.checkup.status.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.status.data.local.entity.CheckUpStatusEntity
import net.calvuz.qreport.checkup.status.data.local.entity.CheckUpStatusTransitionCrossRef

@Dao
interface CheckUpStatusDao {

    @Query("SELECT * FROM checkup_statuses WHERE is_active = 1 ORDER BY sort_order ASC")
    fun observeActiveCheckUpStatuses(): Flow<List<CheckUpStatusEntity>>

    @Query("SELECT * FROM checkup_statuses ORDER BY sort_order ASC")
    fun observeAllCheckUpStatuses(): Flow<List<CheckUpStatusEntity>>

    @Query("SELECT * FROM checkup_statuses WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CheckUpStatusEntity?

    @Query("SELECT * FROM checkup_statuses WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): CheckUpStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(status: CheckUpStatusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(status: CheckUpStatusEntity)

    @Query("UPDATE checkup_statuses SET is_active = 0, updated_at = :ts WHERE id = :id")
    suspend fun deactivate(id: String, ts: Long)

    @Query("UPDATE checkup_statuses SET is_active = 1, updated_at = :ts WHERE id = :id")
    suspend fun restore(id: String, ts: Long)

    @Query("SELECT * FROM checkup_status_transitions")
    fun observeAllTransitions(): Flow<List<CheckUpStatusTransitionCrossRef>>

    @Query("SELECT EXISTS(SELECT 1 FROM checkup_status_transitions WHERE from_status_id = :fromId AND to_status_id = :toId)")
    suspend fun isTransitionAllowed(fromId: String, toId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransitions(transitions: List<CheckUpStatusTransitionCrossRef>)

    @Query("DELETE FROM checkup_status_transitions WHERE from_status_id = :fromId")
    suspend fun deleteTransitionsFrom(fromId: String)

    @Transaction
    suspend fun replaceTransitionsFrom(fromId: String, toIds: List<String>) {
        deleteTransitionsFrom(fromId)
        insertTransitions(toIds.map { CheckUpStatusTransitionCrossRef(fromId, it) })
    }
}
