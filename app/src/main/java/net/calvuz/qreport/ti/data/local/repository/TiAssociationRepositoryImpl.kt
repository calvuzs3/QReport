package net.calvuz.qreport.ti.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.ti.data.local.dao.TiAssociationDao
import net.calvuz.qreport.ti.data.local.entity.TiIslandAssociationEntity
import net.calvuz.qreport.ti.domain.model.TiAssociationType
import net.calvuz.qreport.ti.domain.model.TiIslandAssociation
import net.calvuz.qreport.ti.domain.repository.TiAssociationRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TiAssociationRepositoryImpl @Inject constructor(
    private val dao: TiAssociationDao
) : TiAssociationRepository {

    override suspend fun createAssociation(
        interventionId: String,
        islandId: String,
        associationType: TiAssociationType,
        notes: String?
    ): Result<String> = try {
        val now = Clock.System.now()
        val id = UUID.randomUUID().toString()
        dao.insert(
            TiIslandAssociationEntity(
                id = id,
                interventionId = interventionId,
                islandId = islandId,
                associationType = associationType.name,
                notes = notes,
                createdAt = now.toEpochMilliseconds(),
                updatedAt = now.toEpochMilliseconds()
            )
        )
        Result.success(id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteAssociation(associationId: String): Result<Unit> = try {
        val entity = dao.getById(associationId)
        if (entity != null) dao.delete(entity)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteAssociationsByIntervention(interventionId: String): Result<Unit> = try {
        dao.deleteByInterventionId(interventionId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteAssociationsByIsland(islandId: String): Result<Unit> = try {
        dao.deleteByIslandId(islandId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteSpecificAssociation(interventionId: String, islandId: String): Result<Unit> = try {
        dao.deleteAssociation(interventionId, islandId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getAssociation(associationId: String): TiIslandAssociation? =
        dao.getById(associationId)?.toDomain()

    override suspend fun getAssociation(interventionId: String, islandId: String): TiIslandAssociation? =
        dao.getAssociation(interventionId, islandId)?.toDomain()

    override suspend fun getAssociationsByIntervention(interventionId: String): List<TiIslandAssociation> =
        dao.getByInterventionId(interventionId).map { it.toDomain() }

    override fun getAssociationsByInterventionFlow(interventionId: String): Flow<List<TiIslandAssociation>> =
        dao.observeByInterventionId(interventionId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAssociationsByIsland(islandId: String): List<TiIslandAssociation> =
        dao.getByIslandId(islandId).map { it.toDomain() }

    override fun getAssociationsByIslandFlow(islandId: String): Flow<List<TiIslandAssociation>> =
        dao.observeByIslandId(islandId).map { list -> list.map { it.toDomain() } }

    override suspend fun isAssociated(interventionId: String, islandId: String): Boolean =
        dao.exists(interventionId, islandId)

    override suspend fun hasAssociations(interventionId: String): Boolean =
        dao.hasAssociations(interventionId)

    override suspend fun getAssociationCount(interventionId: String): Int =
        dao.getAssociationCount(interventionId)

    override suspend fun getTiCountForIsland(islandId: String): Int =
        dao.getTiCountForIsland(islandId)

    override suspend fun getTiCountForClient(clientId: String): Int =
        dao.getTiCountForClient(clientId)

    override suspend fun getIslandIdsForIntervention(interventionId: String): List<String> =
        dao.getIslandIdsByIntervention(interventionId)

    override suspend fun getInterventionIdsForIsland(islandId: String): List<String> =
        dao.getInterventionIdsByIsland(islandId)

    override suspend fun getRecentAssociationsForIsland(islandId: String, limit: Int): List<TiIslandAssociation> =
        dao.getRecentAssociationsForIsland(islandId, limit).map { it.toDomain() }

    override suspend fun getRecentAssociationsForClient(clientId: String, limit: Int): List<TiIslandAssociation> =
        dao.getRecentAssociationsForClient(clientId, limit).map { it.toDomain() }

    private fun TiIslandAssociationEntity.toDomain() = TiIslandAssociation(
        id = id,
        interventionId = interventionId,
        islandId = islandId,
        associationType = TiAssociationType.entries.firstOrNull { it.name == associationType }
            ?: TiAssociationType.STANDARD,
        notes = notes,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
        syncedAt = syncedAt?.let { Instant.fromEpochMilliseconds(it) },
        isDeleted = isDeleted
    )
}
