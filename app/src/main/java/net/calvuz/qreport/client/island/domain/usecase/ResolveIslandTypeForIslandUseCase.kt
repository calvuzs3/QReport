package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.client.island.domain.model.IslandType
import net.calvuz.qreport.client.island.domain.model.IslandTypeMaster
import net.calvuz.qreport.client.island.domain.repository.IslandTypeMasterRepository
import javax.inject.Inject

/**
 * Resolves the [IslandTypeMaster] record for an island, preferring the FK
 * ([islandTypeId]) and falling back to matching the legacy enum's code — covers
 * islands not yet linked to a master record. Returns null only if the master
 * table itself can't resolve anything (e.g. not yet seeded/synced).
 */
class ResolveIslandTypeForIslandUseCase @Inject constructor(
    private val repository: IslandTypeMasterRepository
) {
    suspend operator fun invoke(islandTypeId: String?, legacyType: IslandType): IslandTypeMaster? {
        val all = repository.getIslandTypes().getOrNull() ?: return null
        return all.find { it.id == islandTypeId } ?: all.find { it.code == legacyType.code }
    }
}
