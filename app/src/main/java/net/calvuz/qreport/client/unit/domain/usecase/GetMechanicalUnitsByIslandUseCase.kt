package net.calvuz.qreport.client.unit.domain.usecase

import kotlinx.coroutines.flow.first
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import javax.inject.Inject


/**
 * Returns the list of active [MechanicalUnit]s belonging to a given island.
 */
class GetMechanicalUnitsByIslandUseCase @Inject constructor(
    private val repository: MechanicalUnitRepository
) {
    suspend operator fun invoke(islandId: String): Result<List<MechanicalUnit>> = runCatching {
        repository.getForIslandFlow(islandId).first()
    }
}

