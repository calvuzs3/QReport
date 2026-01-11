package net.calvuz.qreport.client.contact.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.model.ContactMethod

/**
 * Repository interface per gestione contatti
 * Definisce il contratto per accesso ai dati dei contatti
 * Implementazione nel data layer
 *
 * Updated to use QrResult<T, QrError> pattern
 */
interface ContactRepository {

    // ===== CRUD OPERATIONS =====

    suspend fun getAllContacts(): QrResult<List<Contact>, QrError>
    suspend fun getActiveContacts(): QrResult<List<Contact>, QrError>
    suspend fun getContactById(id: String): QrResult<Contact?, QrError>
    suspend fun createContact(contact: Contact): QrResult<Contact, QrError>
    suspend fun updateContact(contact: Contact): QrResult<Contact, QrError>
    suspend fun deleteContact(id: String): QrResult<Unit, QrError>

    // ===== CLIENT RELATED =====

    suspend fun getContactsByClient(clientId: String): QrResult<List<Contact>, QrError>
    fun getContactsByClientFlow(clientId: String): Flow<List<Contact>>
    suspend fun getActiveContactsByClient(clientId: String): QrResult<List<Contact>, QrError>
    suspend fun getPrimaryContact(clientId: String): QrResult<Contact?, QrError>

    // ===== FLOW OPERATIONS (REACTIVE) =====

    fun getAllActiveContactsFlow(): Flow<List<Contact>>
    fun getContactByIdFlow(id: String): Flow<Contact?>

    // ===== SEARCH & FILTER =====

    suspend fun searchContacts(query: String): QrResult<List<Contact>, QrError>
    suspend fun getContactsByRole(role: String): QrResult<List<Contact>, QrError>
    suspend fun getContactsByDepartment(department: String): QrResult<List<Contact>, QrError>
    suspend fun getContactsByPreferredMethod(clientId: String, contactMethod: ContactMethod): QrResult<List<Contact>, QrError>
    suspend fun getContactByEmail(email: String): QrResult<Contact?, QrError>
    suspend fun getContactByPhone(phone: String): QrResult<Contact?, QrError>

    // ===== VALIDATION =====

    suspend fun isEmailTaken(email: String, excludeId: String = ""): QrResult<Boolean, QrError>
    suspend fun isPhoneTaken(phone: String, excludeId: String = ""): QrResult<Boolean, QrError>
    suspend fun hasPrimaryContact(clientId: String, excludeId: String = ""): QrResult<Boolean, QrError>

    // ===== STATISTICS =====

    suspend fun getActiveContactsCount(): QrResult<Int, QrError>
    suspend fun getContactsCountByClient(clientId: String): QrResult<Int, QrError>
    suspend fun getAllRoles(): QrResult<List<String>, QrError>
    suspend fun getAllDepartments(): QrResult<List<String>, QrError>
    suspend fun getContactMethodStats(): QrResult<Map<ContactMethod, Int>, QrError>

    // ===== BULK OPERATIONS =====

    suspend fun createContacts(contacts: List<Contact>): QrResult<List<Contact>, QrError>
    suspend fun setPrimaryContact(clientId: String, contactId: String): QrResult<Unit, QrError>

    // ===== ADVANCED QUERIES =====

    suspend fun getContactsWithAnyContactInfo(): QrResult<List<Contact>, QrError>
    suspend fun getPrimaryContactForClients(clientId: String): QrResult<Contact?, QrError>

    // ===== MAINTENANCE =====

    suspend fun touchContact(id: String): QrResult<Unit, QrError>
}