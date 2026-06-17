package net.calvuz.qreport.client.island.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.island.data.local.entity.IslandTypeEntity
import net.calvuz.qreport.client.island.domain.repository.IslandTypeMasterRepository
import javax.inject.Inject

/** Active island types only — for the type picker in the island creation/edit form. */
class ObserveActiveIslandTypesUseCase @Inject constructor(
    private val repository: IslandTypeMasterRepository
) {
    operator fun invoke(): Flow<List<IslandTypeEntity>> = repository.observeActiveIslandTypes()
}
