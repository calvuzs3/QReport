package net.calvuz.qreport.domain.usecase.client.facilityisland

import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.repository.FacilityIslandRepository
import javax.inject.Inject

/**
 * Use Case per recuperare un'isola robotizzata per ID
 *
 * Gestisce:
 * - Validazione ID input
 * - Recupero dal repository
 * - Gestione isola non trovata
 */
class GetFacilityIslandByIdUseCase @Inject constructor(
    private val facilityIslandRepository: FacilityIslandRepository
) {

    /**
     * Recupera un'isola robotizzata per ID
     *
     * @param islandId ID dell'isola da recuperare
     * @return Result con FacilityIsland se trovata, errore se non trovata o errore di sistema
     */
    suspend operator fun invoke(islandId: String): Result<FacilityIsland> {
        return try {
            // 1. Validazione ID
            if (islandId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID isola non può essere vuoto"))
            }

            // 2. Recupero dal repository
            facilityIslandRepository.getIslandById(islandId)
                .mapCatching { island ->
                    // 3. Gestione isola non trovata
                    island ?: throw NoSuchElementException("Isola con ID '$islandId' non trovata")
                }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}