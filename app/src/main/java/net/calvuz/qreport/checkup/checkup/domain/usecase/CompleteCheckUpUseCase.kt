package net.calvuz.qreport.checkup.checkup.domain.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.Clock
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUpStatus
import net.calvuz.qreport.checkup.checkup.domain.repository.CheckUpAssociationRepository
import net.calvuz.qreport.checkup.checkup.domain.repository.CheckUpRepository
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceLog
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceOperationType
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceOutcome
import net.calvuz.qreport.client.island.maintenance.domain.usecase.CreateMaintenanceLogUseCase
import timber.log.Timber
import javax.inject.Inject

class CompleteCheckUpUseCase @Inject constructor(
    private val updateCheckUpStatusUseCase: UpdateCheckUpStatusUseCase,
    private val checkUpRepository: CheckUpRepository,
    private val islandAssociationRepository: CheckUpAssociationRepository,
    private val createMaintenanceLogUseCase: CreateMaintenanceLogUseCase,
    private val associateToMaintenanceLogUseCase: AssociateCheckUpToMaintenanceLogUseCase,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(checkUpId: String): QrResult<Unit, QrError.Checkup> {
        val statusResult = updateCheckUpStatusUseCase(checkUpId, CheckUpStatus.COMPLETED)
        if (statusResult is QrResult.Error) return statusResult

        val checkUp = checkUpRepository.getCheckUpById(checkUpId)
            ?: return QrResult.Success(Unit)

        val islandIds = islandAssociationRepository.getIslandIdsForCheckUp(checkUpId)
        if (islandIds.isEmpty()) return QrResult.Success(Unit)

        val technicianName = checkUp.header.technicianInfo.name
            .ifBlank { context.getString(R.string.maint_log_unknown_technician) }

        islandIds.forEach { islandId ->
            val log = MaintenanceLog(
                id = "",
                islandId = islandId,
                operationType = MaintenanceOperationType.GENERAL_CHECKUP,
                description = context.getString(R.string.maint_log_general_checkup_description),
                technicianName = technicianName,
                technicianCompany = checkUp.header.technicianInfo.company.ifBlank { null },
                operatingHoursAtEvent = checkUp.header.islandInfo.operatingHours,
                cycleCountAtEvent = checkUp.header.islandInfo.cycleCount,
                outcome = MaintenanceOutcome.COMPLETED,
                performedAt = checkUp.completedAt ?: Clock.System.now(),
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )

            when (val logResult = createMaintenanceLogUseCase(log)) {
                is QrResult.Success -> {
                    associateToMaintenanceLogUseCase(checkUpId, logResult.data)
                        .onFailure { Timber.w(it, "Failed to link log ${logResult.data} to checkup $checkUpId") }
                }
                is QrResult.Error -> Timber.w("Maintenance log for island $islandId failed: ${logResult.error}")
            }
        }

        return QrResult.Success(Unit)
    }
}
