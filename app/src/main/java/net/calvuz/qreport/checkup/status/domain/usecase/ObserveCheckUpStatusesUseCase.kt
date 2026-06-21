package net.calvuz.qreport.checkup.status.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster
import net.calvuz.qreport.checkup.status.domain.repository.CheckUpStatusMasterRepository
import javax.inject.Inject

/** Observes the checkup status master data list (active and inactive), for the management screen. */
class ObserveCheckUpStatusesUseCase @Inject constructor(
    private val repository: CheckUpStatusMasterRepository
) {
    operator fun invoke(): Flow<List<CheckUpStatusMaster>> = repository.observeCheckUpStatuses()
}
