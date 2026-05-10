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
     * Delete a Cliente
     *
     * @param clientId Client ID
     * @param cascade If true, cascade (default)
     * @return Unit Result
     */
    suspend operator fun invoke(
        clientId: String,
        cascade: Boolean = true
    ): Result<Unit> {
        return try {

            // 1. Input validation
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            // 2. Client check
            checkClientExists(clientId).onFailure { return Result.failure(it) }

            // 3. Dependencies check
            if (!cascade) {
                // It would leave orphans behind
                checkClientDependencies(clientId).onFailure { return Result.failure(it) }
            }

            // 4. Delete dependencies
            if (cascade) {
                deleteClientDependencies(clientId).onFailure { return Result.failure(it) }
            }

            // 5. Delete Client
            clientRepository.deleteClient(clientId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}