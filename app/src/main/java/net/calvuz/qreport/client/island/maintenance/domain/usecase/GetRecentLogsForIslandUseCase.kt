package net.calvuz.qreport.client.island.maintenance.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceLog
import net.calvuz.qreport.client.island.maintenance.domain.repository.MaintenanceLogRepository
import javax.inject.Inject

/**
 * Returns the [limit] most recent maintenance logs for a given island.
 *
 * Suspend (one-shot) variant — used by widgets and summary cards that
 * need a fixed snapshot rather than a live stream.
 * For the full reactive list use [GetLogsForIslandUseCase] instead.
 *
 * Default [limit] is 5, suitable for a preview card in IslandDetailScreen.
 */
class GetRecentLogsForIslandUseCase @Inject constructor(
    private val logRepository: MaintenanceLogRepository
) {
    suspend operator fun invoke(
        islandId: String,
        limit: Int = 5
    ): QrResult<List<MaintenanceLog>, QrError.MaintenanceLogError> =
        logRepository.getRecentLogsForIsland(islandId, limit).fold(
            onSuccess = { QrResult.Success(it) },
            onFailure = { QrResult.Error(QrError.MaintenanceLogError.LoadError(it.message)) }
        )
}