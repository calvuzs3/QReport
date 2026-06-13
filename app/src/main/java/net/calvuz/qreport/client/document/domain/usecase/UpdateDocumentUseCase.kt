package net.calvuz.qreport.client.document.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.document.domain.model.Document
import net.calvuz.qreport.client.document.domain.repository.DocumentRepository
import javax.inject.Inject

/**
 * Updates the editable metadata of an existing document:
 * [Document.title], [Document.category], [Document.notes].
 *
 * File content, path, size, and scope FK fields are immutable after import.
 * A blank title is rejected.
 */
class UpdateDocumentUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(
        document: Document
    ): QrResult<Unit, QrError.IslandDocumentError> {

        // 1. Title must not be blank
        if (document.title.isBlank())
            return QrResult.Error(QrError.IslandDocumentError.MissingTitle())

        // 2. Verify document exists
        val existing = repository.getDocumentById(document.id).getOrNull()
            ?: return QrResult.Error(QrError.IslandDocumentError.FileNotFound(document.id))

        // 3. Persist — stamp updatedAt, preserve all other fields from the
        //    original (file path, scope, FK fields cannot change)
        val updated = existing.copy(
            title    = document.title.trim(),
            category = document.category,
            notes    = document.notes?.trim()?.takeIf { it.isNotEmpty() },
            updatedAt = System.currentTimeMillis()
        )

        return repository.updateDocument(updated).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.IslandDocumentError.UpdateError(it.message)) }
        )
    }
}