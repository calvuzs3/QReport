package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.client.domain.usecase.CheckClientExistsUseCase
import javax.inject.Inject

/**
 * Use Case per recuperare contatti di un cliente
 *
 * Gestisce:
 * - Validazione esistenza cliente
 * - Recupero contatti ordinati (primary prima)
 * - Flow reattivo per UI
 * - Filtri per ruolo e dipartimento
 */
class GetContactsByClientUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val checkClientExists: CheckClientExistsUseCase
) {

    /**
     * Recupera tutti i contatti di un cliente
     *
     * @param clientId ID del cliente
     * @return Result con lista contatti ordinata (primary prima, poi alfabetico)
     */
    suspend operator fun invoke(clientId: String): Result<List<Contact>> {
        return try {
            // 1. Validazione input
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            // 2. Verifica esistenza cliente
            checkClientExists(clientId).onFailure { return Result.failure(it) }

            // 3. Recupero contatti ordinati
            contactRepository.getContactsByClient(clientId)
                .map { contacts ->
                    contacts.sortedWith(
                        compareBy<Contact> { !it.isPrimary } // Primary prima (false viene prima di true)
                            .thenBy { it.firstName.lowercase() } // Poi alfabetico per nome
                            .thenBy { it.lastName?.lowercase() ?: "" } // Poi per cognome
                    )
                }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Osserva tutti i contatti di un cliente (Flow reattivo)
     *
     * @param clientId ID del cliente
     * @return Flow con lista contatti che si aggiorna automaticamente
     */
    fun observeContactsByClient(clientId: String): Flow<List<Contact>> {
        return contactRepository.getContactsByClientFlow(clientId)
            .map { contacts ->
                contacts.sortedWith(
                    compareBy<Contact> { !it.isPrimary }
                        .thenBy { it.firstName.lowercase() }
                        .thenBy { it.lastName?.lowercase() ?: "" }
                )
            }
    }

    /**
     * Recupera solo i contatti attivi di un cliente
     *
     * @param clientId ID del cliente
     * @return Result con lista contatti attivi
     */
    suspend fun getActiveContactsByClient(clientId: String): Result<List<Contact>> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            checkClientExists(clientId).onFailure { return Result.failure(it) }

            contactRepository.getActiveContactsByClient(clientId)
                .map { contacts ->
                    contacts.sortedWith(
                        compareBy<Contact> { !it.isPrimary }
                            .thenBy { it.firstName.lowercase() }
                            .thenBy { it.lastName?.lowercase() ?: "" }
                    )
                }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera il contatto primary di un cliente
     *
     * @param clientId ID del cliente
     * @return Result con contatto primary se esiste
     */
    suspend fun getPrimaryContact(clientId: String): Result<Contact?> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            checkClientExists(clientId).onFailure { return Result.failure(it) }

            contactRepository.getPrimaryContact(clientId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera contatti di un cliente filtrati per ruolo
     *
     * @param clientId ID del cliente
     * @param role Ruolo da filtrare
     * @return Result con lista contatti del ruolo specificato
     */
    suspend fun getContactsByRole(clientId: String, role: String): Result<List<Contact>> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }
            if (role.isBlank()) {
                return Result.failure(IllegalArgumentException("Ruolo non può essere vuoto"))
            }

            checkClientExists(clientId).onFailure { return Result.failure(it) }

            invoke(clientId).map { contacts ->
                contacts.filter { contact ->
                    contact.role?.equals(role, ignoreCase = true) == true
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera contatti di un cliente filtrati per dipartimento
     *
     * @param clientId ID del cliente
     * @param department Dipartimento da filtrare
     * @return Result con lista contatti del dipartimento specificato
     */
    suspend fun getContactsByDepartment(clientId: String, department: String): Result<List<Contact>> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }
            if (department.isBlank()) {
                return Result.failure(IllegalArgumentException("Dipartimento non può essere vuoto"))
            }

            checkClientExists(clientId).onFailure { return Result.failure(it) }

            invoke(clientId).map { contacts ->
                contacts.filter { contact ->
                    contact.department?.equals(department, ignoreCase = true) == true
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera contatti che hanno informazioni complete per comunicare
     *
     * @param clientId ID del cliente
     * @return Result con lista contatti con almeno un metodo di contatto
     */
    suspend fun getContactsWithValidContactInfo(clientId: String): Result<List<Contact>> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            checkClientExists(clientId).onFailure { return Result.failure(it) }

            invoke(clientId).map { contacts ->
                contacts.filter { contact ->
                    hasValidContactInfo(contact)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Conta il numero di contatti per un cliente
     *
     * @param clientId ID del cliente
     * @return Result con numero di contatti
     */
    suspend fun getContactsCount(clientId: String): Result<Int> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            contactRepository.getContactsCountByClient(clientId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica che il contatto abbia almeno un metodo di comunicazione valido
     */
    private fun hasValidContactInfo(contact: Contact): Boolean {
        return contact.email?.isNotBlank() == true ||
                contact.phone?.isNotBlank() == true ||
                contact.mobilePhone?.isNotBlank() == true
    }
}