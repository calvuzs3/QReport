package net.calvuz.qreport.client.island.maintenance.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.usecase.CheckIslandExistsUseCase
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceLog
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceOperationType
import net.calvuz.qreport.client.island.maintenance.domain.repository.MaintenanceLogRepository
import net.calvuz.qreport.client.unit.domain.usecase.CheckMechanicalUnitExistsUseCase
import java.util.UUID
import javax.inject.Inject

/**
 * Creates a new [MaintenanceLog] for a robotic island.
 *
 * Validation steps (in order):
 *  1. description must not be blank
 *  2. technicianName must not be blank
 *  3. if operationType == OTHER, customOperationLabel must not be blank
 *  4. performedAt must not be in the future
 *  5. island must exist and be active
 *  6. if mechanicalUnitId is provided, the unit must belong to the given island
 *
 * On success the log is assigned a new UUID and persisted.
 * createdAt and updatedAt are set to Clock.System.now() at persist time.
 */
class CreateMaintenanceLogUseCase @Inject constructor(
    private val logRepository: MaintenanceLogRepository,
    private val checkIslandExists: CheckIslandExistsUseCase,
    private val checkUnitExists: CheckMechanicalUnitExistsUseCase
) {
    suspend operator fun invoke(
        log: MaintenanceLog
    ): QrResult<String, QrError.MaintenanceLogError> {

        // 1. Description required
        if (log.description.isBlank())
            return QrResult.Error(QrError.MaintenanceLogError.MissingDescription())

        // 2. Technician name required
        if (log.technicianName.isBlank())
            return QrResult.Error(QrError.MaintenanceLogError.MissingTechnicianName())

        // 3. OTHER requires a custom label
        if (log.operationType == MaintenanceOperationType.OTHER &&
            log.customOperationLabel.isNullOrBlank()
        ) return QrResult.Error(QrError.MaintenanceLogError.MissingCustomLabel())

        // 4. performedAt must not be in the future
        if (log.performedAt > Clock.System.now())
            return QrResult.Error(QrError.MaintenanceLogError.InvalidPerformedAt())

        // 5. Island must exist and be active
        when (checkIslandExists(log.islandId)) {
            is QrResult.Error -> return QrResult.Error(QrError.MaintenanceLogError.IslandNotFound())
            is QrResult.Success -> Unit
        }

        // 6. If a unit FK is provided, verify it belongs to this island
        if (log.mechanicalUnitId != null) {
            val unit = when (val r = checkUnitExists(log.mechanicalUnitId)) {
                is QrResult.Error -> return QrResult.Error(QrError.MaintenanceLogError.UnitNotInIsland())
                is QrResult.Success -> r.data
            }
            if (unit.islandId != log.islandId)
                return QrResult.Error(QrError.MaintenanceLogError.UnitNotInIsland())
        }

        // 7. Assign ID and timestamps, then persist
        val now = Clock.System.now()
        val logId = UUID.randomUUID().toString()
        val readyLog = log.copy(
            id = logId,
            createdAt = now,
            updatedAt = now
        )

        return logRepository.createLog(readyLog).fold(
            onSuccess = { QrResult.Success(logId) },
            onFailure = { QrResult.Error(QrError.MaintenanceLogError.CreateError(it.message)) }
        )
    }
}