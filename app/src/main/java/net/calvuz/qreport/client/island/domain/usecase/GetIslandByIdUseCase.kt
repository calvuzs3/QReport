package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import javax.inject.Inject

/**
 * Use Case per recuperare un'isola robotizzata per ID
 *
 * Gestisce:
 * - Validazione ID input
 * - Recupero dal repository
 * - Gestione isola non trovata
 */
class GetIslandByIdUseCase @Inject constructor(
    private val islandRepository: IslandRepository
) {

    /**
     * Recupera un'isola robotizzata per ID
     *
     * @param islandId ID dell'isola da recuperare
     * @return Result con Island se trovata, errore se non trovata o errore di sistema
     */
    suspend operator fun invoke(islandId: String): Result<Island> {
        return try {
            // 1. Validazione ID
            if (islandId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID isola non può essere vuoto"))
            }

            // 2. Recupero dal repository
            islandRepository.getIslandById(islandId)
                .mapCatching { island ->
                    // 3. Gestione isola non trovata
                    island ?: throw NoSuchElementException("Isola con ID '$islandId' non trovata")
                }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}