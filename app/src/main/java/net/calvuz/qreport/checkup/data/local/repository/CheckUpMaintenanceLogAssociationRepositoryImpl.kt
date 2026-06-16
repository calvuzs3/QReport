package net.calvuz.qreport.checkup.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.checkup.data.local.dao.CheckUpMaintenanceLogAssociationDao
import net.calvuz.qreport.checkup.data.local.entity.CheckUpMaintenanceLogAssociationEntity
import net.calvuz.qreport.checkup.domain.model.CheckUpMaintenanceLogAssociation
import net.calvuz.qreport.checkup.domain.repository.CheckUpMaintenanceLogAssociationRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckUpMaintenanceLogAssociationRepositoryImpl @Inject constructor(
    private val dao: CheckUpMaintenanceLogAssociationDao
) : CheckUpMaintenanceLogAssociationRepository {

    override suspend fun createAssociation(checkupId: String, maintenanceLogId: String): Result<String> = try {
        val now = Clock.System.now()
        val id = UUID.randomUUID().toString()
        dao.insert(
            CheckUpMaintenanceLogAssociationEntity(
                id = id,
                checkupId = checkupId,
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

    override suspend fun deleteAssociationsByCheckUp(checkupId: String): Result<Unit> = try {
        dao.deleteByCheckUpId(checkupId)
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

    override suspend fun getAssociation(checkupId: String, maintenanceLogId: String): CheckUpMaintenanceLogAssociation? =
        dao.getAssociation(checkupId, maintenanceLogId)?.toDomain()

    override suspend fun getAssociationsByCheckUp(checkupId: String): List<CheckUpMaintenanceLogAssociation> =
        dao.getByCheckUpId(checkupId).map { it.toDomain() }

    override fun getAssociationsByCheckUpFlow(checkupId: String): Flow<List<CheckUpMaintenanceLogAssociation>> =
        dao.observeByCheckUpId(checkupId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAssociationsByLog(maintenanceLogId: String): List<CheckUpMaintenanceLogAssociation> =
        dao.getByLogId(maintenanceLogId).map { it.toDomain() }

    override suspend fun isAssociated(checkupId: String, maintenanceLogId: String): Boolean =
        dao.exists(checkupId, maintenanceLogId)

    override suspend fun getLogCount(checkupId: String): Int =
        dao.getLogCount(checkupId)

    override suspend fun getLogIdsForCheckUp(checkupId: String): List<String> =
        dao.getLogIdsByCheckUp(checkupId)

    override suspend fun getCheckUpIdsForLog(maintenanceLogId: String): List<String> =
        dao.getCheckUpIdsByLog(maintenanceLogId)

    override suspend fun deleteSpecificAssociation(checkupId: String, maintenanceLogId: String): Result<Unit> = try {
        dao.deleteAssociation(checkupId, maintenanceLogId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun CheckUpMaintenanceLogAssociationEntity.toDomain() = CheckUpMaintenanceLogAssociation(
        id = id,
        checkupId = checkupId,
        maintenanceLogId = maintenanceLogId,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
        syncedAt = syncedAt?.let { Instant.fromEpochMilliseconds(it) },
        isDeleted = isDeleted
    )
}
