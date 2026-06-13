package net.calvuz.qreport.client.contract.data.local.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.app.database.data.local.QReportDatabase
import net.calvuz.qreport.client.contract.data.local.dao.ContractDao
import net.calvuz.qreport.client.contract.data.local.mapper.toDomain
import net.calvuz.qreport.client.contract.data.local.mapper.toEntity
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.repository.ContractRepository
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.data.local.dao.ClientDao
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContractRepositoryImpl @Inject constructor(
    private val contractDao: ContractDao,
    private val clientDao: ClientDao,
    private val database: QReportDatabase
) : ContractRepository {

    // --- METODI ---

    override suspend fun getContracts(): QrResult<List<Contract>, QrError> = try {
        val contracts = contractDao.getContracts().map { it.toDomain() }
        QrResult.Success(contracts)
    } catch (e: Exception) {
        QrResult.Error(QrError.DatabaseError.OperationFailed(e.localizedMessage))
    }
    override fun getContractsFlow(): Flow<List<Contract>> {
        return contractDao.getContractsFlow()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { _ ->
                emit(emptyList())
            }
    }

    override suspend fun getActiveContracts(): QrResult<List<Contract>, QrError> {
        return try {
            val contracts = contractDao.getValidContracts().map { it.toDomain() }
            QrResult.Success(contracts)
        } catch (e: Exception) {
            QrResult.Error(QrError.DatabaseError.OperationFailed(e.localizedMessage))
        }
    }
    override fun getActiveContractsFlow(): Flow<List<Contract>> {
        return contractDao.getAllActiveContractsFlow()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { _ ->
                emit(emptyList())
            }
    }

    override suspend fun getContractById(id: String): QrResult<Contract?, QrError> = try {
        val contract = contractDao.getContract(id)?.toDomain()
        QrResult.Success(contract)
    } catch (e: Exception) {
        QrResult.Error(QrError.DatabaseError.OperationFailed(e.localizedMessage))
    }

    override suspend fun createContract(contract: Contract): QrResult<String, QrError> = try {
        val entity = contract.toEntity()
        contractDao.insertContract(entity)
        QrResult.Success(contract.id)
    } catch (e: Exception) {
        QrResult.Error(QrError.DatabaseError.InsertFailed(e.localizedMessage))
    }

    override suspend fun updateContract(contract: Contract): QrResult<String, QrError> = try {
        val entity = contract.toEntity()

        contractDao.updateContract(entity)
        QrResult.Success(contract.id)

    } catch (e: Exception) {
        QrResult.Error(QrError.DatabaseError.UpdateFailed(e.localizedMessage))
    }

    override suspend fun deleteContractById(id: String): QrResult<Int, QrError> = try {
        val c = when ( val res = getContractById(id)) {
            is QrResult.Success -> {
                if (res.data != null) {
                    contractDao.deleteContract(res.data.toEntity())
                    1
                } else {
                    0
                }
            }
            is QrResult.Error -> 0 //return QrResult.Error(c.error)
        }
        QrResult.Success(c)
    } catch (e: Exception) {
        QrResult.Error(QrError.DatabaseError.DeleteFailed(e.localizedMessage))
    }

    // ===== DELETE — TWO-STAGE =====

    override suspend fun deactivateContract(id: String, ts: Long ) = runCatching{
        database.withTransaction {
            contractDao.deactivateContract(id, ts)
        }
    }

    override suspend fun markContractDeleted(id: String, ts: Long ) = runCatching{
        database.withTransaction {
            contractDao.markContractDeleted(id, ts)
        }
    }

    // ===== RESTORE =====

    @Suppress("HardCodedStringLiteral")
    override suspend fun restoreContract(id: String, ts: Long ) = runCatching {
        database.withTransaction {
            val contract = contractDao.getContract(id) ?: error("Contract not found: $id")
            val client = clientDao.getClientById(contract.clientId) ?: error("Client not found: ${contract.clientId}")
            clientDao.restoreClient(client.id)
            contractDao.restoreContract(id)
        }
    }

    // --- METODI ---

    override suspend fun getExpiredContracts(): QrResult<List<Contract>, QrError> = try {
        val contracts = contractDao.getExpiredContracts().map { it.toDomain() }
        QrResult.Success(contracts)
    } catch (e: Exception) {
        QrResult.Error(QrError.DatabaseError.OperationFailed(e.localizedMessage))
    }

    override suspend fun getContractsByClient(clientId: String): QrResult<List<Contract>, QrError> =
        try {
            val contracts = contractDao.getContractsByClientId(clientId).map { it.toDomain() }
            QrResult.Success(contracts)
        } catch (e: Exception) {
            QrResult.Error(QrError.DatabaseError.OperationFailed(e.localizedMessage))
        }

    override fun getContractsByClientFlow(clientId: String): Flow<List<Contract>> {
        return contractDao.getContractsByClientIdFlow(clientId)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { _ ->
                emit(emptyList())
            }
    }

    override suspend fun getContractsCountByClient(clientId: String): QrResult<Int, QrError> =
        try {
            val contracts = contractDao.getContractCountByClientId(clientId)
            QrResult.Success(contracts)
        } catch (e: Exception) {
            QrResult.Error(QrError.DatabaseError.OperationFailed(e.localizedMessage))
        }

    override suspend fun getActiveContractsByClient(clientId: String): QrResult<List<Contract>, QrError> =
        try {
            val contracts =
                contractDao.getActiveContractsByClientId(clientId).map { it.toDomain() }
            QrResult.Success(contracts)
        } catch (e: Exception) {
            Timber.e(
                e,
            )
            QrResult.Error(QrError.DatabaseError.OperationFailed(e.localizedMessage))
        }

    override suspend fun isExpired(id: String): QrResult<Boolean, QrError> = try {
        val contract = contractDao.getContract(id)
        if (contract == null) {
            QrResult.Error(QrError.DatabaseError.NotFound(id))
        } else {
            QrResult.Success(
                // Check contract validity
                contract.endDate < System.currentTimeMillis()
            )
        }
    } catch (e: Exception) {
        QrResult.Error(QrError.DatabaseError.OperationFailed(e.localizedMessage))
    }
}
