package net.calvuz.qreport.checkup.criticality.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.criticality.domain.model.CriticalityMaster
import net.calvuz.qreport.checkup.criticality.domain.repository.CriticalityMasterRepository
import javax.inject.Inject

/** Observes the criticality level master data list (active and inactive), for the management screen. */
class ObserveCriticalityLevelsUseCase @Inject constructor(
    private val repository: CriticalityMasterRepository
) {
    operator fun invoke(): Flow<List<CriticalityMaster>> = repository.observeCriticalityLevels()
}
