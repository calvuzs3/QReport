package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Returns the contacts for a given client.
 */
class GetContactsByClientUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
) {

    /**
     * Get all client's contacts
     *
     * @param clientId the client whose contacts to retrieve
     * @return [QrResult.Success] with contacts sorted: primary first, then alphabetical
     */
    suspend operator fun invoke(clientId: String): QrResult<List<Contact>, QrError> {
        return try {
            Timber.v("Getting contacts for client $clientId")

            // Check input
            if (clientId.isBlank()) {
                Timber.w("ClientId is blank")
                return QrResult.Error(QrError.ContactsError.MissingClientId())
            }

            // Get
            when (val result = contactRepository.getContactsByClient(clientId)) {
                is QrResult.Success -> {
                    Timber.d("Successfully retrieved contacts for client $clientId: ${result.data.size}")
                    QrResult.Success(result.data.sortedByNameThenLastname())
                }

                is QrResult.Error -> {
                    Timber.e("Repository error for clientId $clientId: ${result.error}")
                    QrResult.Error(result.error)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, clientId)
            QrResult.Error(QrError.SystemError.UnknownError())
        }
    }

    suspend fun getActive(clientId: String): QrResult<List<Contact>, QrError> {
        Timber.v("Getting active contacts for client $clientId")

        if (clientId.isBlank()) {
            return QrResult.Error(QrError.ContactsError.MissingClientId())
        }
        return when (val result = contactRepository.getActiveContactsByClient(clientId)) {
            is QrResult.Success -> {
               Timber.d("Successfully retrieved active contacts for client $clientId: ${result.data.size}")
                QrResult.Success(result.data.sortedByNameThenLastname())
            }

            is QrResult.Error -> {
                Timber.d("Repository error for clientId $clientId: ${result.error}")
                QrResult.Error(result.error)
            }
        }
    }

    private fun List<Contact>.sortedByNameThenLastname(): List<Contact> =
        sortedWith(
            compareBy<Contact> { it.firstName.lowercase() }
                .thenBy { it.lastName?.lowercase()}
        )

//    /**
//     * ✅ METHOD 2/8: Osserva tutti i contatti di un cliente (Flow reattivo)
//     *
//     * @param clientId the client whose contacts to retrieve
//     * @return Flow con lista contatti che si aggiorna automaticamente
//     */
//    fun observeContactsByClient(clientId: String): Flow<List<Contact>> {
//        return contactRepository.getContactsByClientFlow(clientId)
//            .map { contacts ->
//                contacts.sortedWith(
//                    compareBy<Contact> { !it.isPrimary }
//                        .thenBy { it.firstName.lowercase() }
//                        .thenBy { it.lastName?.lowercase() ?: "" }
//                )
//            }
//            .catch { exception ->
//                Timber.e(exception, "GetContactsByClientUseCase.observeContactsByClient: Exception in contacts flow for client: $clientId")
//                emit(emptyList()) // Emit empty list on error to avoid crashing UI
//            }
//    }
//
//    /**
//     * ✅ METHOD 3/8: Recupera solo i contatti attivi di un cliente
//     *
//     * @param clientId the client whose contacts to retrieve
//     * @return QrResult.Success con lista contatti attivi
//     */
//    suspend fun getActiveContactsByClient(clientId: String): QrResult<List<Contact>, QrError> {
//        return try {
//            if (clientId.isBlank()) {
//                Timber.w("GetContactsByClientUseCase.getActiveContactsByClient: clientId is blank")
//                return QrResult.Error(QrError.ContactsError.MissingClientId())
//            }
//
//            // Check client exists
//            when (val clientCheck = checkClientExists(clientId)) {
//                is QrResult.Error -> {
//                    Timber.w("GetContactsByClientUseCase.getActiveContactsByClient: Client does not exist: $clientId")
//                    return QrResult.Error(clientCheck.error)
//                }
//                is QrResult.Success -> {
//                    // Client exists, continue
//                }
//            }
//
//            when (val result = contactRepository.getActiveContactsByClient(clientId)) {
//                is QrResult.Success -> {
//                    val contacts = result.data
//                    val sortedContacts = contacts.sortedWith(
//                        compareBy<Contact> { !it.isPrimary }
//                            .thenBy { it.firstName.lowercase() }
//                            .thenBy { it.lastName?.lowercase() ?: "" }
//                    )
//
//                    Timber.d("GetContactsByClientUseCase.getActiveContactsByClient: Retrieved ${sortedContacts.size} active contacts for client: $clientId")
//                    QrResult.Success(sortedContacts)
//                }
//
//                is QrResult.Error -> {
//                    Timber.e("GetContactsByClientUseCase.getActiveContactsByClient: Repository error for clientId $clientId: ${result.error}")
//                    QrResult.Error(result.error)
//                }
//            }
//        } catch (e: Exception) {
//            Timber.e(e, "GetContactsByClientUseCase.getActiveContactsByClient: Exception for client: $clientId")
//            QrResult.Error(QrError.SystemError.UnknownError()    )
//        }
//    }
//
//    /**
//     * ✅ METHOD 4/8: Recupera il contatto primary di un cliente
//     *
//     * @param clientId the client whose contacts to retrieve
//     * @return QrResult.Success con contatto primary se esiste, null se non esiste
//     */
//    suspend fun getPrimaryContact(clientId: String): QrResult<Contact?, QrError> {
//        return try {
//            if (clientId.isBlank()) {
//                Timber.w("GetContactsByClientUseCase.getPrimaryContact: clientId is blank")
//                return QrResult.Error(QrError.ContactsError.MissingClientId())
//            }
//
//            // Check client exists
//            when (val clientCheck = checkClientExists(clientId)) {
//                is QrResult.Error -> {
//                    Timber.w("GetContactsByClientUseCase.getPrimaryContact: Client does not exist: $clientId")
//                    return QrResult.Error(clientCheck.error)
//                }
//                is QrResult.Success -> {
//                    // Client exists, continue
//                }
//            }
//
//            when (val result = contactRepository.getPrimaryContact(clientId)) {
//                is QrResult.Success -> {
//                    val primaryContact = result.data
//                    Timber.d("GetContactsByClientUseCase.getPrimaryContact: Primary contact ${if (primaryContact != null) "found" else "not found"} for client: $clientId")
//                    QrResult.Success(primaryContact)
//                }
//
//                is QrResult.Error -> {
//                    Timber.e("GetContactsByClientUseCase.getPrimaryContact: Repository error for clientId $clientId: ${result.error}")
//                    QrResult.Error(result.error)
//                }
//            }
//        } catch (e: Exception) {
//            Timber.e(e, "GetContactsByClientUseCase.getPrimaryContact: Exception for client: $clientId")
//            QrResult.Error(QrError.SystemError.UnknownError())
//        }
//    }
//
//    /**
//     * ✅ METHOD 5/8: Recupera contatti di un cliente filtrati per ruolo
//     *
//     * @param clientId the client whose contacts to retrieve
//     * @param role Ruolo da filtrare
//     * @return QrResult.Success con lista contatti del ruolo specificato
//     */
//    suspend fun getContactsByRole(clientId: String, role: String): QrResult<List<Contact>, QrError> {
//        return try {
//            if (clientId.isBlank()) {
//                Timber.w("GetContactsByClientUseCase.getContactsByRole: clientId is blank")
//                return QrResult.Error(QrError.ContactsError.MissingClientId())
//            }
//            if (role.isBlank()) {
//                Timber.w("GetContactsByClientUseCase.getContactsByRole: role is blank")
//                return QrResult.Error(QrError.ValidationError.EmptyField(role.toString()))
//            }
//
//            // Check client exists
//            when (val clientCheck = checkClientExists(clientId)) {
//                is QrResult.Error -> {
//                    return QrResult.Error(clientCheck.error)
//                }
//                is QrResult.Success -> {
//                    // Client exists, continue
//                }
//            }
//
//            // Get all contacts then filter by role
//            when (val allContactsResult = invoke(clientId)) {
//                is QrResult.Success -> {
//                    val contacts = allContactsResult.data
//                    val filteredContacts = contacts.filter { contact ->
//                        contact.role?.equals(role, ignoreCase = true) == true
//                    }
//
//                    Timber.d("GetContactsByClientUseCase.getContactsByRole: Found ${filteredContacts.size} contacts with role '$role' for client: $clientId")
//                    QrResult.Success(filteredContacts)
//                }
//
//                is QrResult.Error -> {
//                    Timber.e("GetContactsByClientUseCase.getContactsByRole: Error getting contacts for clientId $clientId: ${allContactsResult.error}")
//                    QrResult.Error(allContactsResult.error)
//                }
//            }
//        } catch (e: Exception) {
//            Timber.e(e, "GetContactsByClientUseCase.getContactsByRole: Exception for client: $clientId, role: $role")
//            QrResult.Error(QrError.SystemError.UnknownError())
//        }
//    }
//
//    /**
//     * ✅ METHOD 6/8: Recupera contatti di un cliente filtrati per dipartimento
//     *
//     * @param clientId the client whose contacts to retrieve
//     * @param department Dipartimento da filtrare
//     * @return QrResult.Success con lista contatti del dipartimento specificato
//     */
//    suspend fun getContactsByDepartment(clientId: String, department: String): QrResult<List<Contact>, QrError> {
//        return try {
//            if (clientId.isBlank()) {
//                Timber.w("GetContactsByClientUseCase.getContactsByDepartment: clientId is blank")
//                return QrResult.Error(QrError.ContactsError.MissingClientId())
//            }
//            if (department.isBlank()) {
//                Timber.w("GetContactsByClientUseCase.getContactsByDepartment: department is blank")
//                return QrResult.Error(QrError.ValidationError.EmptyField(department.toString()))
//            }
//
//            // Check client exists
//            when (val clientCheck = checkClientExists(clientId)) {
//                is QrResult.Error -> {
//                    return QrResult.Error(clientCheck.error)
//                }
//                is QrResult.Success -> {
//                    // Client exists, continue
//                }
//            }
//
//            // Get all contacts then filter by department
//            when (val allContactsResult = invoke(clientId)) {
//                is QrResult.Success -> {
//                    val contacts = allContactsResult.data
//                    val filteredContacts = contacts.filter { contact ->
//                        contact.department?.equals(department, ignoreCase = true) == true
//                    }
//
//                    Timber.d("GetContactsByClientUseCase.getContactsByDepartment: Found ${filteredContacts.size} contacts in department '$department' for client: $clientId")
//                    QrResult.Success(filteredContacts)
//                }
//
//                is QrResult.Error -> {
//                    Timber.e("GetContactsByClientUseCase.getContactsByDepartment: Error getting contacts for clientId $clientId: ${allContactsResult.error}")
//                    QrResult.Error(allContactsResult.error)
//                }
//            }
//        } catch (e: Exception) {
//            Timber.e(e, "GetContactsByClientUseCase.getContactsByDepartment: Exception for client: $clientId, department: $department")
//            QrResult.Error(QrError.SystemError.UnknownError())
//        }
//    }
//
//    /**
//     * ✅ METHOD 7/8: Recupera contatti che hanno informazioni complete per comunicare
//     *
//     * @param clientId the client whose contacts to retrieve
//     * @return QrResult.Success con lista contatti con almeno un metodo di contatto
//     */
//    suspend fun getContactsWithValidContactInfo(clientId: String): QrResult<List<Contact>, QrError> {
//        return try {
//            if (clientId.isBlank()) {
//                Timber.w("GetContactsByClientUseCase.getContactsWithValidContactInfo: clientId is blank")
//                return QrResult.Error(QrError.ContactsError.MissingClientId())
//            }
//
//            // Check client exists
//            when (val clientCheck = checkClientExists(clientId)) {
//                is QrResult.Error -> {
//                    return QrResult.Error(clientCheck.error)
//                }
//                is QrResult.Success -> {
//                    // Client exists, continue
//                }
//            }
//
//            // Get all contacts then filter by valid contact info
//            when (val allContactsResult = invoke(clientId)) {
//                is QrResult.Success -> {
//                    val contacts = allContactsResult.data
//                    val filteredContacts = contacts.filter { contact ->
//                        hasValidContactInfo(contact)
//                    }
//
//                    Timber.d("GetContactsByClientUseCase.getContactsWithValidContactInfo: Found ${filteredContacts.size} contacts with valid contact info for client: $clientId")
//                    QrResult.Success(filteredContacts)
//                }
//
//                is QrResult.Error -> {
//                    Timber.e("GetContactsByClientUseCase.getContactsWithValidContactInfo: Error getting contacts for clientId $clientId: ${allContactsResult.error}")
//                    QrResult.Error(allContactsResult.error)
//                }
//            }
//        } catch (e: Exception) {
//            Timber.e(e, "GetContactsByClientUseCase.getContactsWithValidContactInfo: Exception for client: $clientId")
//            QrResult.Error(QrError.SystemError.UnknownError())
//        }
//    }
//
//    /**
//     * ✅ METHOD 8/8: Conta il numero di contatti per un cliente
//     *
//     * @param clientId the client whose contacts to retrieve
//     * @return QrResult.Success con numero di contatti
//     */
//    suspend fun getContactsCount(clientId: String): QrResult<Int, QrError> {
//        return try {
//            if (clientId.isBlank()) {
//                Timber.w("GetContactsByClientUseCase.getContactsCount: clientId is blank")
//                return QrResult.Error(QrError.ContactsError.MissingClientId())
//            }
//
//            when (val result = contactRepository.getContactsCountByClient(clientId)) {
//                is QrResult.Success -> {
//                    val count = result.data
//                    Timber.d("GetContactsByClientUseCase.getContactsCount: Found $count contacts for client: $clientId")
//                    QrResult.Success(count)
//                }
//
//                is QrResult.Error -> {
//                    Timber.e("GetContactsByClientUseCase.getContactsCount: Repository error for clientId $clientId: ${result.error}")
//                    QrResult.Error(result.error)
//                }
//            }
//        } catch (e: Exception) {
//            Timber.e(e, "GetContactsByClientUseCase.getContactsCount: Exception for client: $clientId")
//            QrResult.Error(QrError.SystemError.UnknownError())
//        }
//    }

//    /**
//     * ✅ HELPER METHOD: Verifica che il contatto abbia almeno un metodo di comunicazione valido
//     */
//    private fun hasValidContactInfo(contact: Contact): Boolean {
//        return contact.email?.isNotBlank() == true ||
//                contact.phone?.isNotBlank() == true ||
//                contact.mobilePhone?.isNotBlank() == true
//    }
}