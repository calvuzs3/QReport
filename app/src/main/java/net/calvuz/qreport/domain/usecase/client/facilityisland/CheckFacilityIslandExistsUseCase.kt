package net.calvuz.qreport.domain.usecase.client.facilityisland

import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.repository.FacilityIslandRepository
import javax.inject.Inject

class CheckFacilityIslandExistsUseCase @Inject constructor(
    private val facilityIslandRepository: FacilityIslandRepository
) {

    /**
     * Verifica che l'isola esista e la restituisce
     */
    suspend operator fun invoke(islandId: String): Result<FacilityIsland> {
        return facilityIslandRepository.getIslandById(islandId)
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