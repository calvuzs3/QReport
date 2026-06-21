package net.calvuz.qreport.checkup.criticality.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.criticality.domain.model.CriticalityMaster
import net.calvuz.qreport.checkup.criticality.domain.repository.CriticalityMasterRepository
import javax.inject.Inject

/** Active criticality levels only — for dropdowns (e.g. the checklist template editor). */
class ObserveActiveCriticalityLevelsUseCase @Inject constructor(
    private val repository: CriticalityMasterRepository
) {
    operator fun invoke(): Flow<List<CriticalityMaster>> = repository.observeActiveCriticalityLevels()
}
