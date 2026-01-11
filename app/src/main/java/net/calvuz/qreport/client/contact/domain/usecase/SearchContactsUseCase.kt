package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.model.ContactMethod
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import net.calvuz.qreport.client.contact.domain.validator.ContactDataValidator
import timber.log.Timber
import javax.inject.Inject

/**
 * Use Case per ricerca e filtro contatti
 *
 * Gestisce:
 * - Ricerca testuale su più campi
 * - Filtro per ruolo, dipartimento, metodo contatto preferito
 * - Ricerca per email/telefono specifico
 * - Analytics contatti
 *
 * Updated to use QrResult<T, QrError> pattern for all methods
 */
class SearchContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val contactDataValidator: ContactDataValidator,
) {

    /**
     * Ricerca contatti per query testuale
     *
     * Cerca in: nome, cognome, email, telefono, ruolo, azienda
     *
     * @param query Testo da cercare
     * @return QrResult.Success con lista contatti ordinata per relevanza, QrResult.Error per errori
     */
    suspend operator fun invoke(query: String): QrResult<List<Contact>, QrError> {
        return try {
            // Validazione input
            if (query.isBlank()) {
                Timber.w("SearchContactsUseCase: query is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(query.toString()))
            }

            if (query.length < 2) {
                Timber.w("SearchContactsUseCase: query too short: ${query.length}")
                return QrResult.Error(QrError.ValidationError.EmptyField(query.toString()))
            }

            Timber.d("SearchContactsUseCase: Searching contacts with query: $query")

            when (val result = contactRepository.searchContacts(query.trim())) {
                is QrResult.Success -> {
                    val contacts = result.data

                    // Ordina per relevanza
                    val sortedContacts = contacts.sortedWith(
                        compareBy<Contact> { !it.isPrimary } // Primary prima
                            .thenBy { contact ->
                                // Poi per relevanza nome/email
                                when {
                                    contact.firstName.equals(query.trim(), ignoreCase = true) -> 0
                                    contact.lastName?.equals(query.trim(), ignoreCase = true) == true -> 1
                                    contact.email?.equals(query.trim(), ignoreCase = true) == true -> 2
                                    contact.firstName.startsWith(query.trim(), ignoreCase = true) -> 3
                                    else -> 4
                                }
                            }
                            .thenBy { it.firstName.lowercase() }
                    )

                    Timber.d("SearchContactsUseCase: Found ${sortedContacts.size} contacts for query: $query")
                    QrResult.Success(sortedContacts)
                }

                is QrResult.Error -> {
                    Timber.e("SearchContactsUseCase: Repository error for query '$query': ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SearchContactsUseCase: Exception searching with query: $query")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Cerca contatto per email esatta
     *
     * @param email Email da cercare
     * @return QrResult.Success con contatto se trovato, QrResult.Error per errori
     */
    suspend fun findByEmail(email: String): QrResult<Contact?, QrError> {
        return try {
            if (email.isBlank()) {
                Timber.w("SearchContactsUseCase.findByEmail: email is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(email.toString()))
            }

            if (!contactDataValidator.isValidEmail(email)) {
                Timber.w("SearchContactsUseCase.findByEmail: invalid email format: $email")
                return QrResult.Error(QrError.ValidationError.EmptyField(email.toString()))
            }

            Timber.d("SearchContactsUseCase.findByEmail: Searching contact by email: $email")

            when (val result = contactRepository.getContactByEmail(email.trim().lowercase())) {
                is QrResult.Success -> {
                    val contact = result.data
                    if (contact != null) {
                        Timber.d("SearchContactsUseCase.findByEmail: Contact found for email: $email")
                    } else {
                        Timber.d("SearchContactsUseCase.findByEmail: No contact found for email: $email")
                    }
                    QrResult.Success(contact)
                }

                is QrResult.Error -> {
                    Timber.e("SearchContactsUseCase.findByEmail: Repository error for email '$email': ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SearchContactsUseCase.findByEmail: Exception searching by email: $email")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Cerca contatto per numero di telefono
     *
     * @param phone Numero di telefono da cercare
     * @return QrResult.Success con contatto se trovato, QrResult.Error per errori
     */
    suspend fun findByPhone(phone: String): QrResult<Contact?, QrError> {
        return try {
            if (phone.isBlank()) {
                Timber.w("SearchContactsUseCase.findByPhone: phone is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(phone.toString()))
            }

            val cleanPhone = phone.replace("\\s+".toRegex(), "").replace("-", "")
            Timber.d("SearchContactsUseCase.findByPhone: Searching contact by phone: $cleanPhone")

            when (val result = contactRepository.getContactByPhone(cleanPhone)) {
                is QrResult.Success -> {
                    val contact = result.data
                    if (contact != null) {
                        Timber.d("SearchContactsUseCase.findByPhone: Contact found for phone: $cleanPhone")
                    } else {
                        Timber.d("SearchContactsUseCase.findByPhone: No contact found for phone: $cleanPhone")
                    }
                    QrResult.Success(contact)
                }

                is QrResult.Error -> {
                    Timber.e("SearchContactsUseCase.findByPhone: Repository error for phone '$cleanPhone': ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SearchContactsUseCase.findByPhone: Exception searching by phone: $phone")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Filtra contatti per ruolo
     *
     * @param role Ruolo da filtrare
     * @return QrResult.Success con lista contatti del ruolo specificato, QrResult.Error per errori
     */
    suspend fun filterByRole(role: String): QrResult<List<Contact>, QrError> {
        return try {
            if (role.isBlank()) {
                Timber.w("SearchContactsUseCase.filterByRole: role is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(role.toString()))
            }

            Timber.d("SearchContactsUseCase.filterByRole: Filtering contacts by role: $role")

            when (val result = contactRepository.getContactsByRole(role.trim())) {
                is QrResult.Success -> {
                    val contacts = result.data
                    val sortedContacts = contacts.sortedWith(
                        compareBy<Contact> { !it.isPrimary }
                            .thenBy { it.firstName.lowercase() }
                    )

                    Timber.d("SearchContactsUseCase.filterByRole: Found ${sortedContacts.size} contacts with role: $role")
                    QrResult.Success(sortedContacts)
                }

                is QrResult.Error -> {
                    Timber.e("SearchContactsUseCase.filterByRole: Repository error for role '$role': ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SearchContactsUseCase.filterByRole: Exception filtering by role: $role")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Filtra contatti per dipartimento
     *
     * @param department Dipartimento da filtrare
     * @return QrResult.Success con lista contatti del dipartimento specificato, QrResult.Error per errori
     */
    suspend fun filterByDepartment(department: String): QrResult<List<Contact>, QrError> {
        return try {
            if (department.isBlank()) {
                Timber.w("SearchContactsUseCase.filterByDepartment: department is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(department.toString()))
            }

            Timber.d("SearchContactsUseCase.filterByDepartment: Filtering contacts by department: $department")

            when (val result = contactRepository.getContactsByDepartment(department.trim())) {
                is QrResult.Success -> {
                    val contacts = result.data
                    val sortedContacts = contacts.sortedWith(
                        compareBy<Contact> { !it.isPrimary }
                            .thenBy { it.firstName.lowercase() }
                    )

                    Timber.d("SearchContactsUseCase.filterByDepartment: Found ${sortedContacts.size} contacts in department: $department")
                    QrResult.Success(sortedContacts)
                }

                is QrResult.Error -> {
                    Timber.e("SearchContactsUseCase.filterByDepartment: Repository error for department '$department': ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SearchContactsUseCase.filterByDepartment: Exception filtering by department: $department")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Filtra contatti per metodo di contatto preferito
     *
     * @param clientId ID del cliente (opzionale, se vuoi filtrare per un cliente specifico)
     * @param contactMethod Metodo di contatto preferito
     * @return QrResult.Success con lista contatti che preferiscono il metodo specificato, QrResult.Error per errori
     */
    suspend fun filterByPreferredContactMethod(
        clientId: String? = null,
        contactMethod: ContactMethod
    ): QrResult<List<Contact>, QrError> {
        return try {
            if (clientId != null) {
                if (clientId.isBlank()) {
                    Timber.w("SearchContactsUseCase.filterByPreferredContactMethod: clientId is blank")
                    return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
                }

                Timber.d("SearchContactsUseCase.filterByPreferredContactMethod: Filtering contacts for client $clientId by method: $contactMethod")

                when (val result = contactRepository.getContactsByPreferredMethod(clientId, contactMethod)) {
                    is QrResult.Success -> {
                        val contacts = result.data
                        val sortedContacts = contacts.sortedWith(
                            compareBy<Contact> { !it.isPrimary }
                                .thenBy { it.firstName.lowercase() }
                        )

                        Timber.d("SearchContactsUseCase.filterByPreferredContactMethod: Found ${sortedContacts.size} contacts with method $contactMethod for client $clientId")
                        QrResult.Success(sortedContacts)
                    }

                    is QrResult.Error -> {
                        Timber.e("SearchContactsUseCase.filterByPreferredContactMethod: Repository error for method $contactMethod: ${result.error}")
                        QrResult.Error(result.error)
                    }
                }
            } else {
                // Implementazione per tutti i contatti con quel metodo preferito
                // Richiederebbe una query specifica nel repository
                Timber.w("SearchContactsUseCase.filterByPreferredContactMethod: Filtering by method without clientId not implemented")
                QrResult.Error(QrError.ValidationError.InvalidOperation("Filtering by method without clientId not implemented"))
            }
        } catch (e: Exception) {
            Timber.e(e, "SearchContactsUseCase.filterByPreferredContactMethod: Exception filtering by method: $contactMethod")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Ricerca avanzata con filtri multipli
     *
     * @param searchCriteria Criteri di ricerca
     * @return QrResult.Success con lista contatti filtrata, QrResult.Error per errori
     */
    suspend fun advancedSearch(searchCriteria: ContactSearchCriteria): QrResult<List<Contact>, QrError> {
        return try {
            Timber.d("SearchContactsUseCase.advancedSearch: Starting advanced search with criteria: $searchCriteria")

            // Get all active contacts first
            var result = contactRepository.getActiveContacts()

            // Apply filters progressively
            result = when (result) {
                is QrResult.Success -> {
                    var filtered = result.data

                    // Filtro per query testuale
                    searchCriteria.textQuery?.takeIf { it.isNotBlank() }?.let { query ->
                        filtered = filtered.filter { contact ->
                            contact.firstName.contains(query, ignoreCase = true) ||
                                    contact.lastName?.contains(query, ignoreCase = true) == true ||
                                    contact.email?.contains(query, ignoreCase = true) == true ||
                                    contact.phone?.contains(query, ignoreCase = true) == true ||
                                    contact.role?.contains(query, ignoreCase = true) == true
                        }
                    }

                    // Filtro per cliente specifico
                    searchCriteria.clientId?.takeIf { it.isNotBlank() }?.let { clientId ->
                        filtered = filtered.filter { contact ->
                            contact.clientId == clientId
                        }
                    }

                    // Filtro per ruolo
                    searchCriteria.role?.takeIf { it.isNotBlank() }?.let { role ->
                        filtered = filtered.filter { contact ->
                            contact.role?.equals(role, ignoreCase = true) == true
                        }
                    }

                    // Filtro per dipartimento
                    searchCriteria.department?.takeIf { it.isNotBlank() }?.let { department ->
                        filtered = filtered.filter { contact ->
                            contact.department?.equals(department, ignoreCase = true) == true
                        }
                    }

                    // Filtro per primary contact
                    if (searchCriteria.isPrimary == true) {
                        filtered = filtered.filter { it.isPrimary }
                    }

                    // Filtro per presenza email
                    if (searchCriteria.hasEmail == true) {
                        filtered = filtered.filter { contact ->
                            !contact.email.isNullOrBlank()
                        }
                    }

                    // Filtro per presenza telefono
                    if (searchCriteria.hasPhone == true) {
                        filtered = filtered.filter { contact ->
                            !contact.phone.isNullOrBlank() || !contact.mobilePhone.isNullOrBlank()
                        }
                    }

                    // Filtro per metodo contatto preferito
                    searchCriteria.preferredContactMethod?.let { method ->
                        filtered = filtered.filter { contact ->
                            contact.preferredContactMethod == method
                        }
                    }

                    // Ordinamento finale
                    val sortedContacts = filtered.sortedWith(
                        compareBy<Contact> { !it.isPrimary }
                            .thenBy { it.firstName.lowercase() }
                            .thenBy { it.lastName?.lowercase() ?: "" }
                    )

                    Timber.d("SearchContactsUseCase.advancedSearch: Advanced search completed, found ${sortedContacts.size} contacts")
                    QrResult.Success(sortedContacts)
                }

                is QrResult.Error -> {
                    Timber.e("SearchContactsUseCase.advancedSearch: Repository error getting active contacts: ${result.error}")
                    QrResult.Error(result.error)
                }
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "SearchContactsUseCase.advancedSearch: Exception in advanced search")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Ottiene contatti con informazioni di comunicazione complete
     *
     * @return QrResult.Success con lista contatti che hanno almeno un metodo di contatto, QrResult.Error per errori
     */
    suspend fun getContactsWithValidContactInfo(): QrResult<List<Contact>, QrError> {
        return try {
            Timber.d("SearchContactsUseCase.getContactsWithValidContactInfo: Getting contacts with valid contact info")

            when (val result = contactRepository.getContactsWithAnyContactInfo()) {
                is QrResult.Success -> {
                    val contacts = result.data
                    val sortedContacts = contacts.sortedWith(
                        compareBy<Contact> { !it.isPrimary }
                            .thenBy { it.firstName.lowercase() }
                    )

                    Timber.d("SearchContactsUseCase.getContactsWithValidContactInfo: Found ${sortedContacts.size} contacts with valid contact info")
                    QrResult.Success(sortedContacts)
                }

                is QrResult.Error -> {
                    Timber.e("SearchContactsUseCase.getContactsWithValidContactInfo: Repository error: ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SearchContactsUseCase.getContactsWithValidContactInfo: Exception getting contacts with valid info")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Ottiene tutti i ruoli disponibili per filtro
     *
     * @return QrResult.Success con lista ruoli ordinata alfabeticamente, QrResult.Error per errori
     */
    suspend fun getAvailableRoles(): QrResult<List<String>, QrError> {
        return try {
            Timber.d("SearchContactsUseCase.getAvailableRoles: Getting available roles")

            when (val result = contactRepository.getAllRoles()) {
                is QrResult.Success -> {
                    val roles = result.data.sorted()
                    Timber.d("SearchContactsUseCase.getAvailableRoles: Found ${roles.size} roles")
                    QrResult.Success(roles)
                }

                is QrResult.Error -> {
                    Timber.e("SearchContactsUseCase.getAvailableRoles: Repository error: ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SearchContactsUseCase.getAvailableRoles: Exception getting roles")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Ottiene tutti i dipartimenti disponibili per filtro
     *
     * @return QrResult.Success con lista dipartimenti ordinata alfabeticamente, QrResult.Error per errori
     */
    suspend fun getAvailableDepartments(): QrResult<List<String>, QrError> {
        return try {
            Timber.d("SearchContactsUseCase.getAvailableDepartments: Getting available departments")

            when (val result = contactRepository.getAllDepartments()) {
                is QrResult.Success -> {
                    val departments = result.data.sorted()
                    Timber.d("SearchContactsUseCase.getAvailableDepartments: Found ${departments.size} departments")
                    QrResult.Success(departments)
                }

                is QrResult.Error -> {
                    Timber.e("SearchContactsUseCase.getAvailableDepartments: Repository error: ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SearchContactsUseCase.getAvailableDepartments: Exception getting departments")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Ottiene statistiche sui metodi di contatto preferiti
     *
     * @return QrResult.Success con mappa metodo → numero contatti, QrResult.Error per errori
     */
    suspend fun getContactMethodStatistics(): QrResult<Map<ContactMethod, Int>, QrError> {
        return try {
            Timber.d("SearchContactsUseCase.getContactMethodStatistics: Getting contact method statistics")

            when (val result = contactRepository.getContactMethodStats()) {
                is QrResult.Success -> {
                    val stats = result.data
                    Timber.d("SearchContactsUseCase.getContactMethodStatistics: Found statistics for ${stats.size} methods")
                    QrResult.Success(stats)
                }

                is QrResult.Error -> {
                    Timber.e("SearchContactsUseCase.getContactMethodStatistics: Repository error: ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SearchContactsUseCase.getContactMethodStatistics: Exception getting statistics")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }
}

/**
 * Data class per criteri di ricerca avanzata contatti
 */
data class ContactSearchCriteria(
    val textQuery: String? = null,
    val clientId: String? = null,
    val role: String? = null,
    val department: String? = null,
    val isPrimary: Boolean? = null,
    val hasEmail: Boolean? = null,
    val hasPhone: Boolean? = null,
    val preferredContactMethod: ContactMethod? = null
)