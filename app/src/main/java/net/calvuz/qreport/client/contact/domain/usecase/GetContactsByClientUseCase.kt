package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import net.calvuz.qreport.client.client.domain.usecase.MyCheckClientExistsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Use Case per recuperare contatti di un cliente
 *
 * Gestisce:
 * - Validazione esistenza cliente
 * - Recupero contatti ordinati (primary prima)
 * - Flow reattivo per UI
 * - Filtri per ruolo e dipartimento
 *
 * ✅ COMPLETE: All 8 methods updated to use QrResult<T, QrError> pattern
 */
class GetContactsByClientUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val checkClientExists: MyCheckClientExistsUseCase
) {

    /**
     * ✅ METHOD 1/8: Recupera tutti i contatti di un cliente
     *
     * @param clientId ID del cliente
     * @return QrResult.Success con lista contatti ordinata (primary prima, poi alfabetico)
     */
    suspend operator fun invoke(clientId: String): QrResult<List<Contact>, QrError> {
        return try {
            // 1. Validazione input
            if (clientId.isBlank()) {
                Timber.w("GetContactsByClientUseCase.invoke: clientId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
            }

            // 2. Verifica esistenza cliente
            when (val clientCheck = checkClientExists(clientId)) {
                is QrResult.Error -> {
                    Timber.w("GetContactsByClientUseCase.invoke: Client does not exist: $clientId")
                    return QrResult.Error(clientCheck.error)
                }
                is QrResult.Success -> {
                    // Client exists, continue
                }
            }

            // 3. Recupero contatti ordinati
            when (val result = contactRepository.getContactsByClient(clientId)) {
                is QrResult.Success -> {
                    val contacts = result.data
                    val sortedContacts = contacts.sortedWith(
                        compareBy<Contact> { !it.isPrimary } // Primary prima (false viene prima di true)
                            .thenBy { it.firstName.lowercase() } // Poi alfabetico per nome
                            .thenBy { it.lastName?.lowercase() ?: "" } // Poi per cognome
                    )

                    Timber.d("GetContactsByClientUseCase.invoke: Retrieved ${sortedContacts.size} contacts for client: $clientId")
                    QrResult.Success(sortedContacts)
                }

                is QrResult.Error -> {
                    Timber.e("GetContactsByClientUseCase.invoke: Repository error for clientId $clientId: ${result.error}")
                    QrResult.Error(result.error)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "GetContactsByClientUseCase.invoke: Exception getting contacts for client: $clientId")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

//    /**
//     * ✅ METHOD 2/8: Osserva tutti i contatti di un cliente (Flow reattivo)
//     *
//     * @param clientId ID del cliente
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
//     * @param clientId ID del cliente
//     * @return QrResult.Success con lista contatti attivi
//     */
//    suspend fun getActiveContactsByClient(clientId: String): QrResult<List<Contact>, QrError> {
//        return try {
//            if (clientId.isBlank()) {
//                Timber.w("GetContactsByClientUseCase.getActiveContactsByClient: clientId is blank")
//                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
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
//            QrResult.Error(QrError.SystemError.Unknown()    )
//        }
//    }
//
//    /**
//     * ✅ METHOD 4/8: Recupera il contatto primary di un cliente
//     *
//     * @param clientId ID del cliente
//     * @return QrResult.Success con contatto primary se esiste, null se non esiste
//     */
//    suspend fun getPrimaryContact(clientId: String): QrResult<Contact?, QrError> {
//        return try {
//            if (clientId.isBlank()) {
//                Timber.w("GetContactsByClientUseCase.getPrimaryContact: clientId is blank")
//                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
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
//            QrResult.Error(QrError.SystemError.Unknown())
//        }
//    }
//
//    /**
//     * ✅ METHOD 5/8: Recupera contatti di un cliente filtrati per ruolo
//     *
//     * @param clientId ID del cliente
//     * @param role Ruolo da filtrare
//     * @return QrResult.Success con lista contatti del ruolo specificato
//     */
//    suspend fun getContactsByRole(clientId: String, role: String): QrResult<List<Contact>, QrError> {
//        return try {
//            if (clientId.isBlank()) {
//                Timber.w("GetContactsByClientUseCase.getContactsByRole: clientId is blank")
//                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
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
//            QrResult.Error(QrError.SystemError.Unknown())
//        }
//    }
//
//    /**
//     * ✅ METHOD 6/8: Recupera contatti di un cliente filtrati per dipartimento
//     *
//     * @param clientId ID del cliente
//     * @param department Dipartimento da filtrare
//     * @return QrResult.Success con lista contatti del dipartimento specificato
//     */
//    suspend fun getContactsByDepartment(clientId: String, department: String): QrResult<List<Contact>, QrError> {
//        return try {
//            if (clientId.isBlank()) {
//                Timber.w("GetContactsByClientUseCase.getContactsByDepartment: clientId is blank")
//                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
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
//            QrResult.Error(QrError.SystemError.Unknown())
//        }
//    }
//
//    /**
//     * ✅ METHOD 7/8: Recupera contatti che hanno informazioni complete per comunicare
//     *
//     * @param clientId ID del cliente
//     * @return QrResult.Success con lista contatti con almeno un metodo di contatto
//     */
//    suspend fun getContactsWithValidContactInfo(clientId: String): QrResult<List<Contact>, QrError> {
//        return try {
//            if (clientId.isBlank()) {
//                Timber.w("GetContactsByClientUseCase.getContactsWithValidContactInfo: clientId is blank")
//                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
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
//            QrResult.Error(QrError.SystemError.Unknown())
//        }
//    }
//
//    /**
//     * ✅ METHOD 8/8: Conta il numero di contatti per un cliente
//     *
//     * @param clientId ID del cliente
//     * @return QrResult.Success con numero di contatti
//     */
//    suspend fun getContactsCount(clientId: String): QrResult<Int, QrError> {
//        return try {
//            if (clientId.isBlank()) {
//                Timber.w("GetContactsByClientUseCase.getContactsCount: clientId is blank")
//                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
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
//            QrResult.Error(QrError.SystemError.Unknown())
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