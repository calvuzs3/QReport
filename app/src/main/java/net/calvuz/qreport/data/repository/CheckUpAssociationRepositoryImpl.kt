package net.calvuz.qreport.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import net.calvuz.qreport.data.local.dao.CheckUpAssociationDao
import net.calvuz.qreport.data.local.entity.CheckUpIslandAssociationEntity
import net.calvuz.qreport.domain.model.checkup.AssociationType
import net.calvuz.qreport.domain.model.checkup.CheckUpIslandAssociation
import net.calvuz.qreport.domain.repository.CheckUpAssociationRepository
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckUpAssociationRepositoryImpl @Inject constructor(
    private val checkUpAssociationDao: CheckUpAssociationDao
) : CheckUpAssociationRepository {

    // ===== CREATE =====

    override suspend fun createAssociation(
        checkupId: String,
        islandId: String,
        associationType: AssociationType,
        notes: String?
    ): Result<String> = try {

        val now = Clock.System.now()
        val associationId = UUID.randomUUID().toString()

        val entity = CheckUpIslandAssociationEntity(
            id = associationId,
            checkupId = checkupId,
            islandId = islandId,
            associationType = associationType.name,
            notes = notes,
            createdAt = now.toEpochMilliseconds(),
            updatedAt = now.toEpochMilliseconds()
        )

        checkUpAssociationDao.insert(entity)
        Result.success(associationId)

    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createMultipleAssociations(
        checkupId: String,
        islandIds: List<String>,
        associationType: AssociationType,
        notes: String?
    ): Result<List<String>> = try {

        val now = Clock.System.now()

        val entities = islandIds.map { islandId ->
            CheckUpIslandAssociationEntity(
                id = UUID.randomUUID().toString(),
                checkupId = checkupId,
                islandId = islandId,
                associationType = associationType.name,
                notes = notes,
                createdAt = now.toEpochMilliseconds(),
                updatedAt = now.toEpochMilliseconds()
            )
        }

        checkUpAssociationDao.insertAll(entities)
        Result.success(entities.map { it.id })

    } catch (e: Exception) {
        Result.failure(e)
    }

    // ===== DELETE =====

    override suspend fun deleteAssociation(associationId: String): Result<Unit> = try {

        val entity = checkUpAssociationDao.getById(associationId)
        if (entity != null) {
            checkUpAssociationDao.delete(entity)
        }
        Result.success(Unit)

    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteAssociationsByCheckUp(checkupId: String): Result<Unit> = try {

        checkUpAssociationDao.deleteByCheckUpId(checkupId)
        Result.success(Unit)

    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteAssociationsByIsland(islandId: String): Result<Unit> = try {

        checkUpAssociationDao.deleteByIslandId(islandId)
        Result.success(Unit)

    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteSpecificAssociation(
        checkupId: String,
        islandId: String
    ): Result<Unit> = try {

        checkUpAssociationDao.deleteAssociation(checkupId, islandId)
        Result.success(Unit)

    } catch (e: Exception) {
        Result.failure(e)
    }

    // ===== READ =====

    override suspend fun getAssociation(associationId: String): CheckUpIslandAssociation? {
        return checkUpAssociationDao.getById(associationId)?.toDomain()
    }

    override suspend fun getAssociation(
        checkupId: String,
        islandId: String
    ): CheckUpIslandAssociation? {
        return checkUpAssociationDao.getAssociation(checkupId, islandId)?.toDomain()
    }

    override suspend fun getAssociationsByCheckUp(checkupId: String): List<CheckUpIslandAssociation> {
        return checkUpAssociationDao.getByCheckUpId(checkupId).map { it.toDomain() }
    }

    override fun getAssociationsByCheckUpFlow(checkupId: String): Flow<List<CheckUpIslandAssociation>> {
        return checkUpAssociationDao.observeByCheckUpId(checkupId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getAssociationsByIsland(islandId: String): List<CheckUpIslandAssociation> {
        return checkUpAssociationDao.getByIslandId(islandId).map { it.toDomain() }
    }

    override fun getAssociationsByIslandFlow(islandId: String): Flow<List<CheckUpIslandAssociation>> {
        return checkUpAssociationDao.observeByIslandId(islandId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    // ===== VALIDATION =====

    override suspend fun isAssociated(checkupId: String, islandId: String): Boolean {
        return checkUpAssociationDao.exists(checkupId, islandId)
    }

    override suspend fun hasAssociations(checkupId: String): Boolean {
        return checkUpAssociationDao.hasAssociations(checkupId)
    }

    override suspend fun getAssociationCount(checkupId: String): Int {
        return checkUpAssociationDao.getAssociationCount(checkupId)
    }

    // ===== STATISTICS =====

    override suspend fun getCheckUpCountForIsland(islandId: String): Int {
        return checkUpAssociationDao.getCheckUpCountForIsland(islandId)
    }

    override suspend fun getCheckUpCountForClient(clientId: String): Int {
        return checkUpAssociationDao.getCheckUpCountForClient(clientId)
    }

    // ===== ADVANCED QUERIES =====

    override suspend fun getUnassociatedCheckUpIds(): List<String> {
        return checkUpAssociationDao.getUnassociatedCheckUpIds()
    }

    override fun getUnassociatedCheckUpIdsFlow(): Flow<List<String>> {
        return checkUpAssociationDao.observeUnassociatedCheckUpIds()
    }

    override suspend fun getIslandIdsForCheckUp(checkupId: String): List<String> {
        return checkUpAssociationDao.getIslandIdsByCheckUp(checkupId)
    }

    override suspend fun getCheckUpIdsForIsland(islandId: String): List<String> {
        return checkUpAssociationDao.getCheckUpIdsByIsland(islandId)
    }

    override suspend fun getRecentAssociationsForIsland(
        islandId: String,
        limit: Int
    ): List<CheckUpIslandAssociation> {
        return checkUpAssociationDao.getRecentAssociationsForIsland(islandId, limit)
            .map { it.toDomain() }
    }

    override suspend fun getRecentAssociationsForClient(
        clientId: String,
        limit: Int
    ): List<CheckUpIslandAssociation> {
        return checkUpAssociationDao.getRecentAssociationsForClient(clientId, limit)
            .map { it.toDomain() }
    }

    // ===== MAPPER EXTENSIONS =====

    private fun CheckUpIslandAssociationEntity.toDomain(): CheckUpIslandAssociation {
        return CheckUpIslandAssociation(
            id = id,
            checkupId = checkupId,
            islandId = islandId,
            associationType = AssociationType.valueOf(associationType),
            notes = notes,
            createdAt = kotlinx.datetime.Instant.fromEpochMilliseconds(createdAt),
            updatedAt = kotlinx.datetime.Instant.fromEpochMilliseconds(updatedAt)
        )
    }
}