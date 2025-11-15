package net.calvuz.qreport.domain.usecase.client.contact

import android.util.Patterns
import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.domain.model.client.ContactMethod
import net.calvuz.qreport.domain.repository.ContactRepository
import javax.inject.Inject

/**
 * Use Case per ricerca e filtro contatti
 *
 * Gestisce:
 * - Ricerca testuale su più campi
 * - Filtro per ruolo, dipartimento, metodo contatto preferito
 * - Ricerca per email/telefono specifico
 * - Analytics contatti
 */
class SearchContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {

    /**
     * Ricerca contatti per query testuale
     *
     * Cerca in: nome, cognome, email, telefono, ruolo, azienda
     *
     * @param query Testo da cercare
     * @return Result con lista contatti ordinata per relevanza
     */
    suspend operator fun invoke(query: String): Result<List<Contact>> {
        return try {
            // Validazione input
            if (query.isBlank()) {
                return Result.failure(IllegalArgumentException("Query di ricerca non può essere vuota"))
            }

            if (query.length < 2) {
                return Result.failure(IllegalArgumentException("Query di ricerca deve essere di almeno 2 caratteri"))
            }

            contactRepository.searchContacts(query.trim())
                .map { contacts ->
                    // Ordina per relevanza
                    contacts.sortedWith(
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
                }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cerca contatto per email esatta
     *
     * @param email Email da cercare
     * @return Result con contatto se trovato
     */
    suspend fun findByEmail(email: String): Result<Contact?> {
        return try {
            if (email.isBlank()) {
                return Result.failure(IllegalArgumentException("Email non può essere vuota"))
            }

            if (!isValidEmail(email)) {
                return Result.failure(IllegalArgumentException("Formato email non valido"))
            }

            contactRepository.getContactByEmail(email.trim().lowercase())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cerca contatto per numero di telefono
     *
     * @param phone Numero di telefono da cercare
     * @return Result con contatto se trovato
     */
    suspend fun findByPhone(phone: String): Result<Contact?> {
        return try {
            if (phone.isBlank()) {
                return Result.failure(IllegalArgumentException("Numero di telefono non può essere vuoto"))
            }

            val cleanPhone = phone.replace("\\s+".toRegex(), "").replace("-", "")
            contactRepository.getContactByPhone(cleanPhone)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Filtra contatti per ruolo
     *
     * @param role Ruolo da filtrare
     * @return Result con lista contatti del ruolo specificato
     */
    suspend fun filterByRole(role: String): Result<List<Contact>> {
        return try {
            if (role.isBlank()) {
                return Result.failure(IllegalArgumentException("Ruolo non può essere vuoto"))
            }

            contactRepository.getContactsByRole(role.trim())
                .map { contacts ->
                    contacts.sortedWith(
                        compareBy<Contact> { !it.isPrimary }
                            .thenBy { it.firstName.lowercase() }
                    )
                }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Filtra contatti per dipartimento
     *
     * @param department Dipartimento da filtrare
     * @return Result con lista contatti del dipartimento specificato
     */
    suspend fun filterByDepartment(department: String): Result<List<Contact>> {
        return try {
            if (department.isBlank()) {
                return Result.failure(IllegalArgumentException("Dipartimento non può essere vuoto"))
            }

            contactRepository.getContactsByDepartment(department.trim())
                .map { contacts ->
                    contacts.sortedWith(
                        compareBy<Contact> { !it.isPrimary }
                            .thenBy { it.firstName.lowercase() }
                    )
                }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Filtra contatti per metodo di contatto preferito
     *
     * @param clientId ID del cliente (opzionale, se vuoi filtrare per un cliente specifico)
     * @param contactMethod Metodo di contatto preferito
     * @return Result con lista contatti che preferiscono il metodo specificato
     */
    suspend fun filterByPreferredContactMethod(
        clientId: String? = null,
        contactMethod: ContactMethod
    ): Result<List<Contact>> {
        return try {
            if (clientId != null) {
                contactRepository.getContactsByPreferredMethod(clientId, contactMethod)
                    .map { contacts ->
                        contacts.sortedWith(
                            compareBy<Contact> { !it.isPrimary }
                                .thenBy { it.firstName.lowercase() }
                        )
                    }
            } else {
                // Implementazione per tutti i contatti con quel metodo preferito
                // Richiederebbe una query specifica nel repository
                Result.failure(UnsupportedOperationException("Filtro per metodo contatto senza clientId non implementato"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ricerca avanzata con filtri multipli
     *
     * @param searchCriteria Criteri di ricerca
     * @return Result con lista contatti filtrata
     */
    suspend fun advancedSearch(searchCriteria: ContactSearchCriteria): Result<List<Contact>> {
        return try {
            var result = contactRepository.getActiveContacts()

            // Applica filtri progressivamente
            result = result.mapCatching { contacts ->
                var filtered = contacts

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
                filtered.sortedWith(
                    compareBy<Contact> { !it.isPrimary }
                        .thenBy { it.firstName.lowercase() }
                        .thenBy { it.lastName?.lowercase() ?: "" }
                )
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene contatti con informazioni di comunicazione complete
     *
     * @return Result con lista contatti che hanno almeno un metodo di contatto
     */
    suspend fun getContactsWithValidContactInfo(): Result<List<Contact>> {
        return try {
            contactRepository.getContactsWithAnyContactInfo()
                .map { contacts ->
                    contacts.sortedWith(
                        compareBy<Contact> { !it.isPrimary }
                            .thenBy { it.firstName.lowercase() }
                    )
                }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene tutti i ruoli disponibili per filtro
     *
     * @return Result con lista ruoli ordinata alfabeticamente
     */
    suspend fun getAvailableRoles(): Result<List<String>> {
        return try {
            contactRepository.getAllRoles()
                .map { roles -> roles.sorted() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene tutti i dipartimenti disponibili per filtro
     *
     * @return Result con lista dipartimenti ordinata alfabeticamente
     */
    suspend fun getAvailableDepartments(): Result<List<String>> {
        return try {
            contactRepository.getAllDepartments()
                .map { departments -> departments.sorted() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene statistiche sui metodi di contatto preferiti
     *
     * @return Result con mappa metodo -> numero contatti
     */
    suspend fun getContactMethodStatistics(): Result<Map<ContactMethod, Int>> {
        return try {
            contactRepository.getContactMethodStats()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validazione formato email
     */
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
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