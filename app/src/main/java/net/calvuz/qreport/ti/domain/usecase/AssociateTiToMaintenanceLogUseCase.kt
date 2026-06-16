package net.calvuz.qreport.ti.domain.usecase

import net.calvuz.qreport.ti.domain.model.TiMaintenanceLogAssociation
import net.calvuz.qreport.ti.domain.repository.TiMaintenanceLogAssociationRepository
import javax.inject.Inject

class AssociateTiToMaintenanceLogUseCase @Inject constructor(
    private val associationRepository: TiMaintenanceLogAssociationRepository
) {
    suspend operator fun invoke(interventionId: String, maintenanceLogId: String): Result<String> {
        if (associationRepository.isAssociated(interventionId, maintenanceLogId))
            return Result.failure(IllegalStateException("Already associated"))
        return associationRepository.createAssociation(interventionId, maintenanceLogId)
    }
}

class RemoveTiMaintenanceLogAssociationUseCase @Inject constructor(
    private val associationRepository: TiMaintenanceLogAssociationRepository
) {
    suspend operator fun invoke(interventionId: String, maintenanceLogId: String): Result<Unit> =
        associationRepository.deleteSpecificAssociation(interventionId, maintenanceLogId)
}

class GetMaintenanceLogsForTiUseCase @Inject constructor(
    private val associationRepository: TiMaintenanceLogAssociationRepository
) {
    suspend operator fun invoke(interventionId: String): List<TiMaintenanceLogAssociation> =
        associationRepository.getAssociationsByIntervention(interventionId)

    suspend fun getLogIds(interventionId: String): List<String> =
        associationRepository.getLogIdsForIntervention(interventionId)
}

class GetTisForMaintenanceLogUseCase @Inject constructor(
    private val associationRepository: TiMaintenanceLogAssociationRepository
) {
    suspend operator fun invoke(maintenanceLogId: String): List<TiMaintenanceLogAssociation> =
        associationRepository.getAssociationsByLog(maintenanceLogId)
}
