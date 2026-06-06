package net.calvuz.qreport.client.contact.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import net.calvuz.qreport.client.contact.data.local.entity.ContactEntity

/**
 * DAO per la gestione dei contatti nel database Room
 * ✅ COMPLETO: Tutti i metodi richiesti dal ContactRepository
 */
@Dao
interface ContactDao {

    // ===== BASIC CRUD =====

    @Query("SELECT * FROM contacts WHERE id = :id AND is_active = 1 LIMIT 1")
    suspend fun getContactById(id: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun getContactByIdFlow(id: String): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts WHERE is_active = 1 ORDER BY first_name ASC")
    suspend fun getAllContacts(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE is_active = 1 ORDER BY first_name ASC")
    suspend fun getActiveContacts(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE is_active = 1 ORDER BY first_name ASC")
    fun getAllActiveContactsFlow(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE client_id = :clientId AND is_active = 1 ORDER BY is_primary DESC, first_name ASC")
    suspend fun getContactsByClient(clientId: String): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE client_id = :clientId AND is_active = 1 ORDER BY is_primary DESC, first_name ASC")
    fun getContactsByClientFlow(clientId: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE client_id = :clientId AND is_active = 1 ORDER BY is_primary DESC, first_name ASC")
    suspend fun getActiveContactsByClient(clientId: String): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("UPDATE contacts SET is_active = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun softDeleteContact(id: String, timestamp: Long)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContact(id: String)

    // ===== DELETE — TWO-STAGE =====

    /**
     * Stage 1: deactivate a single contact.
     * Called by [ContactRepositoryImpl.deactivateContact].
     */
    @Query("UPDATE contacts SET is_active = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun deactivateContact(id: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Stage 2: mark a single contact as deleted for server sync.
     * Called by [ContactRepositoryImpl.markContactDeleted].
     */
    @Query("UPDATE contacts SET is_deleted = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun markContactDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    // ===== PRIMARY CONTACT MANAGEMENT =====

    @Query("SELECT * FROM contacts WHERE client_id = :clientId AND is_primary = 1 AND is_active = 1 LIMIT 1")
    suspend fun getPrimaryContact(clientId: String): ContactEntity?

    @Transaction
    suspend fun setPrimaryContact(clientId: String, contactId: String, now: Long = Clock.System.now().toEpochMilliseconds()) {
        // Remove primary flag from all contacts of this client
        clearPrimaryContacts(clientId, now)
        // Set new primary contact
        setPrimaryContactFlag(contactId, true, now)
    }

    @Query("UPDATE contacts SET is_primary = 0, updated_at = :now WHERE client_id = :clientId AND is_primary = 1")
//        "UPDATE contacts SET is_primary = 0 WHERE client_id = :clientId")
    suspend fun clearPrimaryContacts(clientId: String, now:Long)

    @Query("UPDATE contacts SET is_primary = :isPrimary, updated_at = :now WHERE id = :contactId")
    suspend fun setPrimaryContactFlag(contactId: String, isPrimary: Boolean, now: Long)

    // ===== SEARCH & FILTER =====

    @Query("""
        SELECT * FROM contacts 
        WHERE is_active = 1 
        AND (first_name LIKE '%' || :query || '%' 
             OR last_name LIKE '%' || :query || '%'
             OR phone LIKE '%' || :query || '%'
             OR mobile_phone LIKE '%' || :query || '%'
             OR email LIKE '%' || :query || '%'
             OR role LIKE '%' || :query || '%'
             OR department LIKE '%' || :query || '%')
        ORDER BY is_primary DESC, first_name ASC
    """)
    suspend fun searchContacts(query: String): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE role = :role AND is_active = 1 ORDER BY first_name ASC")
    suspend fun getContactsByRole(role: String): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE department = :department AND is_active = 1 ORDER BY first_name ASC")
    suspend fun getContactsByDepartment(department: String): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE client_id = :clientId AND preferred_contact_method = :contactMethod AND is_active = 1 ORDER BY is_primary DESC, first_name ASC")
    suspend fun getContactsByPreferredMethod(clientId: String, contactMethod: String): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE email = :email AND is_active = 1 LIMIT 1")
    suspend fun getContactByEmail(email: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE phone = :phone OR mobile_phone = :phone AND is_active = 1 LIMIT 1")
    suspend fun getContactByPhone(phone: String): ContactEntity?

    // ===== VALIDATION =====

    @Query("SELECT EXISTS(SELECT 1 FROM contacts WHERE email = :email AND id != :excludeId AND is_active = 1)")
    suspend fun isEmailTaken(email: String, excludeId: String = ""): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM contacts WHERE (phone = :phone OR mobile_phone = :phone) AND id != :excludeId AND is_active = 1)")
    suspend fun isPhoneTaken(phone: String, excludeId: String = ""): Boolean

    // ===== STATISTICS =====

    @Query("SELECT COUNT(*) FROM contacts WHERE client_id = :clientId AND is_active = 1")
    suspend fun getContactsCountForClient(clientId: String): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE is_active = 1")
    suspend fun getActiveContactsCount(): Int

    @Query("SELECT DISTINCT role FROM contacts WHERE role IS NOT NULL AND role != '' AND is_active = 1 ORDER BY role ASC")
    suspend fun getAllRoles(): List<String>

    @Query("SELECT DISTINCT department FROM contacts WHERE department IS NOT NULL AND department != '' AND is_active = 1 ORDER BY department ASC")
    suspend fun getAllDepartments(): List<String>

    @Query("""
        SELECT preferred_contact_method, COUNT(*) as count 
        FROM contacts 
        WHERE is_active = 1 AND preferred_contact_method IS NOT NULL 
        GROUP BY preferred_contact_method
    """)
    suspend fun getContactMethodStats(): List<ContactMethodStatResult>

    // ===== ADVANCED QUERIES =====

    @Query("""
        SELECT * FROM contacts 
        WHERE is_active = 1 
        AND (phone IS NOT NULL AND phone != '' 
             OR mobile_phone IS NOT NULL AND mobile_phone != ''
             OR email IS NOT NULL AND email != '')
        ORDER BY is_primary DESC, first_name ASC
    """)
    suspend fun getContactsWithAnyContactInfo(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE client_id = :clientId AND is_primary = 1 AND is_active = 1 LIMIT 1")
    suspend fun getPrimaryContactForClients(clientId: String): ContactEntity?

    // ===== MAINTENANCE =====

    @Query("UPDATE contacts SET updated_at = :timestamp WHERE id = :id")
    suspend fun touchContact(id: String, timestamp: Long = System.currentTimeMillis())

    // ===== BULK OPERATIONS =====

    @Query("DELETE FROM contacts WHERE client_id = :clientId")
    suspend fun deleteAllContactsForClient(clientId: String)

    @Query("UPDATE contacts SET is_active = 0, updated_at = :timestamp WHERE client_id = :clientId")
    suspend fun softDeleteAllContactsForClient(clientId: String, timestamp: Long)

    // ============================================================
    // BACKUP METHODS
    // ============================================================

    @Query("SELECT * FROM contacts ORDER BY created_at ASC")
    suspend fun getAllForBackup(): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(contacts: List<ContactEntity>)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun count(): Int
}

/**
 * Result class per query statistiche metodi di contatto
 */
data class ContactMethodStatResult(
    val preferred_contact_method: String,  // ✅ Snake case per match SQL
    val count: Int
)