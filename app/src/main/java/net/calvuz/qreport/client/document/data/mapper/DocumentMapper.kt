package net.calvuz.qreport.client.document.data.mapper

import net.calvuz.qreport.client.document.data.local.entity.DocumentEntity
import net.calvuz.qreport.client.document.domain.model.DocumentCategory
import net.calvuz.qreport.client.document.domain.model.DocumentScope
import net.calvuz.qreport.client.document.domain.model.Document

/**
 * Mapper between [DocumentEntity] (data layer) and [Document] (domain layer).
 *
 * Enum conversions use [enumValueOf] with a safe fallback:
 *  - Unknown [DocumentScope] strings fall back to [DocumentScope.GLOBAL].
 *  - Unknown [DocumentCategory] strings fall back to [DocumentCategory.OTHER].
 * This prevents crashes if future server values haven't been added to the
 * local enum yet.
 */
object DocumentMapper {

    // ── Entity → Domain ───────────────────────────────────────────────────────

    fun toDomain(entity: DocumentEntity): Document =
        Document(
            id         = entity.id,
            scope      = entity.scope.toDocumentScope(),
            islandId   = entity.islandId,
            facilityId = entity.facilityId,
            clientId   = entity.clientId,
            fileName   = entity.fileName,
            filePath   = entity.filePath,
            fileSize   = entity.fileSize,
            mimeType   = entity.mimeType,
            fileHash   = entity.fileHash,
            title      = entity.title,
            category   = entity.category.toDocumentCategory(),
            notes      = entity.notes,
            createdAt  = entity.createdAt,
            updatedAt  = entity.updatedAt,
            isActive   = entity.isActive,
            isDeleted  = entity.isDeleted,
            syncedAt   = entity.syncedAt
        )

    fun toDomainList(entities: List<DocumentEntity>): List<Document> =
        entities.map { toDomain(it) }

    // ── Domain → Entity ───────────────────────────────────────────────────────

    fun toEntity(domain: Document): DocumentEntity =
        DocumentEntity(
            id         = domain.id,
            scope      = domain.scope.name,
            islandId   = domain.islandId,
            facilityId = domain.facilityId,
            clientId   = domain.clientId,
            fileName   = domain.fileName,
            filePath   = domain.filePath,
            fileSize   = domain.fileSize,
            mimeType   = domain.mimeType,
            fileHash   = domain.fileHash,
            title      = domain.title,
            category   = domain.category.name,
            notes      = domain.notes,
            createdAt  = domain.createdAt,
            updatedAt  = domain.updatedAt,
            isActive   = domain.isActive,
            isDeleted  = domain.isDeleted,
            syncedAt   = domain.syncedAt
        )

    fun toEntityList(domains: List<Document>): List<DocumentEntity> =
        domains.map { toEntity(it) }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun String.toDocumentScope(): DocumentScope =
        runCatching { enumValueOf<DocumentScope>(this) }
            .getOrDefault(DocumentScope.GLOBAL)

    private fun String.toDocumentCategory(): DocumentCategory =
        runCatching { enumValueOf<DocumentCategory>(this) }
            .getOrDefault(DocumentCategory.OTHER)
}