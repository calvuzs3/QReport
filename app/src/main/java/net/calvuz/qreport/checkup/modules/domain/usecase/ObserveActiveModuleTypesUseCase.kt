package net.calvuz.qreport.checkup.modules.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster
import net.calvuz.qreport.checkup.modules.domain.repository.ModuleTypeMasterRepository
import javax.inject.Inject

/** Active module types only — for dropdowns (e.g. the checklist template editor). */
class ObserveActiveModuleTypesUseCase @Inject constructor(
    private val repository: ModuleTypeMasterRepository
) {
    operator fun invoke(): Flow<List<ModuleTypeMaster>> = repository.observeActiveModuleTypes()
}
