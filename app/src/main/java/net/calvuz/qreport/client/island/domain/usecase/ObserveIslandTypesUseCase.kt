package net.calvuz.qreport.client.island.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.island.data.local.entity.IslandTypeEntity
import net.calvuz.qreport.client.island.domain.repository.IslandTypeMasterRepository
import javax.inject.Inject

/**
 * Observes the island type master data list (active and inactive, not deleted),
 * for the "Tipi isola" management screen.
 */
class ObserveIslandTypesUseCase @Inject constructor(
    private val repository: IslandTypeMasterRepository
) {
    operator fun invoke(): Flow<List<IslandTypeEntity>> = repository.observeIslandTypes()
}
