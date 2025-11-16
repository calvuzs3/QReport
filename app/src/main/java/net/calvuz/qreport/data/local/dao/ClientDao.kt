package net.calvuz.qreport.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.data.local.entity.ClientEntity

/**
 * DAO per gestione clienti
 * Definisce tutte le operazioni CRUD e query complesse per ClientEntity
 */
@Dao
interface ClientDao {

    // ===== BASIC CRUD =====

    @Query("SELECT * FROM clients WHERE is_active = 1 ORDER BY company_name ASC")
    fun getAllActiveClientsFlow(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients ORDER BY company_name ASC")
    suspend fun getAllClients(): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientById(id: String): ClientEntity?

    @Query("SELECT * FROM clients WHERE id = :id")
    fun getClientByIdFlow(id: String): Flow<ClientEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<ClientEntity>)

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Delete
    suspend fun deleteClient(client: ClientEntity)

    @Query("UPDATE clients SET is_active = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun softDeleteClient(id: String, timestamp: Long = System.currentTimeMillis())

    // ===== SEARCH & FILTER =====

    @Query("""
        SELECT * FROM clients 
        WHERE is_active = 1 
        AND (company_name LIKE '%' || :query || '%' 
             OR vat_number LIKE '%' || :query || '%'
             OR industry LIKE '%' || :query || '%')
        ORDER BY company_name ASC
    """)
    suspend fun searchClients(query: String): List<ClientEntity>

    @Query("""
        SELECT * FROM clients 
        WHERE is_active = 1 
        AND (company_name LIKE '%' || :query || '%' 
             OR vat_number LIKE '%' || :query || '%'
             OR industry LIKE '%' || :query || '%')
        ORDER BY company_name ASC
    """)
    fun searchClientsFlow(query: String): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE is_active = 1")
    suspend fun getActiveClients(): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE is_active = 0 ORDER BY updated_at DESC")
    suspend fun getInactiveClients(): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE industry = :industry AND is_active = 1 ORDER BY company_name ASC")
    suspend fun getClientsByIndustry(industry: String): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE vat_number = :vatNumber")
    suspend fun getClientByVatNumber(vatNumber: String): ClientEntity?

    // ===== STATISTICS =====

    @Query("SELECT COUNT(*) FROM clients WHERE is_active = 1")
    suspend fun getActiveClientsCount(): Int

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun getTotalClientsCount(): Int

    @Query("SELECT DISTINCT industry FROM clients WHERE industry IS NOT NULL AND is_active = 1 ORDER BY industry ASC")
    suspend fun getAllIndustries(): List<String>

    @Query("""
        SELECT COUNT(*) FROM facilities f
        INNER JOIN clients c ON f.client_id = c.id
        WHERE c.id = :clientId AND c.is_active = 1 AND f.is_active = 1
    """)
    suspend fun getFacilitiesCount(clientId: String): Int

    @Query("""
        SELECT COUNT(*) FROM contacts ct
        INNER JOIN clients c ON ct.client_id = c.id
        WHERE c.id = :clientId AND c.is_active = 1 AND ct.is_active = 1
    """)
    suspend fun getContactsCount(clientId: String): Int

    @Query("""
        SELECT COUNT(*) FROM facility_islands fi
        INNER JOIN facilities f ON fi.facility_id = f.id
        INNER JOIN clients c ON f.client_id = c.id
        WHERE c.id = :clientId AND c.is_active = 1 AND f.is_active = 1 AND fi.is_active = 1
    """)
    suspend fun getIslandsCount(clientId: String): Int

    // ===== VALIDATION =====

    @Query("SELECT COUNT(*) > 0 FROM clients WHERE company_name = :companyName AND id != :excludeId")
    suspend fun isCompanyNameTaken(companyName: String, excludeId: String = ""): Boolean

    @Query("SELECT COUNT(*) > 0 FROM clients WHERE vat_number = :vatNumber AND id != :excludeId")
    suspend fun isVatNumberTaken(vatNumber: String, excludeId: String = ""): Boolean

    // ===== BULK OPERATIONS =====

    @Transaction
    suspend fun deleteClientCompletely(clientId: String) {
        // Le foreign key CASCADE si occupano di facilities, contacts e islands
        // Ma potremmo voler fare cleanup manuale per controllo
        softDeleteClient(clientId)
    }

    @Query("DELETE FROM clients WHERE is_active = 0 AND updated_at < :cutoffTimestamp")
    suspend fun permanentlyDeleteInactiveClients(cutoffTimestamp: Long): Int

    // ===== COMPLEX QUERIES =====

    @Query("""
        SELECT c.* FROM clients c
        WHERE c.is_active = 1
        AND EXISTS (
            SELECT 1 FROM facilities f 
            WHERE f.client_id = c.id AND f.is_active = 1
        )
        ORDER BY c.company_name ASC
    """)
    suspend fun getClientsWithFacilities(): List<ClientEntity>

    @Query("""
        SELECT c.* FROM clients c
        WHERE c.is_active = 1
        AND EXISTS (
            SELECT 1 FROM contacts ct 
            WHERE ct.client_id = c.id AND ct.is_active = 1
        )
        ORDER BY c.company_name ASC
    """)
    suspend fun getClientsWithContacts(): List<ClientEntity>

    @Query("""
        SELECT c.* FROM clients c
        WHERE c.is_active = 1
        AND EXISTS (
            SELECT 1 FROM facility_islands fi
            INNER JOIN facilities f ON fi.facility_id = f.id
            WHERE f.client_id = c.id AND fi.is_active = 1 AND f.is_active = 1
        )
        ORDER BY c.company_name ASC
    """)
    suspend fun getClientsWithIslands(): List<ClientEntity>

    // ===== QUERY CON CONTEGGI CORRETTA ===== ✅
    @Query("""
        SELECT c.id,
               c.company_name as companyName,
               c.vat_number as vatNumber,
               c.fiscal_code as fiscalCode,
               c.website,
               c.industry,
               c.notes,
               c.headquarters_json as headquartersJson,
               c.is_active as isActive,
               c.created_at as createdAt,
               c.updated_at as updatedAt,
               COUNT(DISTINCT f.id) as facilitiesCount,
               COUNT(DISTINCT ct.id) as contactsCount,
               COUNT(DISTINCT fi.id) as islandsCount
        FROM clients c
        LEFT JOIN facilities f ON c.id = f.client_id AND f.is_active = 1
        LEFT JOIN contacts ct ON c.id = ct.client_id AND ct.is_active = 1  
        LEFT JOIN facility_islands fi ON f.id = fi.facility_id AND fi.is_active = 1
        WHERE c.is_active = 1
        GROUP BY c.id, c.company_name, c.vat_number, c.fiscal_code, c.website, c.industry, c.notes, c.headquarters_json, c.is_active, c.created_at, c.updated_at
        ORDER BY c.company_name ASC
    """)
    suspend fun getClientsWithCounts(): List<ClientWithCountsResult>

    // ===== MAINTENANCE =====

    @Query("UPDATE clients SET updated_at = :timestamp WHERE id = :id")
    suspend fun touchClient(id: String, timestamp: Long = System.currentTimeMillis())
}

/**
 * Result class per query con conteggi - CORRETTA ✅
 */
data class ClientWithCountsResult(
    val id: String,
    val companyName: String,           // ✅ Matches alias in query
    val vatNumber: String?,            // ✅ Matches alias in query
    val fiscalCode: String?,           // ✅ Matches alias in query
    val website: String?,
    val industry: String?,
    val notes: String?,
    val headquartersJson: String?,     // ✅ Matches alias in query
    val isActive: Boolean,             // ✅ Matches alias in query
    val createdAt: Long,               // ✅ Matches alias in query
    val updatedAt: Long,               // ✅ Matches alias in query
    val facilitiesCount: Int,          // ✅ Matches alias in query
    val contactsCount: Int,            // ✅ Matches alias in query
    val islandsCount: Int              // ✅ Matches alias in query
)