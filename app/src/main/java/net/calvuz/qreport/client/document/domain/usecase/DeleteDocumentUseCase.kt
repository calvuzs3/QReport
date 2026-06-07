package net.calvuz.qreport.client.document.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.file.domain.repository.CoreFileRepository
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.document.domain.repository.DocumentRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Two-stage soft-delete for a document.
 *
 * Stage 1 (first call): sets isActive=false — document hidden in UI,
 * file not yet removed.
 *
 * Stage 2 (second call): sets isDeleted=true — sync marker set,
 * physical file removed from filesDir.
 *
 * File removal is non-critical: if it fails, the DB record is already
 * soft-deleted and invisible to queries. A background cleanup job can
 * retry orphaned files on next launch.
 *
 * Returns [DeleteDocumentResult] so the ViewModel can show the appropriate
 * snackbar message for each stage.
 */
enum class DeleteDocumentResult { DEACTIVATED, MARKED_DELETED }

class DeleteDocumentUseCase @Inject constructor(
    private val repository: DocumentRepository,
    private val coreFileRepo: CoreFileRepository
) {
    suspend operator fun invoke(
        documentId: String
    ): QrResult<DeleteDocumentResult, QrError.IslandDocumentError> {

        // 1. Load current state
        val document = repository.getDocumentById(documentId).getOrNull()
            ?: return QrResult.Error(QrError.IslandDocumentError.FileNotFound(documentId))

        return when {

            // ── Stage 1: document is active → deactivate ─────────────────────
            document.isActive -> {
                repository.markDeleted(documentId).fold(
                    onSuccess = {
                        Timber.d("DeleteDocument stage 1 (deactivated): id=$documentId")
                        QrResult.Success(DeleteDocumentResult.DEACTIVATED)
                    },
                    onFailure = {
                        QrResult.Error(QrError.IslandDocumentError.DeleteError(it.message))
                    }
                )
            }

            // ── Stage 2: already deactivated → mark deleted + remove file ────
            !document.isActive && !document.isDeleted -> {
                // DB first
                val dbResult = repository.markDeleted(documentId)
                if (dbResult.isFailure)
                    return QrResult.Error(
                        QrError.IslandDocumentError.DeleteError(
                            dbResult.exceptionOrNull()?.message
                        )
                    )

                // File removal — non-critical
                when (coreFileRepo.deleteFile(document.filePath)) {
                    is QrResult.Error -> Timber.w(
                        "DeleteDocument: file removal failed for ${document.filePath} — " +
                                "DB already soft-deleted, cleanup will retry on next launch."
                    )
                    is QrResult.Success -> Timber.d(
                        "DeleteDocument stage 2 (deleted + file removed): id=$documentId"
                    )
                }

                QrResult.Success(DeleteDocumentResult.MARKED_DELETED)
            }

            // ── Already fully deleted — idempotent ────────────────────────────
            else -> {
                Timber.d("DeleteDocument: already deleted id=$documentId — no-op")
                QrResult.Success(DeleteDocumentResult.MARKED_DELETED)
            }
        }
    }
}