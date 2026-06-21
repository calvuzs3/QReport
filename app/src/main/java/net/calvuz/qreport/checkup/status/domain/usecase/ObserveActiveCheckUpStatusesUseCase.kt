package net.calvuz.qreport.checkup.status.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster
import net.calvuz.qreport.checkup.status.domain.repository.CheckUpStatusMasterRepository
import javax.inject.Inject

/** Active checkup statuses only — for chip/filter rendering and dropdowns. */
class ObserveActiveCheckUpStatusesUseCase @Inject constructor(
    private val repository: CheckUpStatusMasterRepository
) {
    operator fun invoke(): Flow<List<CheckUpStatusMaster>> = repository.observeActiveCheckUpStatuses()
}
