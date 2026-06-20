package net.calvuz.qreport.checkup.checkup.domain.usecase

import net.calvuz.qreport.checkup.checkup.domain.model.CheckUpMaintenanceLogAssociation
import net.calvuz.qreport.checkup.checkup.domain.repository.CheckUpMaintenanceLogAssociationRepository
import javax.inject.Inject

class AssociateCheckUpToMaintenanceLogUseCase @Inject constructor(
    private val associationRepository: CheckUpMaintenanceLogAssociationRepository
) {
    suspend operator fun invoke(checkupId: String, maintenanceLogId: String): Result<String> {
        if (associationRepository.isAssociated(checkupId, maintenanceLogId))
            return Result.failure(IllegalStateException("Already associated"))
        return associationRepository.createAssociation(checkupId, maintenanceLogId)
    }
}

class RemoveCheckUpMaintenanceLogAssociationUseCase @Inject constructor(
    private val associationRepository: CheckUpMaintenanceLogAssociationRepository
) {
    suspend operator fun invoke(checkupId: String, maintenanceLogId: String): Result<Unit> =
        associationRepository.deleteSpecificAssociation(checkupId, maintenanceLogId)
}

class GetMaintenanceLogsForCheckUpUseCase @Inject constructor(
    private val associationRepository: CheckUpMaintenanceLogAssociationRepository
) {
    suspend operator fun invoke(checkupId: String): List<CheckUpMaintenanceLogAssociation> =
        associationRepository.getAssociationsByCheckUp(checkupId)

    suspend fun getLogIds(checkupId: String): List<String> =
        associationRepository.getLogIdsForCheckUp(checkupId)
}

class GetCheckUpsForMaintenanceLogUseCase @Inject constructor(
    private val associationRepository: CheckUpMaintenanceLogAssociationRepository
) {
    suspend operator fun invoke(maintenanceLogId: String): List<CheckUpMaintenanceLogAssociation> =
        associationRepository.getAssociationsByLog(maintenanceLogId)
}
