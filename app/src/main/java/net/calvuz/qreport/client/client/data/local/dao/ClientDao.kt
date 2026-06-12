package net.calvuz.qreport.client.client.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.client.data.local.entity.ClientEntity

@Dao
interface ClientDao {

    // ===== BASIC CRUD =====

    @Query("SELECT * FROM clients ORDER BY company_name ASC")
    fun getClientsFlow(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE is_active = 1 ORDER BY company_name ASC")
    fun getActiveClientsFlow(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients ORDER BY company_name ASC")
    suspend fun getClients(): List<ClientEntity>

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

    // ===== DELETE — TWO-STAGE =====

    /**
     * Stage 1: deactivate client and cascade to all children.
     * Called by [ClientRepositoryImpl.deactivateClient].
     *
     * Order: deepest children first, then parent — so a partial failure
     * leaves the client still visible rather than orphaning records.
     */
    @Query("UPDATE mechanical_units SET is_active = 0, updated_at = :timestamp WHERE island_id IN (SELECT fi.id FROM facility_islands fi INNER JOIN facilities f ON fi.facility_id = f.id WHERE f.client_id = :clientId)")
    suspend fun deactivateMechanicalUnitsByClient(
        clientId: String, timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE facility_islands SET is_active = 0, updated_at = :timestamp WHERE facility_id IN (SELECT id FROM facilities WHERE client_id = :clientId)")
    suspend fun deactivateIslandsByClient(
        clientId: String, timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE facilities SET is_active = 0, updated_at = :timestamp WHERE client_id = :clientId")
    suspend fun deactivateFacilitiesByClient(
        clientId: String, timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE contacts SET is_active = 0, updated_at = :timestamp WHERE client_id = :clientId")
    suspend fun deactivateContactsByClient(
        clientId: String, timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE contracts SET is_active = 0, updated_at = :timestamp WHERE client_id = :clientId")
    suspend fun deactivateContractsByClient(
        clientId: String, timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE clients SET is_active = 0, updated_at = :timestamp WHERE id = :clientId AND is_active = 1")
    suspend fun deactivateClient(clientId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Stage 2: mark client and all children as deleted for server sync.
     * Called by [ClientRepositoryImpl.markClientDeleted].
     */
    @Query("UPDATE mechanical_units SET is_deleted = 1, updated_at = :timestamp WHERE island_id IN (SELECT fi.id FROM facility_islands fi INNER JOIN facilities f ON fi.facility_id = f.id WHERE f.client_id = :clientId)")
    suspend fun markMechanicalUnitsDeletedByClient(
        clientId: String, timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE facility_islands SET is_deleted = 1, updated_at = :timestamp WHERE facility_id IN (SELECT id FROM facilities WHERE client_id = :clientId)")
    suspend fun markIslandsDeletedByClient(
        clientId: String, timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE facilities SET is_deleted = 1, updated_at = :timestamp WHERE client_id = :clientId")
    suspend fun markFacilitiesDeletedByClient(
        clientId: String, timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE contacts SET is_active = 0, updated_at = :timestamp WHERE client_id = :clientId")
    suspend fun markContactsDeletedByClient(
        clientId: String, timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE contracts SET is_active = 0, updated_at = :timestamp WHERE client_id = :clientId")
    suspend fun markContractsDeletedByClient(
        clientId: String, timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE clients SET is_deleted = 1, updated_at = :timestamp WHERE id = :clientId AND is_deleted = 0")
    suspend fun markClientDeleted(clientId: String, timestamp: Long = System.currentTimeMillis())

    // ===== RESTORE =====

    @Query("UPDATE clients SET is_active = 1, updated_at = :timestamp WHERE id = :clientId AND is_active = 0")
    suspend fun restoreClient(clientId: String, timestamp: Long = System.currentTimeMillis())

    // ===== SEARCH & FILTER =====

    @Query(
        """
        SELECT * FROM clients 
        WHERE is_active = 1 
        AND (company_name LIKE '%' || :query || '%' 
             OR notes LIKE '%' || :query || '%' )
        ORDER BY company_name ASC
    """
    )
    suspend fun searchClients(query: String): List<ClientEntity>

    @Query(
        """
        SELECT * FROM clients 
        WHERE (company_name LIKE '%' || :query || '%' 
             OR notes LIKE '%' || :query || '%' )
        ORDER BY company_name ASC
    """
    )
    suspend fun searchAllClients(query: String): List<ClientEntity>

    @Query(
        """
        SELECT * FROM clients 
        WHERE is_active = 1 
        AND (company_name LIKE '%' || :query || '%' 
             OR notes LIKE '%' || :query || '%' )
        ORDER BY company_name ASC
    """
    )
    fun searchClientsFlow(query: String): Flow<List<ClientEntity>>

    @Query(
        """
        SELECT * FROM clients 
        WHERE (company_name LIKE '%' || :query || '%' 
             OR notes LIKE '%' || :query || '%' )
        ORDER BY company_name ASC
    """
    )
    fun searchAllClientsFlow(query: String): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE is_active = 1")
    suspend fun getActiveClients(): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE is_active = 0 ORDER BY updated_at DESC")
    suspend fun getInactiveClients(): List<ClientEntity>

    // ===== STATISTICS =====

    @Query("SELECT COUNT(*) FROM clients WHERE is_active = 1")
    suspend fun getActiveClientsCount(): Int

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun getTotalClientsCount(): Int

    @Query(
        """
        SELECT COUNT(*) FROM facilities f
        INNER JOIN clients c ON f.client_id = c.id
        WHERE c.id = :clientId AND c.is_active = 1 AND f.is_active = 1
    """
    )
    suspend fun getFacilitiesCount(clientId: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM contacts ct
        INNER JOIN clients c ON ct.client_id = c.id
        WHERE c.id = :clientId AND c.is_active = 1 AND ct.is_active = 1
    """
    )
    suspend fun getContactsCount(clientId: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM contracts ctr
        INNER JOIN clients c ON ctr.client_id = c.id
        WHERE c.id = :clientId AND c.is_active = 1 AND ctr.is_active = 1
    """
    )
    suspend fun getContractsCount(clientId: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM facility_islands fi
        INNER JOIN facilities f ON fi.facility_id = f.id
        INNER JOIN clients c ON f.client_id = c.id
        WHERE c.id = :clientId AND c.is_active = 1 AND f.is_active = 1 AND fi.is_active = 1
    """
    )
    suspend fun getIslandsCount(clientId: String): Int

    // ===== VALIDATION =====

    @Query("SELECT COUNT(*) > 0 FROM clients WHERE company_name = :companyName AND id != :excludeId")
    suspend fun isCompanyNameTaken(companyName: String, excludeId: String = ""): Boolean

    // ===== BULK OPERATIONS =====

    @Query("DELETE FROM clients WHERE is_active = 0 AND updated_at < :cutoffTimestamp")
    suspend fun permanentlyDeleteInactiveClients(cutoffTimestamp: Long): Int

    // ===== COMPLEX QUERIES =====

    @Query(
        """
        SELECT c.* FROM clients c
        WHERE c.is_active = 1
        AND EXISTS (
            SELECT 1 FROM facilities f 
            WHERE f.client_id = c.id AND f.is_active = 1
        )
        ORDER BY c.company_name ASC
    """
    )
    suspend fun getActiveClientsWithFacilities(): List<ClientEntity>

    @Query(
        """
        SELECT c.* FROM clients c
        WHERE c.is_active = 1
        AND EXISTS (
            SELECT 1 FROM contacts ct 
            WHERE ct.client_id = c.id AND ct.is_active = 1
        )
        ORDER BY c.company_name ASC
    """
    )
    suspend fun getActiveClientsWithContacts(): List<ClientEntity>

    @Query(
        """
        SELECT c.* FROM clients c
        WHERE c.is_active = 1
        AND EXISTS (
            SELECT 1 FROM contracts ct 
            WHERE ct.client_id = c.id AND ct.is_active = 1
        )
        ORDER BY c.company_name ASC
    """
    )
    suspend fun getActiveClientsWithContracts(): List<ClientEntity>

    @Query(
        """
        SELECT c.* FROM clients c
        WHERE c.is_active = 1
        AND EXISTS (
            SELECT 1 FROM facility_islands fi
            INNER JOIN facilities f ON fi.facility_id = f.id
            WHERE f.client_id = c.id AND fi.is_active = 1 AND f.is_active = 1
        )
        ORDER BY c.company_name ASC
    """
    )
    suspend fun getActiveClientsWithIslands(): List<ClientEntity>

    // ===== MAINTENANCE =====

    @Query("UPDATE clients SET updated_at = :timestamp WHERE id = :id")
    suspend fun touchClient(id: String, timestamp: Long = System.currentTimeMillis())

    // ============================================================
    // BACKUP METHODS
    // ============================================================

    @Query("SELECT * FROM clients ORDER BY created_at ASC")
    suspend fun getAllForBackup(): List<ClientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(clients: List<ClientEntity>)

    @Query("DELETE FROM clients")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun count(): Int
}