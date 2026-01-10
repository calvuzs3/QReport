package net.calvuz.qreport.client.client.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import net.calvuz.qreport.client.client.domain.model.Client

/**
 * Repository interface per gestione clienti
 * Definisce il contratto per accesso ai dati dei clienti
 * Implementazione nel data layer
 */
interface ClientRepository {

    // ===== CRUD OPERATIONS =====

    suspend fun getAllClients(): Result<List<Client>>
    suspend fun getActiveClients(): Result<List<Client>>
    suspend fun getClientById(id: String): Result<Client?>
    suspend fun createClient(client: Client): Result<Unit>
    suspend fun updateClient(client: Client): Result<Unit>
    suspend fun deleteClient(id: String): Result<Unit>

    // ===== FLOW OPERATIONS (REACTIVE) =====

    fun getAllClientsFlow(): Flow<List<Client>>
    fun getClientByIdFlow(id: String): Flow<Client?>

    // ===== SEARCH & FILTER =====

    suspend fun searchClients(query: String): Result<List<Client>>
    fun searchClientsFlow(query: String): Flow<List<Client>>
    suspend fun getClientsByIndustry(industry: String): Result<List<Client>>
    suspend fun getClientByVatNumber(vatNumber: String): Result<Client?>

    // ===== VALIDATION =====

    suspend fun isCompanyNameTaken(companyName: String, excludeId: String = ""): Result<Boolean>
    suspend fun isVatNumberTaken(vatNumber: String, excludeId: String = ""): Result<Boolean>

    // ===== STATISTICS =====

    suspend fun getActiveClientsCount(): Result<Int>
    suspend fun getTotalClientsCount(): Result<Int>
    suspend fun getAllIndustries(): Result<List<String>>
    suspend fun getFacilitiesCount(clientId: String): Result<Int>
    suspend fun getContactsCount(clientId: String): Result<Int>
    suspend fun getContractsCount(clientId: String): Result<Int>
    suspend fun getIslandsCount(clientId: String): Result<Int>

    // ===== COMPLEX QUERIES =====

    suspend fun getClientsWithFacilities(): Result<List<Client>>
    suspend fun getClientsWithContacts(): Result<List<Client>>
    suspend fun getClientsWithIslands(): Result<List<Client>>

    // ===== BULK OPERATIONS =====

    suspend fun createClients(clients: List<Client>): Result<Unit>
    suspend fun deleteInactiveClients(cutoffTimestamp: Instant): Result<Int>

    // ===== MAINTENANCE =====

    suspend fun touchClient(id: String): Result<Unit>
}