package net.calvuz.qreport.client.contract.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContractDao {

    // ===== OPERATIONS =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContract(contract: ContractEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContracts(contracts: List<ContractEntity>)

    @Update
    suspend fun updateContract(contract: ContractEntity)

    @Delete
    suspend fun deleteContract(contract: ContractEntity)

    @Query("DELETE FROM contracts WHERE id = :id")
    suspend fun deleteContractById(id: String)

    @Query("SELECT * FROM contracts WHERE id = :id LIMIT 1")
    suspend fun getContractById(id: String): ContractEntity?

    @Query("SELECT * FROM contracts WHERE id = :id")
    fun getContractByIdFlow(id: String): Flow<ContractEntity?>

    @Query("SELECT * FROM contracts ORDER BY name ASC")
    suspend fun getAllContracts(): List<ContractEntity>

    @Query("SELECT * FROM contracts WHERE end_date > CURRENT_TIMESTAMP ORDER BY end_date ASC, name ASC")
    suspend fun getAllActiveContracts(): List<ContractEntity>

    @Query("SELECT * FROM contracts WHERE end_date > CURRENT_TIMESTAMP ORDER BY end_date ASC, name ASC")
    fun getAllActiveContractsFlow(): Flow<List<ContractEntity>>

    @Query("SELECT * FROM contracts WHERE end_date < CURRENT_TIMESTAMP ORDER BY name ASC")
    suspend fun getAllExpiredContracts(): List<ContractEntity>


    @Query("SELECT * FROM contracts WHERE client_id = :clientId ORDER BY start_date DESC, name ASC")
    suspend fun getContractsByClientId(clientId: String): List<ContractEntity>

    @Query("SELECT * FROM contracts WHERE client_id = :clientId AND end_date > CURRENT_TIMESTAMP ORDER BY end_date ASC, name ASC")
    suspend fun getAllActiveContractsByClientId(clientId: String): List<ContractEntity>

    @Query("SELECT * FROM contracts WHERE client_id = :clientId ORDER BY start_date DESC, name ASC")
    fun getContractsByClientIdFlow(clientId: String): Flow<List<ContractEntity>>

    @Query("SELECT * FROM contracts WHERE client_id = :clientId AND end_date > CURRENT_TIMESTAMP ORDER BY end_date ASC, name ASC")
    fun getAllActiveContractsByClientIdFlow(clientId: String): Flow<List<ContractEntity>>



    // ===== BULK OPERATIONS =====

    @Query("DELETE FROM contracts WHERE client_id = :clientId")
    suspend fun deleteAllContractsForClient(clientId: String)


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