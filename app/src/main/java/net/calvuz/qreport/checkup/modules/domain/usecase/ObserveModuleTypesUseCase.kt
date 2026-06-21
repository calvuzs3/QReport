package net.calvuz.qreport.checkup.modules.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster
import net.calvuz.qreport.checkup.modules.domain.repository.ModuleTypeMasterRepository
import javax.inject.Inject

/** Observes the module type master data list (active and inactive), for the management screen. */
class ObserveModuleTypesUseCase @Inject constructor(
    private val repository: ModuleTypeMasterRepository
) {
    operator fun invoke(): Flow<List<ModuleTypeMaster>> = repository.observeModuleTypes()
}
