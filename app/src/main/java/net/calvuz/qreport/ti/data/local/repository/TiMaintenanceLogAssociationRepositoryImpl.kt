package net.calvuz.qreport.ti.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.ti.data.local.dao.TiMaintenanceLogAssociationDao
import net.calvuz.qreport.ti.data.local.entity.TiMaintenanceLogAssociationEntity
import net.calvuz.qreport.ti.domain.model.TiMaintenanceLogAssociation
import net.calvuz.qreport.ti.domain.repository.TiMaintenanceLogAssociationRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TiMaintenanceLogAssociationRepositoryImpl @Inject constructor(
    private val dao: TiMaintenanceLogAssociationDao
) : TiMaintenanceLogAssociationRepository {

    override suspend fun createAssociation(interventionId: String, maintenanceLogId: String): Result<String> = try {
        val now = Clock.System.now()
        val id = UUID.randomUUID().toString()
        dao.insert(
            TiMaintenanceLogAssociationEntity(
                id = id,
                interventionId = interventionId,
                maintenanceLogId = maintenanceLogId,
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

    override suspend fun deleteAssociationsByLog(maintenanceLogId: String): Result<Unit> = try {
        dao.deleteByLogId(maintenanceLogId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getAssociation(interventionId: String, maintenanceLogId: String): TiMaintenanceLogAssociation? =
        dao.getAssociation(interventionId, maintenanceLogId)?.toDomain()

    override suspend fun getAssociationsByIntervention(interventionId: String): List<TiMaintenanceLogAssociation> =
        dao.getByInterventionId(interventionId).map { it.toDomain() }

    override fun getAssociationsByInterventionFlow(interventionId: String): Flow<List<TiMaintenanceLogAssociation>> =
        dao.observeByInterventionId(interventionId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAssociationsByLog(maintenanceLogId: String): List<TiMaintenanceLogAssociation> =
        dao.getByLogId(maintenanceLogId).map { it.toDomain() }

    override suspend fun isAssociated(interventionId: String, maintenanceLogId: String): Boolean =
        dao.exists(interventionId, maintenanceLogId)

    override suspend fun getLogCount(interventionId: String): Int =
        dao.getLogCount(interventionId)

    override suspend fun getLogIdsForIntervention(interventionId: String): List<String> =
        dao.getLogIdsByIntervention(interventionId)

    override suspend fun getInterventionIdsForLog(maintenanceLogId: String): List<String> =
        dao.getInterventionIdsByLog(maintenanceLogId)

    override suspend fun deleteSpecificAssociation(interventionId: String, maintenanceLogId: String): Result<Unit> = try {
        dao.deleteAssociation(interventionId, maintenanceLogId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun TiMaintenanceLogAssociationEntity.toDomain() = TiMaintenanceLogAssociation(
        id = id,
        interventionId = interventionId,
        maintenanceLogId = maintenanceLogId,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
        syncedAt = syncedAt?.let { Instant.fromEpochMilliseconds(it) },
        isDeleted = isDeleted
    )
}
