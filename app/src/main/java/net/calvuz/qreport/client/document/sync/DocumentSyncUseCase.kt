package net.calvuz.qreport.client.document.sync

import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.calvuz.qreport.app.file.domain.repository.CoreFileRepository
import net.calvuz.qreport.client.document.domain.model.Document
import net.calvuz.qreport.client.document.domain.repository.DocumentRepository
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Orchestrates bidirectional file sync between the device and the server.
 *
 * Must run AFTER [net.calvuz.qreport.sync.domain.usecase.SyncUseCase] so that
 * document metadata records are already in Room before file bytes are transferred.
 *
 * Algorithm:
 *  1. GET /documents/manifest  →  server {id, fileHash} list
 *  2. Load all non-deleted local documents from Room
 *  3. Compute diff:
 *       local only               →  upload
 *       server only              →  download (new document from another device)
 *       hash mismatch            →  download (server wins, same as entity last-write-wins)
 *       hash match               →  skip
 *  4. After each successful transfer: update fileHash + syncedAt in Room
 *
 * File sync failures are non-fatal — they are logged and counted in
 * [DocumentSyncResult.errors]. The entity sync result is never affected.
 */
class DocumentSyncUseCase @Inject constructor(
    private val repository: DocumentRepository,
    private val transferProvider: DocumentTransferProvider,
    private val coreFileRepo: CoreFileRepository,
    @ApplicationContext private val context: Context
) {

    data class DocumentSyncResult(
        val uploaded: Int   = 0,
        val downloaded: Int = 0,
        val skipped: Int    = 0,
        val errors: Int     = 0
    ) {
        val hasErrors: Boolean get() = errors > 0
        val totalTransferred: Int get() = uploaded + downloaded

        override fun toString(): String =
            "DocumentSyncResult(↑$uploaded ↓$downloaded =$skipped ✗$errors)"
    }

    suspend operator fun invoke(): Result<DocumentSyncResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                var uploaded = 0; var downloaded = 0; var skipped = 0; var errors = 0

                // 1. Server manifest
                val serverManifest = transferProvider.getManifest().getOrElse { e ->
                    Timber.e(e, "DocumentSync: manifest fetch failed — aborting file sync")
                    throw e
                }
                val serverMap: Map<String, DocumentManifestEntry> =
                    serverManifest.associateBy { it.id }

                Timber.d("DocumentSync: server has ${serverManifest.size} documents")

                // 2. All local non-deleted documents
                val localDocs: List<Document> = repository
                    .getChangedSince(0L)
                    .getOrElse { emptyList() }
                    .filter { !it.isDeleted }
                val localMap: Map<String, Document> = localDocs.associateBy { it.id }

                Timber.d("DocumentSync: device has ${localDocs.size} documents")

                // 3a. Upload: present locally but not on server
                for (doc in localDocs) {
                    if (doc.id in serverMap) continue   // handled in 3b

                    Timber.d("DocumentSync: uploading ${doc.fileName} id=${doc.id}")
                    transferProvider.upload(doc).fold(
                        onSuccess = {
                            repository.markSynced(doc.id, System.currentTimeMillis())
                            uploaded++
                        },
                        onFailure = { e ->
                            Timber.e(e, "DocumentSync: upload failed id=${doc.id}")
                            errors++
                        }
                    )
                }

                // 3b. Download: server-only OR hash mismatch (server wins)
                for (entry in serverManifest) {
                    val local = localMap[entry.id]

                    val needsDownload = when {
                        local == null             -> true   // new document from another device
                        local.fileHash == null    -> true   // metadata exists but no file yet
                        local.fileHash != entry.fileHash -> true  // content changed on server
                        else                      -> false  // identical — skip
                    }

                    if (!needsDownload) {
                        skipped++
                        continue
                    }

                    val targetPath = resolveTargetPath(entry.id, local)
                    Timber.d("DocumentSync: downloading id=${entry.id} to $targetPath")

                    transferProvider.download(entry.id, targetPath).fold(
                        onSuccess = { downloadedHash ->
                            // Post-download hash verification
                            if (downloadedHash == entry.fileHash) {
                                // Update fileHash in Room via repository
                                updateLocalHash(entry.id, local, targetPath, downloadedHash)
                                repository.markSynced(entry.id, System.currentTimeMillis())
                                downloaded++
                            } else {
                                Timber.e(
                                    "DocumentSync: hash mismatch after download id=${entry.id} " +
                                            "expected=${entry.fileHash} got=$downloadedHash"
                                )
                                // Remove corrupted download and retry next session
                                File(targetPath).delete()
                                errors++
                            }
                        },
                        onFailure = { e ->
                            Timber.e(e, "DocumentSync: download failed id=${entry.id}")
                            errors++
                        }
                    )
                }

                val result = DocumentSyncResult(uploaded, downloaded, skipped, errors)
                Timber.d("DocumentSync: complete — $result")
                result

            }.onFailure { Timber.e(it, "DocumentSync: sync session failed") }
        }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Resolves where to save a downloaded file.
     *
     * If the document is already in Room (local != null), uses the existing path.
     * If it's arriving for the first time (local == null), stores it in a staging
     * directory; the full path will be set when metadata is upserted.
     */
    private fun resolveTargetPath(id: String, local: Document?): String {
        if (local != null && local.filePath.isNotBlank()) {
            return local.filePath
        }
        // Staging path for documents arriving before metadata is resolved
        val stagingDir = File(context.filesDir, "documents/incoming")
        stagingDir.mkdirs()
        return File(stagingDir, id).absolutePath
    }

    /**
     * Updates the [fileHash] field on the local Room record after a
     * successful download.
     *
     * Uses [DocumentRepository.updateDocument] with the hash stamped in.
     * If [local] is null (brand-new document from server), the record was
     * upserted by the entity sync — we reload it from Room to update the hash.
     */
    private suspend fun updateLocalHash(
        id: String,
        local: Document?,
        filePath: String,
        hash: String
    ) {
        val document = local ?: repository.getDocumentById(id).getOrNull() ?: run {
            Timber.w("DocumentSync: cannot update hash — record not found id=$id")
            return
        }

        val updated = document.copy(
            filePath  = filePath,
            fileHash  = hash,
            updatedAt = document.updatedAt   // preserve original updatedAt — hash update
            // is not a user edit, don't trigger re-sync
        )
        repository.updateDocument(updated).onFailure { e ->
            Timber.e(e, "DocumentSync: failed to update hash in Room id=$id")
        }
    }
}