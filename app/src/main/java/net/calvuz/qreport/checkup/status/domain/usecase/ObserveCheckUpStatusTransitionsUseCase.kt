package net.calvuz.qreport.checkup.status.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.status.domain.repository.CheckUpStatusMasterRepository
import javax.inject.Inject

/** All allowed status transitions, grouped by origin status id — for the transitions management screen. */
class ObserveCheckUpStatusTransitionsUseCase @Inject constructor(
    private val repository: CheckUpStatusMasterRepository
) {
    operator fun invoke(): Flow<Map<String, List<String>>> = repository.observeTransitions()
}
