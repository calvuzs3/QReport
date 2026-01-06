package net.calvuz.qreport.client.contract.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.contract.data.local.ContractDao
import net.calvuz.qreport.client.contract.data.local.toDomain
import net.calvuz.qreport.client.contract.data.local.toEntity
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.repository.ContractRepository
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContractRepositoryImpl @Inject constructor(
    private val contractDao: ContractDao
) : ContractRepository {

    // --- METODI CON QrResult (ONE-SHOT) ---

    override suspend fun getAllContracts(): QrResult<List<Contract>, QrError> = try {
        val contracts = contractDao.getAllContracts().map { it.toDomain() }
        QrResult.Success(contracts)
    } catch (e: Exception) {
        Timber.e(e, "Errore durante il recupero di tutti i contratti")
        QrResult.Error(QrError.DatabaseError.OperationFailed(e.localizedMessage))
    }

    override suspend fun getActiveContracts(): QrResult<List<Contract>, QrError> {
        return try {
            val contracts = contractDao.getAllActiveContracts().map { it.toDomain() }
            QrResult.Success(contracts)
        } catch (e: Exception) {
            Timber.e(e, "Errore durante il recupero dei contratti attivi")
            QrResult.Error(QrError.DatabaseError.OperationFailed(e.localizedMessage))
        }
    }

    override suspend fun getExpiredContracts(): QrResult<List<Contract>, QrError> = try {
        val contracts = contractDao.getAllExpiredContracts().map { it.toDomain() }
        QrResult.Success(contracts)
    } catch (e: Exception) {
        Timber.e(e, "Errore durante il recupero dei contratti scaduti")
        QrResult.Error(QrError.DatabaseError.OperationFailed(e.localizedMessage))
    }

    override suspend fun getContractById(id: String): QrResult<Contract?, QrError> = try {
        val contract = contractDao.getContractById(id)?.toDomain()
        QrResult.Success(contract)
    } catch (e: Exception) {
        Timber.e(e, "Errore durante il recupero del contratto con ID: $id")
        QrResult.Error(QrError.DatabaseError.OperationFailed(e.localizedMessage))
    }

    override suspend fun createContract(contract: Contract): QrResult<Unit, QrError> = try {
        val entity = contract.toEntity()
        contractDao.insertContract(entity)
        Timber.d("Contratto creato con ID: ${contract.id}")
        QrResult.Success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Errore durante la creazione del contratto con ID: ${contract.id}")
        QrResult.Error(QrError.DatabaseError.InsertFailed(e.localizedMessage))
    }

    override suspend fun updateContract(contract: Contract): QrResult<Unit, QrError> = try {
        val entity = contract.toEntity()

        contractDao.updateContract(entity)
        QrResult.Success(Unit)

    } catch (e: Exception) {
        Timber.e(e, "Errore durante l'aggiornamento del contratto con ID: ${contract.id}")
        QrResult.Error(QrError.DatabaseError.UpdateFailed(e.localizedMessage))
    }

    override suspend fun deleteContractById(id: String): QrResult<Int, QrError> = try {
        val c = when ( val res = getContractById(id)) {
            is QrResult.Success -> {
                if (res.data != null) {
                    contractDao.deleteContract(res.data.toEntity())
                        Timber.d("Contratto eliminato con ID: $id")
                    1
                } else {
                    Timber.w("Nessun contratto eliminato per l'ID: $id. Il contratto potrebbe non esistere.")
                    0
                }
            }
            is QrResult.Error -> 0 //return QrResult.Error(c.error)
        }
        QrResult.Success(c)
    } catch (e: Exception) {
        Timber.e(e, "Errore durante l'eliminazione del contratto con ID: $id")
        QrResult.Error(QrError.DatabaseError.DeleteFailed(e.localizedMessage))
    }

    override suspend fun getContractsByClient(clientId: String): QrResult<List<Contract>, QrError> =
        try {
            val contracts = contractDao.getContractsByClientId(clientId).map { it.toDomain() }
            QrResult.Success(contracts)
        } catch (e: Exception) {
            Timber.e(e, "Errore durante il recupero dei contratti per il cliente: $clientId")
            QrResult.Error(QrError.DatabaseError.OperationFailed(e.localizedMessage))
        }

    override suspend fun getActiveContractsByClient(clientId: String): QrResult<List<Contract>, QrError> =
        try {
            val contracts =
                contractDao.getAllActiveContractsByClientId(clientId).map { it.toDomain() }
            QrResult.Success(contracts)
        } catch (e: Exception) {
            Timber.e(
                e,
                "Errore durante il recupero dei contratti attivi per il cliente: $clientId"
            )
            QrResult.Error(QrError.DatabaseError.OperationFailed(e.localizedMessage))
        }

    override suspend fun isExpired(id: String): QrResult<Boolean, QrError> = try {
        val contract = contractDao.getContractById(id)
        if (contract == null) {
            QrResult.Error(QrError.DatabaseError.NotFound("Contratto con ID $id non trovato"))
        } else {
            QrResult.Success(
                // Check contract validity
                contract.endDate < System.currentTimeMillis()
            )
        }
    } catch (e: Exception) {
        Timber.e(e, "Errore durante la verifica della scadenza per il contratto con ID: $id")
        QrResult.Error(QrError.DatabaseError.OperationFailed(e.localizedMessage))
    }

    // --- METODI CON FLOW (REATTIVI) ---

    override suspend fun getAllActiveContractsFlow(): Flow<List<Contract>> {
        return contractDao.getAllActiveContractsFlow()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { e ->
                Timber.e(e, "Errore nel flow dei contratti attivi")
                // In un flow, è meglio emettere una lista vuota o gestire l'errore nel ViewModel/UI
                emit(emptyList())
            }
    }

    override fun getContractByIdFlow(id: String): Flow<Contract?> {
        return contractDao.getContractByIdFlow(id)
            .map { entity -> entity?.toDomain() }
            .catch { e ->
                Timber.e(e, "Errore nel flow del contratto con ID: $id")
                emit(null)
            }
    }

    override fun getContractsByClientFlow(clientId: String): Flow<List<Contract>> {
        return contractDao.getContractsByClientIdFlow(clientId)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { e ->
                Timber.e(e, "Errore nel flow dei contratti per il cliente: $clientId")
                emit(emptyList())
            }
    }
}
