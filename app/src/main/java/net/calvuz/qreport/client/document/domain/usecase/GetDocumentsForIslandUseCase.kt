package net.calvuz.qreport.client.document.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.document.domain.model.DocumentCategory
import net.calvuz.qreport.client.document.domain.model.Document
import net.calvuz.qreport.client.document.domain.repository.DocumentRepository
import javax.inject.Inject

/**
 * Returns a reactive [Flow] of documents for a given island.
 * Optional [category] filter narrows the result.
 *
 * The Flow emits a new list on every DB change — use it directly in
 * the ViewModel with [kotlinx.coroutines.flow.stateIn].
 */
class GetDocumentsForIslandUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    operator fun invoke(
        islandId: String,
        category: DocumentCategory? = null
    ): Flow<List<Document>> =
        if (category != null) {
            repository.getDocumentsForIslandByCategoryFlow(islandId, category)
        } else {
            repository.getDocumentsForIslandFlow(islandId)
        }
}