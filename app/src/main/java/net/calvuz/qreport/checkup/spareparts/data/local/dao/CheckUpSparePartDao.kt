package net.calvuz.qreport.checkup.spareparts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.spareparts.data.local.entity.CheckUpSparePartEntity

@Dao
interface CheckUpSparePartDao {

    @Query("SELECT * FROM checkup_spare_parts WHERE checkup_id = :checkupId ORDER BY added_at ASC")
    fun observeByCheckup(checkupId: String): Flow<List<CheckUpSparePartEntity>>

    @Query("SELECT * FROM checkup_spare_parts WHERE checkup_id = :checkupId ORDER BY added_at ASC")
    suspend fun getByCheckup(checkupId: String): List<CheckUpSparePartEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(parts: List<CheckUpSparePartEntity>)

    @Query("DELETE FROM checkup_spare_parts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM checkup_spare_parts WHERE checkup_id = :checkupId")
    suspend fun deleteAllForCheckup(checkupId: String)

    @Query("UPDATE checkup_spare_parts SET quantity = :quantity WHERE id = :id")
    suspend fun updateQuantity(id: String, quantity: Double?)

    @Query("UPDATE checkup_spare_parts SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: String, notes: String)
}
