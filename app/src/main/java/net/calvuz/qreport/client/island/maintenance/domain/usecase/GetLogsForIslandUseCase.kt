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
 * Observes the maintenance log list for a given island as a reactive [Flow].
 *
 * Returns a [Flow] of [QrResult] so the ViewModel can handle errors
 * without try/catch via [Flow.catch].
 *
 * Logs are ordered by performed_at DESC (newest first) — enforced by the DAO.
 *
 * ViewModel usage pattern:
 * ```
 * getLogsForIslandUseCase(islandId)
 *     .catch { _uiState.update { it.copy(error = QrError.MaintenanceLogError.LoadError()) } }
 *     .collect { result ->
 *         when (result) {
 *             is QrResult.Success -> _uiState.update { it.copy(logs = result.data) }
 *             is QrResult.Error   -> _uiState.update { it.copy(error = result.error) }
 *         }
 *     }
 * ```
 */
class GetLogsForIslandUseCase @Inject constructor(
    private val logRepository: MaintenanceLogRepository
) {
    operator fun invoke(
        islandId: String
    ): Flow<QrResult<List<MaintenanceLog>, QrError.MaintenanceLogError>> =
        logRepository.getLogsForIslandFlow(islandId)
            .map<List<MaintenanceLog>, QrResult<List<MaintenanceLog>, QrError.MaintenanceLogError>> {
                QrResult.Success(it)
            }
            .catch {
                emit(QrResult.Error(QrError.MaintenanceLogError.LoadError(it.message)))
            }
}