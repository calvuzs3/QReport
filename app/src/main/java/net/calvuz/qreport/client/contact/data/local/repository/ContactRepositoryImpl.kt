package net.calvuz.qreport.client.contact.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.data.local.dao.ContactDao
import net.calvuz.qreport.client.contact.data.local.mapper.ContactMapper
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.model.ContactMethod
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Implementazione del repository per gestione contatti
 * Utilizza Room DAO per persistenza e mapper per conversioni domain ↔ entity
 *
 * Updated to use QrResult<T, QrError> pattern with comprehensive error handling
 */
class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao,
    private val contactMapper: ContactMapper
) : ContactRepository {

    // ===== CRUD OPERATIONS =====

    override suspend fun getAllContacts(): QrResult<List<Contact>, QrError> {
        return try {
            Timber.d("ContactRepository: Getting all contacts")
            val entities = contactDao.getActiveContacts() // Uses existing DAO method
            val contacts = contactMapper.toDomainList(entities)
            Timber.d("ContactRepository: Retrieved ${contacts.size} contacts")
            QrResult.Success(contacts)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting all contacts")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    override suspend fun getActiveContacts(): QrResult<List<Contact>, QrError> {
        return try {
            Timber.d("ContactRepository: Getting active contacts")
            val entities = contactDao.getActiveContacts()
            val contacts = contactMapper.toDomainList(entities)
            Timber.d("ContactRepository: Retrieved ${contacts.size} active contacts")
            QrResult.Success(contacts)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting active contacts")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    override suspend fun getContactById(id: String): QrResult<Contact?, QrError> {
        return try {
            if (id.isBlank()) {
                Timber.w("ContactRepository: getContactById called with blank id")
                return QrResult.Error(QrError.ValidationError.EmptyField(id.toString()))
            }

            Timber.d("ContactRepository: Getting contact by id: $id")
            val entity = contactDao.getContactById(id)
            val contact = entity?.let { contactMapper.toDomain(it) }

            if (contact != null) {
                Timber.d("ContactRepository: Contact found: $id")
            } else {
                Timber.d("ContactRepository: Contact not found: $id")
            }

            QrResult.Success(contact)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting contact by id: $id")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    override suspend fun createContact(contact: Contact): QrResult<Contact, QrError> {
        return try {
            Timber.d("ContactRepository: Creating contact: ${contact.id}")
            val entity = contactMapper.toEntity(contact)
            contactDao.insertContact(entity)

            // Return the created contact (could be enhanced with DB-generated fields)
            Timber.d("ContactRepository: Contact created successfully: ${contact.id}")
            QrResult.Success(contact)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception creating contact: ${contact.id}")
            QrResult.Error(QrError.DatabaseError.InsertFailed())
        }
    }

    override suspend fun updateContact(contact: Contact): QrResult<Contact, QrError> {
        return try {
            Timber.d("ContactRepository: Updating contact: ${contact.id}")
            val entity = contactMapper.toEntity(contact)
            contactDao.updateContact(entity)

                Timber.d("ContactRepository: Contact updated successfully: ${contact.id}")
                QrResult.Success(contact)

        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception updating contact: ${contact.id}")
            QrResult.Error(QrError.DatabaseError.InsertFailed())
        }
    }

    override suspend fun deleteContact(id: String): QrResult<Unit, QrError> {
        return try {
            if (id.isBlank()) {
                Timber.w("ContactRepository: deleteContact called with blank id")
                return QrResult.Error(QrError.ValidationError.EmptyField(id.toString()))
            }

            Timber.d("ContactRepository: Soft deleting contact: $id")
            contactDao.softDeleteContact(id, System.currentTimeMillis())

                Timber.d("ContactRepository: Contact soft deleted successfully: $id")
                QrResult.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception soft deleting contact: $id")
            QrResult.Error(QrError.DatabaseError.InsertFailed())
        }
    }

    // ===== CLIENT RELATED =====

    override suspend fun getContactsByClient(clientId: String): QrResult<List<Contact>, QrError> {
        return try {
            if (clientId.isBlank()) {
                Timber.w("ContactRepository: getContactsByClient called with blank clientId")
                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
            }

            Timber.d("ContactRepository: Getting contacts for client: $clientId")
            val entities = contactDao.getContactsByClient(clientId)
            val contacts = contactMapper.toDomainList(entities)
            Timber.d("ContactRepository: Retrieved ${contacts.size} contacts for client: $clientId")
            QrResult.Success(contacts)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting contacts for client: $clientId")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    override fun getContactsByClientFlow(clientId: String): Flow<List<Contact>> {
        return contactDao.getContactsByClientFlow(clientId)
            .map { entities ->
                contactMapper.toDomainList(entities)
            }
            .catch { exception ->
                Timber.e(exception, "ContactRepository: Exception in contacts flow for client: $clientId")
                emit(emptyList()) // Graceful degradation
            }
    }

    override suspend fun getActiveContactsByClient(clientId: String): QrResult<List<Contact>, QrError> {
        return try {
            if (clientId.isBlank()) {
                Timber.w("ContactRepository: getActiveContactsByClient called with blank clientId")
                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
            }

            Timber.d("ContactRepository: Getting active contacts for client: $clientId")
            val entities = contactDao.getActiveContactsByClient(clientId)
            val contacts = contactMapper.toDomainList(entities)
            Timber.d("ContactRepository: Retrieved ${contacts.size} active contacts for client: $clientId")
            QrResult.Success(contacts)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting active contacts for client: $clientId")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    override suspend fun getPrimaryContact(clientId: String): QrResult<Contact?, QrError> {
        return try {
            if (clientId.isBlank()) {
                Timber.w("ContactRepository: getPrimaryContact called with blank clientId")
                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
            }

            Timber.d("ContactRepository: Getting primary contact for client: $clientId")
            val entity = contactDao.getPrimaryContact(clientId)
            val contact = entity?.let { contactMapper.toDomain(it) }

            if (contact != null) {
                Timber.d("ContactRepository: Primary contact found for client: $clientId")
            } else {
                Timber.d("ContactRepository: No primary contact found for client: $clientId")
            }

            QrResult.Success(contact)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting primary contact for client: $clientId")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    // ===== FLOW OPERATIONS (REACTIVE) =====

    override fun getAllActiveContactsFlow(): Flow<List<Contact>> {
        return contactDao.getAllActiveContactsFlow()
            .map { entities ->
                contactMapper.toDomainList(entities)
            }
            .catch { exception ->
                Timber.e(exception, "ContactRepository: Exception in all active contacts flow")
                emit(emptyList()) // Graceful degradation
            }
    }

    override fun getContactByIdFlow(id: String): Flow<Contact?> {
        return contactDao.getContactByIdFlow(id)
            .map { entity ->
                entity?.let { contactMapper.toDomain(it) }
            }
            .catch { exception ->
                Timber.e(exception, "ContactRepository: Exception in contact flow for id: $id")
                emit(null) // Graceful degradation
            }
    }

    // ===== SEARCH & FILTER =====

    override suspend fun searchContacts(query: String): QrResult<List<Contact>, QrError> {
        return try {
            if (query.isBlank()) {
                Timber.w("ContactRepository: searchContacts called with blank query")
                return QrResult.Error(QrError.ValidationError.EmptyField(query.toString()))
            }

            Timber.d("ContactRepository: Searching contacts with query: $query")
            val entities = contactDao.searchContacts(query)
            val contacts = contactMapper.toDomainList(entities)
            Timber.d("ContactRepository: Found ${contacts.size} contacts for query: $query")
            QrResult.Success(contacts)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception searching contacts with query: $query")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    override suspend fun getContactsByRole(role: String): QrResult<List<Contact>, QrError> {
        return try {
            if (role.isBlank()) {
                Timber.w("ContactRepository: getContactsByRole called with blank role")
                return QrResult.Error(QrError.ValidationError.EmptyField(role.toString()))
            }

            Timber.d("ContactRepository: Getting contacts by role: $role")
            val entities = contactDao.getContactsByRole(role)
            val contacts = contactMapper.toDomainList(entities)
            Timber.d("ContactRepository: Found ${contacts.size} contacts with role: $role")
            QrResult.Success(contacts)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting contacts by role: $role")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    override suspend fun getContactsByDepartment(department: String): QrResult<List<Contact>, QrError> {
        return try {
            if (department.isBlank()) {
                Timber.w("ContactRepository: getContactsByDepartment called with blank department")
                return QrResult.Error(QrError.ValidationError.EmptyField(department.toString()))
            }

            Timber.d("ContactRepository: Getting contacts by department: $department")
            val entities = contactDao.getContactsByDepartment(department)
            val contacts = contactMapper.toDomainList(entities)
            Timber.d("ContactRepository: Found ${contacts.size} contacts in department: $department")
            QrResult.Success(contacts)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting contacts by department: $department")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    override suspend fun getContactsByPreferredMethod(clientId: String, contactMethod: ContactMethod): QrResult<List<Contact>, QrError> {
        return try {
            if (clientId.isBlank()) {
                Timber.w("ContactRepository: getContactsByPreferredMethod called with blank clientId")
                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
            }

            Timber.d("ContactRepository: Getting contacts by preferred method: $contactMethod for client: $clientId")
            val entities = contactDao.getContactsByPreferredMethod(clientId, contactMethod.name)
            val contacts = contactMapper.toDomainList(entities)
            Timber.d("ContactRepository: Found ${contacts.size} contacts with preferred method $contactMethod for client: $clientId")
            QrResult.Success(contacts)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting contacts by preferred method: $contactMethod for client: $clientId")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    override suspend fun getContactByEmail(email: String): QrResult<Contact?, QrError> {
        return try {
            if (email.isBlank()) {
                Timber.w("ContactRepository: getContactByEmail called with blank email")
                return QrResult.Error(QrError.ValidationError.EmptyField(email.toString()))
            }

            Timber.d("ContactRepository: Getting contact by email: $email")
            val entity = contactDao.getContactByEmail(email)
            val contact = entity?.let { contactMapper.toDomain(it) }

            if (contact != null) {
                Timber.d("ContactRepository: Contact found with email: $email")
            } else {
                Timber.d("ContactRepository: No contact found with email: $email")
            }

            QrResult.Success(contact)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting contact by email: $email")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    override suspend fun getContactByPhone(phone: String): QrResult<Contact?, QrError> {
        return try {
            if (phone.isBlank()) {
                Timber.w("ContactRepository: getContactByPhone called with blank phone")
                return QrResult.Error(QrError.ValidationError.EmptyField(phone.toString()))
            }

            Timber.d("ContactRepository: Getting contact by phone: $phone")
            val entity = contactDao.getContactByPhone(phone)
            val contact = entity?.let { contactMapper.toDomain(it) }

            if (contact != null) {
                Timber.d("ContactRepository: Contact found with phone: $phone")
            } else {
                Timber.d("ContactRepository: No contact found with phone: $phone")
            }

            QrResult.Success(contact)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting contact by phone: $phone")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    // ===== VALIDATION =====

    override suspend fun isEmailTaken(email: String, excludeId: String): QrResult<Boolean, QrError> {
        return try {
            if (email.isBlank()) {
                Timber.w("ContactRepository: isEmailTaken called with blank email")
                return QrResult.Error(QrError.ValidationError.EmptyField(email.toString()))
            }

            Timber.d("ContactRepository: Checking if email is taken: $email (exclude: $excludeId)")
            val isTaken = contactDao.isEmailTaken(email, excludeId)
            Timber.d("ContactRepository: Email $email is ${if (isTaken) "taken" else "available"}")
            QrResult.Success(isTaken)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception checking if email is taken: $email")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    override suspend fun isPhoneTaken(phone: String, excludeId: String): QrResult<Boolean, QrError> {
        return try {
            if (phone.isBlank()) {
                Timber.w("ContactRepository: isPhoneTaken called with blank phone")
                return QrResult.Error(QrError.ValidationError.EmptyField(phone.toString()))
            }

            Timber.d("ContactRepository: Checking if phone is taken: $phone (exclude: $excludeId)")
            val isTaken = contactDao.isPhoneTaken(phone, excludeId)
            Timber.d("ContactRepository: Phone $phone is ${if (isTaken) "taken" else "available"}")
            QrResult.Success(isTaken)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception checking if phone is taken: $phone")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    override suspend fun hasPrimaryContact(clientId: String, excludeId: String): QrResult<Boolean, QrError> {
        return try {
            if (clientId.isBlank()) {
                Timber.w("ContactRepository: hasPrimaryContact called with blank clientId")
                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
            }

            Timber.d("ContactRepository: Checking if client has primary contact: $clientId (exclude: $excludeId)")
            val primaryContact = contactDao.getPrimaryContact(clientId)
            val hasPrimary = primaryContact != null && primaryContact.id != excludeId
            Timber.d("ContactRepository: Client $clientId ${if (hasPrimary) "has" else "does not have"} primary contact")
            QrResult.Success(hasPrimary)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception checking primary contact for client: $clientId")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    // ===== STATISTICS =====

    override suspend fun getActiveContactsCount(): QrResult<Int, QrError> {
        return try {
            Timber.d("ContactRepository: Getting active contacts count")
            val count = contactDao.getActiveContactsCount()
            Timber.d("ContactRepository: Active contacts count: $count")
            QrResult.Success(count)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting active contacts count")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    override suspend fun getContactsCountByClient(clientId: String): QrResult<Int, QrError> {
        return try {
            if (clientId.isBlank()) {
                Timber.w("ContactRepository: getContactsCountByClient called with blank clientId")
                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
            }

            Timber.d("ContactRepository: Getting contacts count for client: $clientId")
            val count = contactDao.getContactsCountForClient(clientId) // Correct DAO method name
            Timber.d("ContactRepository: Contacts count for client $clientId: $count")
            QrResult.Success(count)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting contacts count for client: $clientId")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    override suspend fun getAllRoles(): QrResult<List<String>, QrError> {
        return try {
            Timber.d("ContactRepository: Getting all roles")
            val roles = contactDao.getAllRoles()
            Timber.d("ContactRepository: Found ${roles.size} roles")
            QrResult.Success(roles)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting all roles")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    override suspend fun getAllDepartments(): QrResult<List<String>, QrError> {
        return try {
            Timber.d("ContactRepository: Getting all departments")
            val departments = contactDao.getAllDepartments()
            Timber.d("ContactRepository: Found ${departments.size} departments")
            QrResult.Success(departments)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting all departments")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    override suspend fun getContactMethodStats(): QrResult<Map<ContactMethod, Int>, QrError> {
        return try {
            Timber.d("ContactRepository: Getting contact method statistics")
            val methodStats = contactDao.getContactMethodStats()
            val stats = methodStats.mapNotNull { result ->
                try {
                    ContactMethod.valueOf(result.preferred_contact_method) to result.count
                } catch (_: IllegalArgumentException) {
                    Timber.w("ContactRepository: Unknown contact method in stats: ${result.preferred_contact_method}")
                    null
                }
            }.toMap()
            Timber.d("ContactRepository: Contact method stats: $stats")
            QrResult.Success(stats)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting contact method statistics")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    // ===== BULK OPERATIONS =====

    override suspend fun createContacts(contacts: List<Contact>): QrResult<List<Contact>, QrError> {
        return try {
            if (contacts.isEmpty()) {
                Timber.w("ContactRepository: createContacts called with empty list")
                return QrResult.Success(emptyList())
            }

            Timber.d("ContactRepository: Creating ${contacts.size} contacts")
            val entities = contactMapper.toEntityList(contacts)
            contactDao.insertContacts(entities)
            Timber.d("ContactRepository: Successfully created ${contacts.size} contacts")
            QrResult.Success(contacts)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception creating ${contacts.size} contacts")
            QrResult.Error(QrError.DatabaseError.InsertFailed())
        }
    }

    override suspend fun setPrimaryContact(clientId: String, contactId: String): QrResult<Unit, QrError> {
        return try {
            if (clientId.isBlank() || contactId.isBlank()) {
                Timber.w("ContactRepository: setPrimaryContact called with blank clientId or contactId")
                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
            }

            Timber.d("ContactRepository: Setting primary contact $contactId for client $clientId")
            contactDao.setPrimaryContact(clientId, contactId)

                Timber.d("ContactRepository: Primary contact set successfully: $contactId for client: $clientId")
                QrResult.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception setting primary contact $contactId for client $clientId")
            QrResult.Error(QrError.DatabaseError.InsertFailed())
        }
    }

    // ===== ADVANCED QUERIES =====

    override suspend fun getContactsWithAnyContactInfo(): QrResult<List<Contact>, QrError> {
        return try {
            Timber.d("ContactRepository: Getting contacts with any contact info")
            val entities = contactDao.getContactsWithAnyContactInfo()
            val contacts = contactMapper.toDomainList(entities)
            Timber.d("ContactRepository: Found ${contacts.size} contacts with contact info")
            QrResult.Success(contacts)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting contacts with any contact info")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    override suspend fun getPrimaryContactForClients(clientId: String): QrResult<Contact?, QrError> {
        return try {
            if (clientId.isBlank()) {
                Timber.w("ContactRepository: getPrimaryContactForClients called with blank clientId")
                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
            }

            Timber.d("ContactRepository: Getting primary contact for client: $clientId")
            val entity = contactDao.getPrimaryContact(clientId)
            val contact = entity?.let { contactMapper.toDomain(it) }

            if (contact != null) {
                Timber.d("ContactRepository: Primary contact found for client: $clientId")
            } else {
                Timber.d("ContactRepository: No primary contact found for client: $clientId")
            }

            QrResult.Success(contact)
        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception getting primary contact for client: $clientId")
            QrResult.Error(QrError.DatabaseError.OperationFailed())
        }
    }

    // ===== MAINTENANCE =====

    override suspend fun touchContact(id: String): QrResult<Unit, QrError> {
        return try {
            if (id.isBlank()) {
                Timber.w("ContactRepository: touchContact called with blank id")
                return QrResult.Error(QrError.ValidationError.EmptyField(id.toString()))
            }

            Timber.d("ContactRepository: Touching contact: $id")
            contactDao.touchContact(id, System.currentTimeMillis())

                Timber.d("ContactRepository: Contact touched successfully: $id")
                QrResult.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "ContactRepository: Exception touching contact: $id")
            QrResult.Error(QrError.DatabaseError.InsertFailed())
        }
    }
}