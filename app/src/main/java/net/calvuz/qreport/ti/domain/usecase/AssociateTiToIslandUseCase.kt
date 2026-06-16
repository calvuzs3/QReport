package net.calvuz.qreport.ti.domain.usecase

import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import net.calvuz.qreport.ti.domain.model.TiAssociationType
import net.calvuz.qreport.ti.domain.model.TiIslandAssociation
import net.calvuz.qreport.ti.domain.repository.TiAssociationRepository
import net.calvuz.qreport.ti.domain.repository.TechnicalInterventionRepository
import javax.inject.Inject

class AssociateTiToIslandUseCase @Inject constructor(
    private val associationRepository: TiAssociationRepository,
    private val interventionRepository: TechnicalInterventionRepository,
    private val islandRepository: IslandRepository
) {
    suspend operator fun invoke(
        interventionId: String,
        islandId: String,
        notes: String? = null
    ): Result<String> {
        val intervention = interventionRepository.getInterventionById(interventionId).getOrNull()
            ?: return Result.failure(IllegalArgumentException("Intervention not found: $interventionId"))
        islandRepository.getIslandById(islandId).getOrNull()
            ?: return Result.failure(IllegalArgumentException("Island not found: $islandId"))
        if (associationRepository.isAssociated(interventionId, islandId))
            return Result.failure(IllegalStateException("Already associated"))
        return associationRepository.createAssociation(interventionId, islandId, TiAssociationType.STANDARD, notes)
    }
}

class RemoveTiAssociationUseCase @Inject constructor(
    private val associationRepository: TiAssociationRepository
) {
    suspend operator fun invoke(interventionId: String, islandId: String): Result<Unit> =
        associationRepository.deleteSpecificAssociation(interventionId, islandId)
}

class GetAssociationsForTiUseCase @Inject constructor(
    private val associationRepository: TiAssociationRepository
) {
    suspend operator fun invoke(interventionId: String): List<TiIslandAssociation> =
        associationRepository.getAssociationsByIntervention(interventionId)
}

class GetAssociationsForIslandFromTiUseCase @Inject constructor(
    private val associationRepository: TiAssociationRepository
) {
    suspend operator fun invoke(islandId: String): List<TiIslandAssociation> =
        associationRepository.getAssociationsByIsland(islandId)
}

class GetTiCountForIslandUseCase @Inject constructor(
    private val associationRepository: TiAssociationRepository
) {
    suspend operator fun invoke(islandId: String): Int =
        associationRepository.getTiCountForIsland(islandId)
}

class GetTiCountForClientUseCase @Inject constructor(
    private val associationRepository: TiAssociationRepository
) {
    suspend operator fun invoke(clientId: String): Int =
        associationRepository.getTiCountForClient(clientId)
}
