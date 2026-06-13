@file:Suppress("HardcodedStringLiteral")

package net.calvuz.qreport.client.client.data.local.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import net.calvuz.qreport.app.database.data.local.QReportDatabase
import net.calvuz.qreport.client.client.data.local.dao.ClientDao
import net.calvuz.qreport.client.client.data.local.mapper.ClientMapper
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import net.calvuz.qreport.client.contact.data.local.dao.ContactDao
import net.calvuz.qreport.client.contract.data.local.dao.ContractDao
import net.calvuz.qreport.client.facility.data.local.dao.FacilityDao
import javax.inject.Inject

class ClientRepositoryImpl @Inject constructor(
    private val clientDao: ClientDao,
    private val facilityDao: FacilityDao,
    private val contactsDao: ContactDao,
    private val contractsDao: ContractDao,
    private val clientMapper: ClientMapper,
    private val database: QReportDatabase
) : ClientRepository {

    // ===== CRUD OPERATIONS =====

    override suspend fun getClients(): Result<List<Client>> {
        return try {
            val entities = clientDao.getClients()
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

    override suspend fun deleteClient(client: Client): Result<Unit> {
        return try {
            val entity = clientMapper.toEntity(client)
            clientDao.deleteClient(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== TWO-STAGE DELETE =====

    override suspend fun deactivateClient(id: String, ts: Long): Result<Unit> = runCatching {
        database.withTransaction {
            val client  = clientDao.getClientById(id) ?: error("Client not found: $id")
            val facilities = facilityDao.getActiveFacilitiesByClient(client.id)
            val contacts = contactsDao.getActiveContactsByClient(client.id)
            val contracts = contractsDao.getActiveContractsByClientId(client.id)

            contacts.forEach { contact ->
                contactsDao.deactivateContact(contact.id, ts)
            }
            contracts.forEach { contract ->
                contractsDao.deactivateContract(contract.id, ts)
            }
            facilities.forEach { facility ->
                facilityDao.deactivateFacility(facility.id, ts)
            }
            clientDao.deactivateClient(id, ts)
        }
    }

    override suspend fun markClientDeleted(id: String, ts: Long): Result<Unit> = runCatching {
        database.withTransaction {
            val client = clientDao.getClientById(id) ?: error("Client not found: $id")
            val facilities = facilityDao.getFacilitiesByClient(client.id)
            val contacts = contactsDao.getContactsByClient(client.id)
            val contracts = contractsDao.getContractsByClientId(client.id)

            facilities.forEach { facility ->
                facilityDao.markFacilityDeleted(facility.id, ts)
            }
            contacts.forEach { contact ->
                contactsDao.markContactDeleted(contact.id, ts)
            }
            contracts.forEach { contract ->
                contractsDao.markContractDeleted(contract.id, ts)
            }
            clientDao.markClientDeleted(id, ts)
        }
    }

    // ===== RESURRECT =====

    override suspend fun restoreClient(id: String, ts: Long): Result<Unit> {
        return try {
            clientDao.restoreClient(id, ts)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== FLOW OPERATIONS (REACTIVE) =====

    override fun getClientsFlow(): Flow<List<Client>> {
        return clientDao.getClientsFlow()
            .map { entities -> entities.map { clientMapper.toDomain(it) } }
    }

    override fun getActiveClientsFlow(): Flow<List<Client>> {
        return clientDao.getActiveClientsFlow()
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

    // ===== VALIDATION =====

    override suspend fun isCompanyNameTaken(
        companyName: String, excludeId: String
    ): Result<Boolean> {
        return try {
            val isTaken = clientDao.isCompanyNameTaken(companyName, excludeId)
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

    override suspend fun getActiveClientsWithFacilities(): Result<List<Client>> {
        return try {
            val entities = clientDao.getActiveClientsWithFacilities()
            val clients = entities.map { clientMapper.toDomain(it) }
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveClientsWithContacts(): Result<List<Client>> {
        return try {
            val entities = clientDao.getActiveClientsWithContacts()
            val clients = entities.map { clientMapper.toDomain(it) }
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveClientsWithContracts(): Result<List<Client>> {
        return try {
            val entities = clientDao.getActiveClientsWithContracts()
            val clients = entities.map { clientMapper.toDomain(it) }
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveClientsWithIslands(): Result<List<Client>> {
        return try {
            val entities = clientDao.getActiveClientsWithIslands()
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
            val deletedCount =
                clientDao.permanentlyDeleteInactiveClients(cutoffTimestamp.toEpochMilliseconds())
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