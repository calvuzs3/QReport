package net.calvuz.qreport.client.document.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.document.domain.model.DocumentCategory
import net.calvuz.qreport.client.document.domain.model.IslandDocument
import net.calvuz.qreport.client.document.domain.repository.DocumentRepository
import javax.inject.Inject

/**
 * Returns a reactive [Flow] of documents for a given facility.
 * Optional [category] filter narrows the result.
 */
class GetDocumentsForFacilityUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    operator fun invoke(
        facilityId: String,
        category: DocumentCategory? = null
    ): Flow<List<IslandDocument>> =
        if (category != null) {
            repository.getDocumentsForFacilityByCategoryFlow(facilityId, category)
        } else {
            repository.getDocumentsForFacilityFlow(facilityId)
        }
}

// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns a reactive [Flow] of documents for a given client.
 */
class GetDocumentsForClientUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    operator fun invoke(clientId: String): Flow<List<IslandDocument>> =
        repository.getDocumentsForClientFlow(clientId)
}

// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns a reactive [Flow] of global documents (no parent entity).
 */
class GetGlobalDocumentsUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    operator fun invoke(): Flow<List<IslandDocument>> =
        repository.getGlobalDocumentsFlow()
}

// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns a single document by id, or null if not found.
 */
class GetDocumentByIdUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(id: String): IslandDocument? =
        repository.getDocumentById(id).getOrNull()
}