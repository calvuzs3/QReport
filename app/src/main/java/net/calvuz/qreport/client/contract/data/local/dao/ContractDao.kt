@file:Suppress("HardcodedStringLiteral")

package net.calvuz.qreport.client.contract.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.contract.data.local.entity.ContractEntity

@Dao
interface ContractDao {

    // ===== OPERATIONS =====

    @Query("SELECT * FROM contracts ORDER BY name ASC")
    suspend fun getContracts(): List<ContractEntity>

    @Query("SELECT * FROM contracts ORDER BY name ASC")
    fun getContractsFlow(): Flow<List<ContractEntity>>

    @Query("SELECT * FROM contracts WHERE id = :id")
    fun getContractFlow(id: String): Flow<ContractEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContract(contract: ContractEntity): Long

    @Update
    suspend fun updateContract(contract: ContractEntity)

    @Delete
    suspend fun deleteContract(contract: ContractEntity)

    @Query("SELECT * FROM contracts WHERE id = :id LIMIT 1")
    suspend fun getContract(id: String): ContractEntity?

    @Query("SELECT * FROM contracts WHERE is_Active = 1 AND end_date > CURRENT_TIMESTAMP ORDER BY end_date ASC, name ASC")
    suspend fun getValidContracts(): List<ContractEntity>

    @Query("SELECT * FROM contracts WHERE is_Active = 1 AND end_date < CURRENT_TIMESTAMP ORDER BY end_date DESC")
    suspend fun getExpiredContracts(): List<ContractEntity>

    @Query("SELECT * FROM contracts WHERE client_id = :clientId ORDER BY end_date ASC, name ASC")
    suspend fun getContractsByClientId(clientId: String): List<ContractEntity>

    @Query("SELECT * FROM contracts WHERE client_id = :clientId ORDER BY end_date ASC, name ASC")
    fun getContractsByClientIdFlow(clientId: String): Flow<List<ContractEntity>>

    @Query("SELECT COUNT(*) FROM contracts WHERE client_id = :clientId AND is_active = 1 ORDER BY end_date ASC, name ASC")
    suspend fun getContractCountByClientId(clientId: String): Int

    @Query("SELECT * FROM contracts WHERE client_id = :clientId AND is_active = 1 ORDER BY end_date ASC, name ASC")
    suspend fun getActiveContractsByClientId(clientId: String): List<ContractEntity>

    @Query("SELECT * FROM contracts WHERE client_id = :clientId AND is_active = 1 ORDER BY end_date ASC, name ASC")
    fun getActiveContractsByClientIdFlow(clientId: String): Flow<List<ContractEntity>>

    @Query("SELECT * FROM contracts WHERE is_active = 1 ORDER BY end_date ASC, name ASC")
    fun getAllActiveContractsFlow(): Flow<List<ContractEntity>>

    // ===== DELETE — TWO-STAGE =====

    /**
     * Stage 1: deactivate a single contract.
     * Called by [ContractRepositoryImpl.deactivateContract()].
     */
    @Query("UPDATE contracts SET is_active = 0, updated_at = :ts WHERE id = :id AND is_active = 1")
    suspend fun deactivateContract(id: String, ts: Long = System.currentTimeMillis())

    /**
     * Stage 2: mark a single contract as deleted for server sync.
     * Called by [ContractRepositoryImpl.markContractDeleted()].
     */
    @Query("UPDATE contracts SET is_deleted = 1, updated_at = :ts WHERE id = :id AND is_deleted = 0")
    suspend fun markContractDeleted(id: String, ts: Long = System.currentTimeMillis())

    // ===== RESTORE =====

    @Query("UPDATE contracts SET is_active = 1, updated_at = :ts WHERE id = :id AND is_active = 0")
    suspend fun restoreContract(id: String, ts: Long = System.currentTimeMillis())

    // ===== BULK OPERATIONS =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllContracts(contracts: List<ContractEntity>)

    @Query("DELETE FROM contracts WHERE client_id = :clientId")
    suspend fun deleteAllContractsForClient(clientId: String)

    @Query("DELETE FROM contracts WHERE id IN (:ids)")
    suspend fun deleteAllContractsByIds(ids: List<String>)


    // ===== BACKUP OPERATIONS =====

    @Query("SELECT * FROM contracts ORDER BY created_at ASC")
    suspend fun getAllForBackup(): List<ContractEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(contracts: List<ContractEntity>)

    @Query("DELETE FROM contracts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM contracts")
    suspend fun count(): Int
}