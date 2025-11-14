package net.calvuz.qreport.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.data.local.entity.ContactEntity

/**
 * DAO per gestione referenti/contatti
 * Definisce tutte le operazioni CRUD e query complesse per ContactEntity
 */
@Dao
interface ContactDao {

    // ===== BASIC CRUD =====

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun getContactByIdFlow(id: String): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts WHERE client_id = :clientId AND is_active = 1 ORDER BY is_primary DESC, first_name ASC")
    suspend fun getContactsForClient(clientId: String): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE client_id = :clientId AND is_active = 1 ORDER BY is_primary DESC, first_name ASC")
    fun getContactsForClientFlow(clientId: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE is_active = 1 ORDER BY first_name ASC, last_name ASC")
    suspend fun getAllActiveContacts(): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Query("UPDATE contacts SET is_active = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun softDeleteContact(id: String, timestamp: Long = System.currentTimeMillis())

    // ===== PRIMARY CONTACT MANAGEMENT =====

    @Query("SELECT * FROM contacts WHERE client_id = :clientId AND is_primary = 1 AND is_active = 1")
    suspend fun getPrimaryContact(clientId: String): ContactEntity?

    @Transaction
    suspend fun setPrimaryContact(clientId: String, contactId: String) {
        // Rimuovi flag primary da tutti gli altri contatti del cliente
        clearPrimaryContact(clientId)
        // Imposta come primary il contatto specificato
        setPrimaryFlag(contactId, true, System.currentTimeMillis())
    }

    @Query("UPDATE contacts SET is_primary = 0, updated_at = :timestamp WHERE client_id = :clientId")
    suspend fun clearPrimaryContact(clientId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE contacts SET is_primary = :isPrimary, updated_at = :timestamp WHERE id = :contactId")
    suspend fun setPrimaryFlag(contactId: String, isPrimary: Boolean, timestamp: Long)

    // ===== SEARCH & FILTER =====

    @Query("""
        SELECT c.* FROM contacts c
        INNER JOIN clients cl ON c.client_id = cl.id
        WHERE c.is_active = 1 AND cl.is_active = 1
        AND (c.first_name LIKE '%' || :query || '%' 
             OR c.last_name LIKE '%' || :query || '%'
             OR c.email LIKE '%' || :query || '%'
             OR c.phone LIKE '%' || :query || '%'
             OR c.role LIKE '%' || :query || '%'
             OR cl.company_name LIKE '%' || :query || '%')
        ORDER BY c.first_name ASC, c.last_name ASC
    """)
    suspend fun searchContacts(query: String): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE role = :role AND is_active = 1 ORDER BY first_name ASC")
    suspend fun getContactsByRole(role: String): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE department = :department AND is_active = 1 ORDER BY first_name ASC")
    suspend fun getContactsByDepartment(department: String): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE email = :email AND is_active = 1")
    suspend fun getContactByEmail(email: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE phone = :phone AND is_active = 1")
    suspend fun getContactByPhone(phone: String): ContactEntity?

    @Query("SELECT DISTINCT role FROM contacts WHERE role IS NOT NULL AND is_active = 1 ORDER BY role ASC")
    suspend fun getAllRoles(): List<String>

    @Query("SELECT DISTINCT department FROM contacts WHERE department IS NOT NULL AND is_active = 1 ORDER BY department ASC")
    suspend fun getAllDepartments(): List<String>

    @Query("SELECT DISTINCT preferred_contact_method FROM contacts WHERE preferred_contact_method IS NOT NULL AND is_active = 1")
    suspend fun getAllPreferredContactMethods(): List<String>

    // ===== STATISTICS =====

    @Query("SELECT COUNT(*) FROM contacts WHERE client_id = :clientId AND is_active = 1")
    suspend fun getContactsCountForClient(clientId: String): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE is_active = 1")
    suspend fun getActiveContactsCount(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE role = :role AND is_active = 1")
    suspend fun getContactsCountByRole(role: String): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE department = :department AND is_active = 1")
    suspend fun getContactsCountByDepartment(department: String): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE is_primary = 1 AND is_active = 1")
    suspend fun getPrimaryContactsCount(): Int

    // ===== VALIDATION =====

    @Query("SELECT COUNT(*) > 0 FROM contacts WHERE client_id = :clientId AND email = :email AND id != :excludeId AND is_active = 1")
    suspend fun isEmailTakenForClient(clientId: String, email: String, excludeId: String = ""): Boolean

    @Query("SELECT COUNT(*) > 0 FROM contacts WHERE email = :email AND id != :excludeId AND is_active = 1")
    suspend fun isEmailTakenGlobally(email: String, excludeId: String = ""): Boolean

    // ===== COMMUNICATION QUERIES =====

    @Query("SELECT * FROM contacts WHERE email IS NOT NULL AND email != '' AND is_active = 1 ORDER BY first_name ASC")
    suspend fun getContactsWithEmail(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE phone IS NOT NULL AND phone != '' AND is_active = 1 ORDER BY first_name ASC")
    suspend fun getContactsWithPhone(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE mobile_phone IS NOT NULL AND mobile_phone != '' AND is_active = 1 ORDER BY first_name ASC")
    suspend fun getContactsWithMobilePhone(): List<ContactEntity>

    @Query("""
        SELECT * FROM contacts 
        WHERE is_active = 1 
        AND (email IS NOT NULL AND email != '' 
             OR phone IS NOT NULL AND phone != ''
             OR mobile_phone IS NOT NULL AND mobile_phone != '')
        ORDER BY first_name ASC
    """)
    suspend fun getContactsWithAnyContactInfo(): List<ContactEntity>

    @Query("""
        SELECT * FROM contacts 
        WHERE is_active = 1 
        AND (email IS NULL OR email = '') 
        AND (phone IS NULL OR phone = '')
        AND (mobile_phone IS NULL OR mobile_phone = '')
        ORDER BY first_name ASC
    """)
    suspend fun getContactsWithoutContactInfo(): List<ContactEntity>

    // ===== COMPLEX QUERIES =====

    @Query("""
        SELECT c.*, cl.company_name
        FROM contacts c
        INNER JOIN clients cl ON c.client_id = cl.id
        WHERE c.is_active = 1 AND cl.is_active = 1
        ORDER BY cl.company_name ASC, c.is_primary DESC, c.first_name ASC
    """)
    suspend fun getContactsWithClientInfo(): List<ContactWithClientResult>

    @Query("""
        SELECT * FROM contacts 
        WHERE client_id = :clientId AND is_active = 1
        AND preferred_contact_method = :contactMethod
        ORDER BY is_primary DESC, first_name ASC
    """)
    suspend fun getContactsByPreferredMethod(clientId: String, contactMethod: String): List<ContactEntity>

    // ===== MAINTENANCE =====

    @Query("UPDATE contacts SET updated_at = :timestamp WHERE id = :id")
    suspend fun touchContact(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM contacts WHERE is_active = 0 AND updated_at < :cutoffTimestamp")
    suspend fun permanentlyDeleteInactiveContacts(cutoffTimestamp: Long): Int

    // ===== BATCH OPERATIONS =====

    @Query("UPDATE contacts SET department = :newDepartment, updated_at = :timestamp WHERE department = :oldDepartment AND client_id = :clientId")
    suspend fun updateDepartmentForClient(clientId: String, oldDepartment: String, newDepartment: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE contacts SET role = :newRole, updated_at = :timestamp WHERE role = :oldRole AND client_id = :clientId")
    suspend fun updateRoleForClient(clientId: String, oldRole: String, newRole: String, timestamp: Long = System.currentTimeMillis())
}

/**
 * Result class per query con info cliente
 */
data class ContactWithClientResult(
    val id: String,
    val clientId: String,
    val firstName: String,
    val lastName: String?,
    val title: String?,
    val role: String?,
    val department: String?,
    val phone: String?,
    val mobilePhone: String?,
    val email: String?,
    val alternativeEmail: String?,
    val isPrimary: Boolean,
    val isActive: Boolean,
    val preferredContactMethod: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val companyName: String
)