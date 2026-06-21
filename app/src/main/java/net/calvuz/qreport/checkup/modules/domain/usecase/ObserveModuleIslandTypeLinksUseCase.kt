package net.calvuz.qreport.checkup.modules.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.modules.domain.repository.ModuleTypeMasterRepository
import javax.inject.Inject

/** All module↔island-type links, grouped by island type id — for the association management screen. */
class ObserveModuleIslandTypeLinksUseCase @Inject constructor(
    private val repository: ModuleTypeMasterRepository
) {
    operator fun invoke(): Flow<Map<String, List<String>>> = repository.observeModuleIslandTypeLinks()
}
