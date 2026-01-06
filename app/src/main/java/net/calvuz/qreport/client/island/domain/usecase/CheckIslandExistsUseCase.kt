package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import javax.inject.Inject

class CheckIslandExistsUseCase @Inject constructor(
    private val islandRepository: IslandRepository
) {

    /**
     * Verifica che l'isola esista e la restituisce
     */
    suspend operator fun invoke(islandId: String): Result<Island> {
        return islandRepository.getIslandById(islandId)
            .mapCatching { island ->
                when {
                    island == null ->
                        throw NoSuchElementException("Isola con ID '$islandId' non trovata")
                    !island.isActive ->
                        throw IllegalStateException("Isola con ID '$islandId' già eliminata")
                    else -> island
                }
            }
    }
}