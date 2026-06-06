package net.calvuz.qreport.client.client.domain.repository

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import net.calvuz.qreport.client.client.domain.model.Client

/**
 * Client Repository
 */
interface ClientRepository {

    // ===== CRUD OPERATIONS =====

    suspend fun getClients(): Result<List<Client>>
    suspend fun getActiveClients(): Result<List<Client>>
    suspend fun getClientById(id: String): Result<Client?>
    suspend fun createClient(client: Client): Result<Unit>
    suspend fun updateClient(client: Client): Result<Unit>
    suspend fun deleteClient(client: Client): Result<Unit>

    // ===== TWO-STAGE DELETE =====

    @Transaction
    suspend fun deactivateClient(id: String): Result<Unit>
    @Transaction
    suspend fun markClientDeleted(id: String): Result<Unit>

    // ===== FLOW OPERATIONS (REACTIVE) =====

    fun getClientsFlow(): Flow<List<Client>>
    fun getActiveClientsFlow(): Flow<List<Client>>
    fun getClientByIdFlow(id: String): Flow<Client?>

    // ===== SEARCH & FILTER =====

    suspend fun searchClients(query: String): Result<List<Client>>
    fun searchClientsFlow(query: String): Flow<List<Client>>

    // ===== VALIDATION =====

    suspend fun isCompanyNameTaken(companyName: String, excludeId: String = ""): Result<Boolean>

    // ===== STATISTICS =====

    suspend fun getActiveClientsCount(): Result<Int>
    suspend fun getTotalClientsCount(): Result<Int>
    suspend fun getFacilitiesCount(clientId: String): Result<Int>
    suspend fun getContactsCount(clientId: String): Result<Int>
    suspend fun getContractsCount(clientId: String): Result<Int>
    suspend fun getIslandsCount(clientId: String): Result<Int>

    // ===== COMPLEX QUERIES =====

    suspend fun getActiveClientsWithFacilities(): Result<List<Client>>
    suspend fun getActiveClientsWithContacts(): Result<List<Client>>
    suspend fun getActiveClientsWithContracts(): Result<List<Client>>
    suspend fun getActiveClientsWithIslands(): Result<List<Client>>

    // ===== BULK OPERATIONS =====

    suspend fun createClients(clients: List<Client>): Result<Unit>
    suspend fun deleteInactiveClients(cutoffTimestamp: Instant): Result<Int>

    // ===== MAINTENANCE =====

    suspend fun touchClient(id: String): Result<Unit>
}