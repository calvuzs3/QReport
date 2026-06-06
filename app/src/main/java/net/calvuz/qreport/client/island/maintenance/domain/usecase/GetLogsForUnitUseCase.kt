package net.calvuz.qreport.client.island.maintenance.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceLog
import net.calvuz.qreport.client.island.maintenance.domain.repository.MaintenanceLogRepository
import javax.inject.Inject

/**
 * Observes all maintenance logs targeting a specific catalogued [MechanicalUnit].
 *
 * Only returns logs where mechanicalUnitId == [unitId] (FK match).
 * Logs with a matching free-text componentLabel but no FK are not included —
 * those are accessible via [GetLogsForIslandUseCase] with client-side filtering.
 *
 * Intended for a future per-unit history panel in MechanicalUnit detail.
 */
class GetLogsForUnitUseCase @Inject constructor(
    private val logRepository: MaintenanceLogRepository
) {
    operator fun invoke(
        unitId: String
    ): Flow<QrResult<List<MaintenanceLog>, QrError.MaintenanceLogError>> =
        logRepository.getLogsForUnitFlow(unitId)
            .map<List<MaintenanceLog>, QrResult<List<MaintenanceLog>, QrError.MaintenanceLogError>> {
                QrResult.Success(it)
            }
            .catch {
                emit(QrResult.Error(QrError.MaintenanceLogError.LoadError(it.message)))
            }
}