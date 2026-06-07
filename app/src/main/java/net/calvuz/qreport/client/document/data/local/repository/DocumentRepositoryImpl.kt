package net.calvuz.qreport.client.document.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.document.data.local.dao.IslandDocumentDao
import net.calvuz.qreport.client.document.data.mapper.IslandDocumentMapper
import net.calvuz.qreport.client.document.domain.model.DocumentCategory
import net.calvuz.qreport.client.document.domain.model.IslandDocument
import net.calvuz.qreport.client.document.domain.repository.DocumentRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [DocumentRepository].
 *
 * Responsibilities:
 *  - Translate between [IslandDocument] (domain) and [IslandDocumentEntity] (data)
 *    via [IslandDocumentMapper].
 *  - Delegate all persistence to [IslandDocumentDao].
 *  - Wrap every suspend operation in runCatching → Result.
 *
 * Does NOT contain business logic — that belongs in use cases.
 */
@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val dao: IslandDocumentDao
) : DocumentRepository {

    // =========================================================================
    // REACTIVE
    // =========================================================================

    override fun getDocumentsForIslandFlow(islandId: String): Flow<List<IslandDocument>> =
        dao.getDocumentsForIslandFlow(islandId)
            .map { IslandDocumentMapper.toDomainList(it) }

    override fun getDocumentsForFacilityFlow(facilityId: String): Flow<List<IslandDocument>> =
        dao.getDocumentsForFacilityFlow(facilityId)
            .map { IslandDocumentMapper.toDomainList(it) }

    override fun getDocumentsForClientFlow(clientId: String): Flow<List<IslandDocument>> =
        dao.getDocumentsForClientFlow(clientId)
            .map { IslandDocumentMapper.toDomainList(it) }

    override fun getGlobalDocumentsFlow(): Flow<List<IslandDocument>> =
        dao.getGlobalDocumentsFlow()
            .map { IslandDocumentMapper.toDomainList(it) }

    override fun getDocumentsForIslandByCategoryFlow(
        islandId: String,
        category: DocumentCategory
    ): Flow<List<IslandDocument>> =
        dao.getDocumentsForIslandByCategoryFlow(islandId, category.name)
            .map { IslandDocumentMapper.toDomainList(it) }

    override fun getDocumentsForFacilityByCategoryFlow(
        facilityId: String,
        category: DocumentCategory
    ): Flow<List<IslandDocument>> =
        dao.getDocumentsForFacilityByCategoryFlow(facilityId, category.name)
            .map { IslandDocumentMapper.toDomainList(it) }

    // =========================================================================
    // SUSPEND — read
    // =========================================================================

    override suspend fun getDocumentById(id: String): Result<IslandDocument?> =
        runCatching {
            dao.getDocumentById(id)?.let { IslandDocumentMapper.toDomain(it) }
        }.onFailure { Timber.e(it, "getDocumentById failed: id=$id") }

    override suspend fun countDocumentsForIsland(islandId: String): Result<Int> =
        runCatching {
            dao.countDocumentsForIsland(islandId)
        }.onFailure { Timber.e(it, "countDocumentsForIsland failed: islandId=$islandId") }

    override suspend fun countDocumentsForFacility(facilityId: String): Result<Int> =
        runCatching {
            dao.countDocumentsForFacility(facilityId)
        }.onFailure { Timber.e(it, "countDocumentsForFacility failed: facilityId=$facilityId") }

    override suspend fun countGlobalDocuments(): Result<Int> =
        runCatching {
            dao.countGlobalDocuments()
        }.onFailure { Timber.e(it, "countGlobalDocuments failed") }

    // =========================================================================
    // SUSPEND — write
    // =========================================================================

    override suspend fun insertDocument(document: IslandDocument): Result<Unit> =
        runCatching {
            dao.insertDocument(IslandDocumentMapper.toEntity(document))
            Timber.d("insertDocument: id=${document.id} scope=${document.scope}")
        }.onFailure { Timber.e(it, "insertDocument failed: id=${document.id}") }

    override suspend fun updateDocument(document: IslandDocument): Result<Unit> =
        runCatching {
            dao.updateDocument(IslandDocumentMapper.toEntity(document))
            Timber.d("updateDocument: id=${document.id}")
        }.onFailure { Timber.e(it, "updateDocument failed: id=${document.id}") }

    override suspend fun markDeleted(id: String): Result<Unit> =
        runCatching {
            val existing = dao.getDocumentById(id)

            when {
                existing == null -> {
                    Timber.w("markDeleted: document not found id=$id")
                    // Idempotent — not an error
                }
                !existing.isActive && !existing.isDeleted -> {
                    // Already deactivated → advance to stage 2
                    dao.markDocumentDeleted(id)
                    Timber.d("markDeleted stage 2: id=$id")
                }
                existing.isActive -> {
                    // Normal → deactivate (stage 1)
                    dao.deactivateDocument(id)
                    Timber.d("markDeleted stage 1 (deactivate): id=$id")
                }
                else -> {
                    // Already fully deleted — idempotent
                    Timber.d("markDeleted: already deleted id=$id")
                }
            }
        }.onFailure { Timber.e(it, "markDeleted failed: id=$id") }

    // =========================================================================
    // SYNC
    // =========================================================================

    override suspend fun getPendingSync(): Result<List<IslandDocument>> =
        runCatching {
            IslandDocumentMapper.toDomainList(dao.getPendingSync())
        }.onFailure { Timber.e(it, "getPendingSync failed") }

    override suspend fun getChangedSince(since: Long): Result<List<IslandDocument>> =
        runCatching {
            IslandDocumentMapper.toDomainList(dao.getChangedSince(since))
        }.onFailure { Timber.e(it, "getChangedSince failed: since=$since") }

    override suspend fun markSynced(id: String, syncedAt: Long): Result<Unit> =
        runCatching {
            dao.markSynced(id, syncedAt)
            Timber.d("markSynced: id=$id syncedAt=$syncedAt")
        }.onFailure { Timber.e(it, "markSynced failed: id=$id") }

    override suspend fun upsertDocuments(documents: List<IslandDocument>): Result<Unit> =
        runCatching {
            dao.upsertDocuments(IslandDocumentMapper.toEntityList(documents))
            Timber.d("upsertDocuments: count=${documents.size}")
        }.onFailure { Timber.e(it, "upsertDocuments failed: count=${documents.size}") }
}