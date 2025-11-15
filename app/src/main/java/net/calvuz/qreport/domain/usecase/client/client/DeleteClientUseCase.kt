package net.calvuz.qreport.domain.usecase.client.client

import net.calvuz.qreport.domain.repository.ClientRepository
import net.calvuz.qreport.domain.repository.ContactRepository
import net.calvuz.qreport.domain.repository.FacilityRepository
import javax.inject.Inject

/**
 * Use Case per eliminazione di un cliente
 *
 * Gestisce:
 * - Validazione esistenza cliente
 * - Controllo dipendenze (facilities, contatti, isole)
 * - Eliminazione sicura (soft delete)
 * - Pulizia opzionale dipendenze
 *
 * Business Rules:
 * - Cliente può essere eliminato solo se non ha dipendenze attive
 * - Oppure con flag di forzatura per eliminare anche dipendenze
 */
class DeleteClientUseCase @Inject constructor(
    private val clientRepository: ClientRepository,
    private val facilityRepository: FacilityRepository,
    private val contactRepository: ContactRepository
) {

    /**
     * Elimina un cliente
     *
     * @param clientId ID del cliente da eliminare
     * @param force Se true, elimina anche facilities e contatti associati
     * @return Result con Unit se successo, errore con dettagli se fallimento
     */
    suspend operator fun invoke(
        clientId: String,
        force: Boolean = false
    ): Result<Unit> {
        return try {
            // 1. Validazione input
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            // 2. Verificare che il cliente esista
            checkClientExists(clientId).onFailure { return Result.failure(it) }

            // 3. Controllo dipendenze se non è forzato
            if (!force) {
                checkDependencies(clientId).onFailure { return Result.failure(it) }
            }

            // 4. Eliminazione dipendenze se forzato
            if (force) {
                deleteDependencies(clientId).onFailure { return Result.failure(it) }
            }

            // 5. Eliminazione cliente (soft delete)
            clientRepository.deleteClient(clientId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica che il cliente esista
     */
    private suspend fun checkClientExists(clientId: String): Result<Unit> {
        return clientRepository.getClientById(clientId)
            .mapCatching { client ->
                if (client == null) {
                    throw NoSuchElementException("Cliente con ID '$clientId' non trovato")
                }
                if (!client.isActive) {
                    throw IllegalStateException("Cliente con ID '$clientId' già eliminato")
                }
            }
    }

    /**
     * Controlla se esistono dipendenze che impediscono l'eliminazione
     */
    private suspend fun checkDependencies(clientId: String): Result<Unit> {
        return try {
            val dependencies = mutableListOf<String>()

            // Controllo facilities
            facilityRepository.getFacilitiesCountByClient(clientId)
                .onSuccess { count ->
                    if (count > 0) {
                        dependencies.add("$count stabilimento/i")
                    }
                }
                .onFailure { return Result.failure(it) }

            // Controllo contatti
            contactRepository.getContactsCountByClient(clientId)
                .onSuccess { count ->
                    if (count > 0) {
                        dependencies.add("$count contatto/i")
                    }
                }
                .onFailure { return Result.failure(it) }

            // Controllo isole (tramite client)
            clientRepository.getIslandsCount(clientId)
                .onSuccess { count ->
                    if (count > 0) {
                        dependencies.add("$count isola/e robotizzata/e")
                    }
                }
                .onFailure { return Result.failure(it) }

            if (dependencies.isNotEmpty()) {
                val dependencyText = dependencies.joinToString(", ")
                throw IllegalStateException(
                    "Impossibile eliminare cliente: sono presenti $dependencyText. " +
                            "Utilizzare force=true per eliminare anche le dipendenze."
                )
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina tutte le dipendenze del cliente
     */
    private suspend fun deleteDependencies(clientId: String): Result<Unit> {
        return try {
            // Elimina facilities (che a cascata eliminerà le isole)
            facilityRepository.getFacilitiesByClient(clientId)
                .onSuccess { facilities ->
                    facilities.forEach { facility ->
                        facilityRepository.deleteFacility(facility.id)
                            .onFailure { return Result.failure(it) }
                    }
                }
                .onFailure { return Result.failure(it) }

            // Elimina contatti
            contactRepository.getContactsByClient(clientId)
                .onSuccess { contacts ->
                    contacts.forEach { contact ->
                        contactRepository.deleteContact(contact.id)
                            .onFailure { return Result.failure(it) }
                    }
                }
                .onFailure { return Result.failure(it) }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica se un cliente può essere eliminato senza forzatura
     *
     * @param clientId ID del cliente da verificare
     * @return Result con Boolean - true se può essere eliminato, false altrimenti
     */
    suspend fun canDeleteClient(clientId: String): Result<Boolean> {
        return try {
            checkClientExists(clientId).onFailure {
                return Result.success(false) // Cliente non esiste, quindi non può essere eliminato
            }

            checkDependencies(clientId)
                .map { true } // Nessuna dipendenza, può essere eliminato
                .recover { false } // Ha dipendenze, non può essere eliminato senza forzatura

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene informazioni sulle dipendenze di un cliente
     *
     * @param clientId ID del cliente
     * @return Result con mappa delle dipendenze (tipo -> conteggio)
     */
    suspend fun getClientDependencies(clientId: String): Result<Map<String, Int>> {
        return try {
            checkClientExists(clientId).onFailure { return Result.failure(it) }

            val dependencies = mutableMapOf<String, Int>()

            facilityRepository.getFacilitiesCountByClient(clientId)
                .onSuccess { count -> dependencies["facilities"] = count }
                .onFailure { return Result.failure(it) }

            contactRepository.getContactsCountByClient(clientId)
                .onSuccess { count -> dependencies["contacts"] = count }
                .onFailure { return Result.failure(it) }

            clientRepository.getIslandsCount(clientId)
                .onSuccess { count -> dependencies["islands"] = count }
                .onFailure { return Result.failure(it) }

            Result.success(dependencies.toMap())

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}