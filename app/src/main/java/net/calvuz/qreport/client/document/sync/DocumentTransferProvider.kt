package net.calvuz.qreport.client.document.sync

import net.calvuz.qreport.client.document.domain.model.Document

/**
 * A single entry in the server's document manifest.
 * Contains only the identity and content fingerprint — no metadata.
 */
data class DocumentManifestEntry(
    val id: String,
    val fileHash: String,
    val fileSize: Long
)

/**
 * Abstracts the file transfer mechanism.
 *
 * Today: [KtorDocumentTransferProvider] — calls /documents/* on the Ktor server.
 * Future: S3DocumentTransferProvider — receives presigned URLs from Ktor,
 *         uploads/downloads directly to object storage.
 *
 * [DocumentSyncUseCase] depends only on this interface — switching the
 * storage backend requires only a new implementation and a Hilt rebinding.
*/*/
interface DocumentTransferProvider {

/**
 * Returns the server manifest: list of {id, fileHash, fileSize} for
 * all non-deleted documents. Used to compute the diff before transferring.
*/
suspend fun getManifest(): Result<List<DocumentManifestEntry>>

/**
 * Uploads the file bytes of [document] to the server.
 * The server computes and stores the authoritative hash.
*/
suspend fun upload(document: Document): Result<Unit>

/**
 * Downloads the file bytes for [id] and writes them to [targetPath].
 * Returns the SHA-256 hash of the downloaded bytes for post-download
 * verification.
*/
suspend fun download(id: String, targetPath: String): Result<String>
}