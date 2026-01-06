package net.calvuz.qreport.checkup.domain.usecase

import net.calvuz.qreport.checkup.domain.model.AssociationType
import net.calvuz.qreport.checkup.domain.model.CheckUpIslandAssociation
import net.calvuz.qreport.checkup.domain.repository.CheckUpAssociationRepository
import net.calvuz.qreport.checkup.domain.repository.CheckUpRepository
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// ==============================================================
// 1. ASSOCIA CHECKUP A SINGOLA ISOLA
// ==============================================================

class AssociateCheckUpToIslandUseCase @Inject constructor(
    private val associationRepository: CheckUpAssociationRepository,
    private val checkUpRepository: CheckUpRepository,
    private val islandRepository: IslandRepository
) {

    suspend operator fun invoke(
        checkupId: String,
        islandId: String,
        notes: String? = null
    ): Result<String> {

        // Validazioni
        val checkUp = checkUpRepository.getCheckUpById(checkupId)
            ?: return Result.failure(Exception("CheckUp non trovato"))

        val island = islandRepository.getIslandById(islandId)

        // Verifica se già associato
        if (associationRepository.isAssociated(checkupId, islandId)) {
            return Result.failure(Exception("CheckUp già associato a questa isola"))
        }

        // Crea associazione
        return associationRepository.createAssociation(
            checkupId = checkupId,
            islandId = islandId,
            associationType = AssociationType.STANDARD,
            notes = notes
        )
    }
}

// ==============================================================
// 2. ASSOCIA CHECKUP A MULTIPLE ISOLE (Multi-Island)
// ==============================================================

class AssociateCheckUpToMultipleIslandsUseCase @Inject constructor(
    private val associationRepository: CheckUpAssociationRepository,
    private val checkUpRepository: CheckUpRepository,
    private val islandRepository: IslandRepository
) {

    suspend operator fun invoke(
        checkupId: String,
        islandIds: List<String>,
        associationType: AssociationType = AssociationType.MULTI_ISLAND,
        notes: String? = null
    ): Result<List<String>> {

        // Validazioni
        if (islandIds.isEmpty()) {
            return Result.failure(Exception("Nessuna isola specificata"))
        }

        val checkUp = checkUpRepository.getCheckUpById(checkupId)
            ?: return Result.failure(Exception("CheckUp non trovato"))

        // Verifica che tutte le isole esistano
        val islands = islandRepository.getIslandsByIds(islandIds)
            .getOrElse { return Result.failure(Exception("Errore recupero isole")) }

        if (islands.size != islandIds.size) {
            val missing = islandIds.toSet() - islands.map { it.id }.toSet()
            return Result.failure(Exception("Isole non trovate: ${missing.joinToString()}"))
        }

        // Rimuovi eventuali associazioni esistenti
        associationRepository.deleteAssociationsByCheckUp(checkupId)
            .onFailure { return Result.failure(it) }

        // Crea nuove associazioni
        return associationRepository.createMultipleAssociations(
            checkupId = checkupId,
            islandIds = islandIds,
            associationType = associationType,
            notes = notes
        )
    }
}

// ==============================================================
// 3. RIMUOVI ASSOCIAZIONE CHECKUP
// ==============================================================

class RemoveCheckUpAssociationUseCase @Inject constructor(
    private val associationRepository: CheckUpAssociationRepository
) {

    suspend operator fun invoke(checkupId: String): Result<Unit> {
        return associationRepository.deleteAssociationsByCheckUp(checkupId)
    }

    suspend fun removeSpecific(checkupId: String, islandId: String): Result<Unit> {
        return associationRepository.deleteSpecificAssociation(checkupId, islandId)
    }
}

// ==============================================================
// 4. OTTIENI ASSOCIAZIONI PER CHECKUP
// ==============================================================

class GetAssociationsForCheckUpUseCase @Inject constructor(
    private val associationRepository: CheckUpAssociationRepository
) {

    suspend operator fun invoke(checkupId: String): Result<List<CheckUpIslandAssociation>> = try {
        val associations = associationRepository.getAssociationsByCheckUp(checkupId)
        Result.success(associations)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun flow(checkupId: String): Flow<List<CheckUpIslandAssociation>> {
        return associationRepository.getAssociationsByCheckUpFlow(checkupId)
    }
}

// ==============================================================
// 5. OTTIENI ASSOCIAZIONI PER ISOLA
// ==============================================================

class GetAssociationsForIslandUseCase @Inject constructor(
    private val associationRepository: CheckUpAssociationRepository
) {

    suspend operator fun invoke(islandId: String): Result<List<CheckUpIslandAssociation>> = try {
        val associations = associationRepository.getAssociationsByIsland(islandId)
        Result.success(associations)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun flow(islandId: String): Flow<List<CheckUpIslandAssociation>> {
        return associationRepository.getAssociationsByIslandFlow(islandId)
    }
}

// ==============================================================
// 6. OTTIENI CHECKUP GENERICI (SENZA ASSOCIAZIONE)
// ==============================================================

class GetUnassociatedCheckUpsUseCase @Inject constructor(
    private val associationRepository: CheckUpAssociationRepository,
    private val checkUpRepository: CheckUpRepository
) {

    suspend operator fun invoke(): Result<List<String>> = try {
        val unassociatedIds = associationRepository.getUnassociatedCheckUpIds()
        Result.success(unassociatedIds)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Flow per UI reactive
    fun flow(): Flow<List<String>> {
        return associationRepository.getUnassociatedCheckUpIdsFlow()
    }
}

// ==============================================================
// 7. VERIFICA STATO ASSOCIAZIONE
// ==============================================================

class CheckAssociationStatusUseCase @Inject constructor(
    private val associationRepository: CheckUpAssociationRepository
) {

    suspend fun isAssociated(checkupId: String, islandId: String): Boolean {
        return associationRepository.isAssociated(checkupId, islandId)
    }

    suspend fun hasAssociations(checkupId: String): Boolean {
        return associationRepository.hasAssociations(checkupId)
    }

    suspend fun getAssociationCount(checkupId: String): Int {
        return associationRepository.getAssociationCount(checkupId)
    }
}

// ==============================================================
// 8. OTTIENI STATISTICHE
// ==============================================================

class GetAssociationStatisticsUseCase @Inject constructor(
    private val associationRepository: CheckUpAssociationRepository
) {

    suspend fun getCheckUpCountForIsland(islandId: String): Int {
        return associationRepository.getCheckUpCountForIsland(islandId)
    }

    suspend fun getCheckUpCountForClient(clientId: String): Int {
        return associationRepository.getCheckUpCountForClient(clientId)
    }

    suspend fun getRecentAssociationsForIsland(
        islandId: String,
        limit: Int = 10
    ): List<CheckUpIslandAssociation> {
        return associationRepository.getRecentAssociationsForIsland(islandId, limit)
    }

    suspend fun getRecentAssociationsForClient(
        clientId: String,
        limit: Int = 20
    ): List<CheckUpIslandAssociation> {
        return associationRepository.getRecentAssociationsForClient(clientId, limit)
    }
}