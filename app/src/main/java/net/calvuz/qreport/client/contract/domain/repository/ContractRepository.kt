package net.calvuz.qreport.client.contract.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult

interface ContractRepository {

    // ===== CRUD OPERATIONS =====

    suspend fun getContracts(): QrResult<List<Contract>, QrError>
    fun getContractsFlow(): Flow<List<Contract>>

    fun getActiveContractsFlow(): Flow<List<Contract>>
    suspend fun getContractById(id: String): QrResult<Contract?, QrError>
    suspend fun getActiveContracts(): QrResult<List<Contract>, QrError>
    suspend fun getExpiredContracts(): QrResult<List<Contract>, QrError>
    suspend fun createContract(contract: Contract): QrResult<String, QrError>
    suspend fun updateContract(contract: Contract): QrResult<String, QrError>
    suspend fun deleteContractById(id: String): QrResult<Int, QrError>

    // ===== DELETE — TWO-STAGE =====

    suspend fun deactivateContract(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>
    suspend fun markContractDeleted(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>

    // ===== RESTORE =====

    suspend fun restoreContract(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>

    // ===== CLIENT RELATED =====

    suspend fun getContractsByClient(clientId: String): QrResult<List<Contract>, QrError>
    fun getContractsByClientFlow(clientId: String): Flow<List<Contract>>
    suspend fun getActiveContractsByClient(clientId: String): QrResult<List<Contract>, QrError>

    // ===== FLOW OPERATIONS (REACTIVE) =====


    // ===== SEARCH & FILTER =====

    // ===== VALIDATION =====

    suspend fun isExpired(id: String): QrResult<Boolean, QrError>

    suspend fun getContractsCountByClient(clientId: String): QrResult<Int, QrError>
}