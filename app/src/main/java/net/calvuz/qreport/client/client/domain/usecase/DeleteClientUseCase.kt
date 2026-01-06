package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
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
    private val contactRepository: ContactRepository,
    private val checkClientExists: CheckClientExistsUseCase,
    private val checkClientDependencies: CheckClientDependenciesUseCase,
    private val deleteClientDependencies: DeleteClientDependenciesUseCase
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
                checkClientDependencies(clientId).onFailure { return Result.failure(it) }
            }

            // 4. Eliminazione dipendenze se forzato
            if (force) {
                deleteClientDependencies(clientId).onFailure { return Result.failure(it) }
            }

            // 5. Eliminazione cliente (soft delete)
            clientRepository.deleteClient(clientId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}