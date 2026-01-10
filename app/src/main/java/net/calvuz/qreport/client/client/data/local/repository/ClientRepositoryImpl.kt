package net.calvuz.qreport.client.client.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import net.calvuz.qreport.client.client.data.local.dao.ClientDao
import net.calvuz.qreport.client.client.data.local.mapper.ClientMapper
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import javax.inject.Inject

/**
 * Implementazione del repository per gestione clienti
 * Implementa tutti i metodi definiti in ClientRepository
 */
class ClientRepositoryImpl @Inject constructor(
    private val clientDao: ClientDao,
    private val clientMapper: ClientMapper
) : ClientRepository {

    // ===== CRUD OPERATIONS =====

    override suspend fun getAllClients(): Result<List<Client>> {
        return try {
            val entities = clientDao.getAllClients()
            val clients = entities.map { clientMapper.toDomain(it) }
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveClients(): Result<List<Client>> {
        return try {
            val entities = clientDao.getActiveClients()
            val clients = entities.map { clientMapper.toDomain(it) }
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getClientById(id: String): Result<Client?> {
        return try {
            val entity = clientDao.getClientById(id)
            val client = entity?.let { clientMapper.toDomain(it) }
            Result.success(client)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createClient(client: Client): Result<Unit> {
        return try {
            val entity = clientMapper.toEntity(client)
            clientDao.insertClient(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateClient(client: Client): Result<Unit> {
        return try {
            val entity = clientMapper.toEntity(client)
            clientDao.updateClient(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteClient(id: String): Result<Unit> {
        return try {
            clientDao.softDeleteClient(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== FLOW OPERATIONS (REACTIVE) =====

    override fun getAllClientsFlow(): Flow<List<Client>> {
        return clientDao.getAllActiveClientsFlow()
            .map { entities -> entities.map { clientMapper.toDomain(it) } }
    }

    override fun getClientByIdFlow(id: String): Flow<Client?> {
        return clientDao.getClientByIdFlow(id)
            .map { entity -> entity?.let { clientMapper.toDomain(it) } }
    }

    // ===== SEARCH & FILTER =====

    override suspend fun searchClients(query: String): Result<List<Client>> {
        return try {
            val entities = clientDao.searchClients(query)
            val clients = entities.map { clientMapper.toDomain(it) }
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun searchClientsFlow(query: String): Flow<List<Client>> {
        return clientDao.searchClientsFlow(query)
            .map { entities -> entities.map { clientMapper.toDomain(it) } }
    }

    override suspend fun getClientsByIndustry(industry: String): Result<List<Client>> {
        return try {
            val entities = clientDao.getClientsByIndustry(industry)
            val clients = entities.map { clientMapper.toDomain(it) }
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getClientByVatNumber(vatNumber: String): Result<Client?> {
        return try {
            val entity = clientDao.getClientByVatNumber(vatNumber)
            val client = entity?.let { clientMapper.toDomain(it) }
            Result.success(client)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== VALIDATION =====

    override suspend fun isCompanyNameTaken(companyName: String, excludeId: String): Result<Boolean> {
        return try {
            val isTaken = clientDao.isCompanyNameTaken(companyName, excludeId)
            Result.success(isTaken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isVatNumberTaken(vatNumber: String, excludeId: String): Result<Boolean> {
        return try {
            val isTaken = clientDao.isVatNumberTaken(vatNumber, excludeId)
            Result.success(isTaken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== STATISTICS =====

    override suspend fun getActiveClientsCount(): Result<Int> {
        return try {
            val count = clientDao.getActiveClientsCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTotalClientsCount(): Result<Int> {
        return try {
            val count = clientDao.getTotalClientsCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllIndustries(): Result<List<String>> {
        return try {
            val industries = clientDao.getAllIndustries()
            Result.success(industries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFacilitiesCount(clientId: String): Result<Int> {
        return try {
            val count = clientDao.getFacilitiesCount(clientId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getContactsCount(clientId: String): Result<Int> {
        return try {
            val count = clientDao.getContactsCount(clientId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getContractsCount(clientId: String): Result<Int> {
        return try {
            val count = clientDao.getContractsCount(clientId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandsCount(clientId: String): Result<Int> {
        return try {
            val count = clientDao.getIslandsCount(clientId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== COMPLEX QUERIES =====

    override suspend fun getClientsWithFacilities(): Result<List<Client>> {
        return try {
            val entities = clientDao.getClientsWithFacilities()
            val clients = entities.map { clientMapper.toDomain(it) }
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getClientsWithContacts(): Result<List<Client>> {
        return try {
            val entities = clientDao.getClientsWithContacts()
            val clients = entities.map { clientMapper.toDomain(it) }
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getClientsWithIslands(): Result<List<Client>> {
        return try {
            val entities = clientDao.getClientsWithIslands()
            val clients = entities.map { clientMapper.toDomain(it) }
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== BULK OPERATIONS =====

    override suspend fun createClients(clients: List<Client>): Result<Unit> {
        return try {
            val entities = clients.map { clientMapper.toEntity(it) }
            clientDao.insertClients(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteInactiveClients(cutoffTimestamp: Instant): Result<Int> {
        return try {
            val deletedCount = clientDao.permanentlyDeleteInactiveClients(cutoffTimestamp.toEpochMilliseconds())
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== MAINTENANCE =====

    override suspend fun touchClient(id: String): Result<Unit> {
        return try {
            clientDao.touchClient(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}