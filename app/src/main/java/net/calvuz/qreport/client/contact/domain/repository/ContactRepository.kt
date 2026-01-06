package net.calvuz.qreport.client.contact.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.model.ContactMethod

/**
 * Repository interface per gestione contatti
 * Definisce il contratto per accesso ai dati dei contatti
 * Implementazione nel data layer
 */
interface ContactRepository {

    // ===== CRUD OPERATIONS =====

    suspend fun getAllContacts(): Result<List<Contact>>
    suspend fun getActiveContacts(): Result<List<Contact>>
    suspend fun getContactById(id: String): Result<Contact?>
    suspend fun createContact(contact: Contact): Result<Unit>
    suspend fun updateContact(contact: Contact): Result<Unit>
    suspend fun deleteContact(id: String): Result<Unit>

    // ===== CLIENT RELATED =====

    suspend fun getContactsByClient(clientId: String): Result<List<Contact>>
    fun getContactsByClientFlow(clientId: String): Flow<List<Contact>>
    suspend fun getActiveContactsByClient(clientId: String): Result<List<Contact>>
    suspend fun getPrimaryContact(clientId: String): Result<Contact?>

    // ===== FLOW OPERATIONS (REACTIVE) =====

    fun getAllActiveContactsFlow(): Flow<List<Contact>>
    fun getContactByIdFlow(id: String): Flow<Contact?>

    // ===== SEARCH & FILTER =====

    suspend fun searchContacts(query: String): Result<List<Contact>>
    suspend fun getContactsByRole(role: String): Result<List<Contact>>
    suspend fun getContactsByDepartment(department: String): Result<List<Contact>>
    suspend fun getContactsByPreferredMethod(clientId: String, contactMethod: ContactMethod): Result<List<Contact>>
    suspend fun getContactByEmail(email: String): Result<Contact?>
    suspend fun getContactByPhone(phone: String): Result<Contact?>

    // ===== VALIDATION =====

    suspend fun isEmailTaken(email: String, excludeId: String = ""): Result<Boolean>
    suspend fun isPhoneTaken(phone: String, excludeId: String = ""): Result<Boolean>
    suspend fun hasPrimaryContact(clientId: String, excludeId: String = ""): Result<Boolean>

    // ===== STATISTICS =====

    suspend fun getActiveContactsCount(): Result<Int>
    suspend fun getContactsCountByClient(clientId: String): Result<Int>
    suspend fun getAllRoles(): Result<List<String>>
    suspend fun getAllDepartments(): Result<List<String>>
    suspend fun getContactMethodStats(): Result<Map<ContactMethod, Int>>

    // ===== BULK OPERATIONS =====

    suspend fun createContacts(contacts: List<Contact>): Result<Unit>
    suspend fun setPrimaryContact(clientId: String, contactId: String): Result<Unit>

    // ===== ADVANCED QUERIES =====

    suspend fun getContactsWithAnyContactInfo(): Result<List<Contact>>
    suspend fun getPrimaryContactForClients(clientId: String): Result<Contact?>

    // ===== MAINTENANCE =====

    suspend fun touchContact(id: String): Result<Unit>
}