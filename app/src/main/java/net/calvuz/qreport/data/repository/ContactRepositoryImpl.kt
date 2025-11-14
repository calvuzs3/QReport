package net.calvuz.qreport.data.repository

import net.calvuz.qreport.data.local.dao.ContactDao
import net.calvuz.qreport.data.mapper.ContactMapper
import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.domain.model.client.ContactMethod
import net.calvuz.qreport.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Implementazione del repository per gestione contatti
 * Utilizza Room DAO per persistenza e mapper per conversioni domain ↔ entity
 */
class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao,
    private val contactMapper: ContactMapper
) : ContactRepository {

    // ===== CRUD OPERATIONS =====

    override suspend fun getAllContacts(): Result<List<Contact>> {
        return try {
            val entities = contactDao.getAllActiveContacts() // Usa il metodo DAO esistente
            val contacts = contactMapper.toDomainList(entities)
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveContacts(): Result<List<Contact>> {
        return try {
            val entities = contactDao.getAllActiveContacts()
            val contacts = contactMapper.toDomainList(entities)
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getContactById(id: String): Result<Contact?> {
        return try {
            val entity = contactDao.getContactById(id)
            val contact = entity?.let { contactMapper.toDomain(it) }
            Result.success(contact)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createContact(contact: Contact): Result<Unit> {
        return try {
            val entity = contactMapper.toEntity(contact)
            contactDao.insertContact(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateContact(contact: Contact): Result<Unit> {
        return try {
            val entity = contactMapper.toEntity(contact)
            contactDao.updateContact(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteContact(id: String): Result<Unit> {
        return try {
            contactDao.softDeleteContact(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== CLIENT RELATED =====

    override suspend fun getContactsByClient(clientId: String): Result<List<Contact>> {
        return try {
            val entities = contactDao.getContactsForClient(clientId) // Nome DAO corretto
            val contacts = contactMapper.toDomainList(entities)
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getContactsByClientFlow(clientId: String): Flow<List<Contact>> {
        return contactDao.getContactsForClientFlow(clientId).map { entities -> // Nome DAO corretto
            contactMapper.toDomainList(entities)
        }
    }

    override suspend fun getActiveContactsByClient(clientId: String): Result<List<Contact>> {
        return try {
            val entities = contactDao.getContactsForClient(clientId) // Usa stesso metodo, già filtrato per is_active
            val contacts = contactMapper.toDomainList(entities)
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPrimaryContact(clientId: String): Result<Contact?> {
        return try {
            val entity = contactDao.getPrimaryContact(clientId)
            val contact = entity?.let { contactMapper.toDomain(it) }
            Result.success(contact)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== FLOW OPERATIONS (REACTIVE) =====

    override fun getAllActiveContactsFlow(): Flow<List<Contact>> {
        // ContactDao non ha getAllActiveContactsFlow, implemento con polling
        return flow {
            while (true) {
                try {
                    val contacts = getActiveContacts().getOrThrow()
                    emit(contacts)
                    delay(1000) // Polling ogni secondo
                } catch (e: Exception) {
                    emit(emptyList())
                }
            }
        }
    }

    override fun getContactByIdFlow(id: String): Flow<Contact?> {
        return contactDao.getContactByIdFlow(id).map { entity ->
            entity?.let { contactMapper.toDomain(it) }
        }
    }

    // ===== SEARCH & FILTER =====

    override suspend fun searchContacts(query: String): Result<List<Contact>> {
        return try {
            val entities = contactDao.searchContacts(query)
            val contacts = contactMapper.toDomainList(entities)
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getContactsByRole(role: String): Result<List<Contact>> {
        return try {
            val entities = contactDao.getContactsByRole(role)
            val contacts = contactMapper.toDomainList(entities)
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getContactsByDepartment(department: String): Result<List<Contact>> {
        return try {
            val entities = contactDao.getContactsByDepartment(department)
            val contacts = contactMapper.toDomainList(entities)
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getContactsByPreferredMethod(clientId: String, contactMethod: ContactMethod): Result<List<Contact>> {
        return try {
            val entities = contactDao.getContactsByPreferredMethod(clientId, contactMethod.name)
            val contacts = contactMapper.toDomainList(entities)
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getContactByEmail(email: String): Result<Contact?> {
        return try {
            val entity = contactDao.getContactByEmail(email)
            val contact = entity?.let { contactMapper.toDomain(it) }
            Result.success(contact)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getContactByPhone(phone: String): Result<Contact?> {
        return try {
            val entity = contactDao.getContactByPhone(phone)
            val contact = entity?.let { contactMapper.toDomain(it) }
            Result.success(contact)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== VALIDATION =====

    override suspend fun isEmailTaken(email: String, excludeId: String): Result<Boolean> {
        return try {
            val isTaken = contactDao.isEmailTakenGlobally(email, excludeId) // Usa metodo DAO esistente
            Result.success(isTaken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isPhoneTaken(phone: String, excludeId: String): Result<Boolean> {
        return try {
            // ContactDao non ha isPhoneTaken, uso implementazione custom
            val contact = contactDao.getContactByPhone(phone)
            val isTaken = contact != null && contact.id != excludeId
            Result.success(isTaken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasPrimaryContact(clientId: String, excludeId: String): Result<Boolean> {
        return try {
            val primaryContact = contactDao.getPrimaryContact(clientId)
            val hasPrimary = primaryContact != null && primaryContact.id != excludeId
            Result.success(hasPrimary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== STATISTICS =====

    override suspend fun getActiveContactsCount(): Result<Int> {
        return try {
            val count = contactDao.getActiveContactsCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getContactsCountByClient(clientId: String): Result<Int> {
        return try {
            val count = contactDao.getContactsCountForClient(clientId) // Nome DAO corretto
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllRoles(): Result<List<String>> {
        return try {
            val roles = contactDao.getAllRoles()
            Result.success(roles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllDepartments(): Result<List<String>> {
        return try {
            val departments = contactDao.getAllDepartments()
            Result.success(departments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getContactMethodStats(): Result<Map<ContactMethod, Int>> {
        return try {
            val methods = contactDao.getAllPreferredContactMethods() // Usa metodo DAO esistente
            val stats = methods.mapNotNull { methodString ->
                try {
                    ContactMethod.valueOf(methodString) to contactDao.getContactsCountByRole(methodString) // Approx
                } catch (e: IllegalArgumentException) {
                    null
                }
            }.toMap()
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== BULK OPERATIONS =====

    override suspend fun createContacts(contacts: List<Contact>): Result<Unit> {
        return try {
            val entities = contactMapper.toEntityList(contacts)
            contactDao.insertContacts(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setPrimaryContact(clientId: String, contactId: String): Result<Unit> {
        return try {
            contactDao.setPrimaryContact(clientId, contactId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== ADVANCED QUERIES =====

    override suspend fun getContactsWithAnyContactInfo(): Result<List<Contact>> {
        return try {
            val entities = contactDao.getContactsWithAnyContactInfo()
            val contacts = contactMapper.toDomainList(entities)
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getPrimaryContactForClients(clientId: String): Result<Contact> {
        return try {
            val entity = contactDao.getPrimaryContact(clientId)
            val contact = entity?.let { contactMapper.toDomain(it) }
            Result.success(contact)
        } catch (e: Exception) {
            Result.failure(e)
        } as Result<Contact>
    }

    // ===== MAINTENANCE =====

    override suspend fun touchContact(id: String): Result<Unit> {
        return try {
            contactDao.touchContact(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}