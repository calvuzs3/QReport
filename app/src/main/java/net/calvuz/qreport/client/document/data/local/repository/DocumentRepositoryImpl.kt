@file:Suppress("HardCodedStringLiteral")
package net.calvuz.qreport.client.document.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.document.data.local.dao.DocumentDao
import net.calvuz.qreport.client.document.data.local.entity.DocumentEntity
import net.calvuz.qreport.client.document.data.mapper.DocumentMapper
import net.calvuz.qreport.client.document.domain.model.DocumentCategory
import net.calvuz.qreport.client.document.domain.model.Document
import net.calvuz.qreport.client.document.domain.repository.DocumentRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [DocumentRepository].
 *
 * Responsibilities:
 *  - Translate between [Document] (domain) and [DocumentEntity] (data)
 *    via [DocumentMapper].
 *  - Delegate all persistence to [DocumentDao].
 *  - Wrap every suspend operation in runCatching → Result.
 *
 * Does NOT contain business logic — that belongs in use cases.
 */
@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val dao: DocumentDao
) : DocumentRepository {

    // =========================================================================
    // REACTIVE
    // =========================================================================

    override fun getDocumentsForIslandFlow(islandId: String): Flow<List<Document>> =
        dao.getDocumentsForIslandFlow(islandId)
            .map { DocumentMapper.toDomainList(it) }

    override fun getDocumentsForFacilityFlow(facilityId: String): Flow<List<Document>> =
        dao.getDocumentsForFacilityFlow(facilityId)
            .map { DocumentMapper.toDomainList(it) }

    override fun getDocumentsForClientFlow(clientId: String): Flow<List<Document>> =
        dao.getDocumentsForClientFlow(clientId)
            .map { DocumentMapper.toDomainList(it) }

    override fun getGlobalDocumentsFlow(): Flow<List<Document>> =
        dao.getGlobalDocumentsFlow()
            .map { DocumentMapper.toDomainList(it) }

    override fun getDocumentsForIslandByCategoryFlow(
        islandId: String,
        category: DocumentCategory
    ): Flow<List<Document>> =
        dao.getDocumentsForIslandByCategoryFlow(islandId, category.name)
            .map { DocumentMapper.toDomainList(it) }

    override fun getDocumentsForFacilityByCategoryFlow(
        facilityId: String,
        category: DocumentCategory
    ): Flow<List<Document>> =
        dao.getDocumentsForFacilityByCategoryFlow(facilityId, category.name)
            .map { DocumentMapper.toDomainList(it) }

    // =========================================================================
    // SUSPEND — read
    // =========================================================================

    override suspend fun getDocumentById(id: String): Result<Document?> =
        runCatching {
            dao.getDocumentById(id)?.let { DocumentMapper.toDomain(it) }
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

    override suspend fun insertDocument(document: Document): Result<Unit> =
        runCatching {
            dao.insertDocument(DocumentMapper.toEntity(document))
            Timber.d("insertDocument: id=${document.id} scope=${document.scope}")
        }.onFailure { Timber.e(it, "insertDocument failed: id=${document.id}") }

    override suspend fun updateDocument(document: Document): Result<Unit> =
        runCatching {
            dao.updateDocument(DocumentMapper.toEntity(document))
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

    override suspend fun getPendingSync(): Result<List<Document>> =
        runCatching {
            DocumentMapper.toDomainList(dao.getPendingSync())
        }.onFailure { Timber.e(it, "getPendingSync failed") }

    override suspend fun getChangedSince(since: Long): Result<List<Document>> =
        runCatching {
            DocumentMapper.toDomainList(dao.getChangedSince(since))
        }.onFailure { Timber.e(it, "getChangedSince failed: since=$since") }

    override suspend fun markSynced(id: String, syncedAt: Long): Result<Unit> =
        runCatching {
            dao.markSynced(id, syncedAt)
            Timber.d("markSynced: id=$id syncedAt=$syncedAt")
        }.onFailure { Timber.e(it, "markSynced failed: id=$id") }

    override suspend fun upsertDocuments(documents: List<Document>): Result<Unit> =
        runCatching {
            dao.upsertDocuments(DocumentMapper.toEntityList(documents))
            Timber.d("upsertDocuments: count=${documents.size}")
        }.onFailure { Timber.e(it, "upsertDocuments failed: count=${documents.size}") }
}