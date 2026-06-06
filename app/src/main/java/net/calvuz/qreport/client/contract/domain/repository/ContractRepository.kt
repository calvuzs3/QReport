package net.calvuz.qreport.client.contract.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult

interface ContractRepository {

    // ===== CRUD OPERATIONS =====

    suspend fun getAllContracts(): QrResult<List<Contract>, QrError>
    suspend fun setActiveContractById(id: String): QrResult<Int, QrError>
    suspend fun setInactiveContractById(id: String): QrResult<Int, QrError>
    suspend fun getActiveContracts(): QrResult<List<Contract>, QrError>
    suspend fun getExpiredContracts(): QrResult<List<Contract>, QrError>
    suspend fun getContractById(id: String): QrResult<Contract?, QrError>
    suspend fun createContract(contract: Contract): QrResult<String, QrError>
    suspend fun updateContract(contract: Contract): QrResult<String, QrError>
    suspend fun deleteContractById(id: String): QrResult<Int, QrError>

    // ===== DELETE — TWO-STAGE =====

    suspend fun deactivateContract(id: String, timestamp: Long = System.currentTimeMillis())
    suspend fun markContractDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    // ===== CLIENT RELATED =====

    suspend fun getContractsByClient(clientId: String): QrResult<List<Contract>, QrError>
    suspend fun getActiveContractsByClient(clientId: String): QrResult<List<Contract>, QrError>

    // ===== FLOW OPERATIONS (REACTIVE) =====

    suspend fun getAllActiveContractsFlow(): Flow<List<Contract>>
    fun getContractByIdFlow(id: String): Flow<Contract?>
    fun getContractsByClientFlow(clientId: String): Flow<List<Contract>>

    // ===== SEARCH & FILTER =====

    // ===== VALIDATION =====

    suspend fun isExpired(id: String): QrResult<Boolean, QrError>

    suspend fun getContractsCountByClient(clientId: String): QrResult<Int, QrError>
}