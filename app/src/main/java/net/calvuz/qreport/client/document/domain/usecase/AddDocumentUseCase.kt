package net.calvuz.qreport.client.document.domain.usecase

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.file.domain.repository.CoreFileRepository
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.document.domain.model.DocumentCategory
import net.calvuz.qreport.client.document.domain.model.DocumentDirectories
import net.calvuz.qreport.client.document.domain.model.DocumentMimeTypes
import net.calvuz.qreport.client.document.domain.model.DocumentScope
import net.calvuz.qreport.client.document.domain.model.Document
import net.calvuz.qreport.client.document.domain.repository.DocumentRepository
import net.calvuz.qreport.client.document.sync.DocumentHash
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/**
 * Imports a document from an external content URI into QReport's internal storage
 * and persists the metadata record to the database.
 *
 * Accepts any file type — MIME validation is advisory (logs a warning for unknown
 * types but does not block). Field technicians may carry proprietary formats.
 *
 * Flow:
 *  1. Resolve file name, MIME type and size from [ContentResolver]
 *  2. Check file size limit
 *  3. Ensure target directory exists via [CoreFileRepository]
 *  4. Copy bytes from content URI to internal path
 *  5. Persist [Document] to DB
 *  6. On DB failure: rollback by deleting the copied file
 *
 * The original content URI is used only during step 4 and never stored.
 * [Document.filePath] always points to the stable internal path.
 */
class AddDocumentUseCase @Inject constructor(
    private val repository: DocumentRepository,
    private val coreFileRepo: CoreFileRepository,
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Maximum accepted file size. Protects internal storage on industrial devices. */
        private const val MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024  // 50 MB
    }

    suspend operator fun invoke(
        scope: DocumentScope,
        scopeEntityId: String?,         // null when scope == GLOBAL
        sourceUri: Uri,
        category: DocumentCategory,
        title: String? = null,
        notes: String? = null
    ): QrResult<Document, QrError.IslandDocumentError> {

        // 1. Resolve metadata from ContentResolver
        val (fileName, mimeType, fileSize) = resolveUriMetadata(sourceUri)
            ?: return QrResult.Error(
                QrError.IslandDocumentError.ImportFailed("Cannot read URI metadata")
            )

        // 2. File size check
        if (fileSize > MAX_FILE_SIZE_BYTES)
            return QrResult.Error(
                QrError.IslandDocumentError.FileTooLarge(fileSize, MAX_FILE_SIZE_BYTES)
            )

        // 3. Advisory MIME check — log warning but never block
        if (!DocumentMimeTypes.isKnown(mimeType))
            Timber.w("AddDocument: unknown MIME type '$mimeType' — proceeding")

        // 4. Build the FK fields based on scope
        val islandId   = if (scope == DocumentScope.ISLAND)   scopeEntityId else null
        val facilityId = if (scope == DocumentScope.FACILITY) scopeEntityId else null
        val clientId   = if (scope == DocumentScope.CLIENT)   scopeEntityId else null

        // 5. Resolve target directory
        val dirSpec = DocumentDirectories.forScope(
            scope      = scope,
            islandId   = islandId,
            facilityId = facilityId,
            clientId   = clientId
        )
        val dirPath = when (val r = coreFileRepo.getOrCreateDirectory(dirSpec)) {
            is QrResult.Error ->
                return QrResult.Error(
                    QrError.IslandDocumentError.ImportFailed("Cannot create directory: ${r.error}")
                )
            is QrResult.Success -> r.data
        }

        // 6. Copy bytes from content URI to internal path
        val targetPath = "$dirPath/$fileName"
        val copyResult = copyFromUri(sourceUri, targetPath)
        if ( copyResult is QrResult.Error)
                return QrResult.Error(
                    QrError.IslandDocumentError.ImportFailed("File copy failed: ${copyResult.error}")
                )
        // Compute SHA-256 on the copied bytes.
        // This is the authoritative local hash — computed once and never
        // changed by metadata edits (title, category, notes).
        val fileHash = runCatching {
            DocumentHash.compute(targetPath)
        }.getOrElse { e ->
            Timber.e(e, "AddDocument: hash computation failed for $targetPath")
            null    // null hash is acceptable — sync will detect and re-hash
        }

        // 7. Build domain object
        val now = System.currentTimeMillis()
        val document = Document(
            id         = UUID.randomUUID().toString(),
            scope      = scope,
            islandId   = islandId,
            facilityId = facilityId,
            clientId   = clientId,
            fileName   = fileName,
            filePath   = targetPath,
            fileSize   = fileSize,
            mimeType   = mimeType,
            fileHash   = fileHash,
            title      = title?.trim()?.takeIf { it.isNotBlank() } ?: fileName,
            category   = category,
            notes      = notes?.trim()?.takeIf { it.isNotBlank() },
            createdAt  = now,
            updatedAt  = now
        )

        // 8. Persist — rollback file on DB failure
        return repository.insertDocument(document).fold(
            onSuccess = {
                Timber.d("AddDocument: imported '${document.fileName}' id=${document.id}")
                QrResult.Success(document)
            },
            onFailure = { exception ->
                Timber.e(exception, "AddDocument: DB insert failed, rolling back file $targetPath")
                coreFileRepo.deleteFile(targetPath)
                QrResult.Error(QrError.IslandDocumentError.CreateError(exception.message))
            }
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Reads display name, MIME type and file size from the content URI
     * using [android.content.ContentResolver].
     * Returns null if the cursor is unavailable or the required columns are missing.
     */
    private fun resolveUriMetadata(uri: Uri): Triple<String, String, Long>? {
        return try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null, null, null
            ) ?: return null

            cursor.use {
                if (!it.moveToFirst()) return null

                val name = it.getString(
                    it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                ) ?: return null

                val size = it.getLong(
                    it.getColumnIndexOrThrow(OpenableColumns.SIZE)
                )

                val mime = context.contentResolver.getType(uri)
                    ?: "application/octet-stream"

                Triple(name, mime, size)
            }
        } catch (e: Exception) {
            Timber.e(e, "AddDocument: resolveUriMetadata failed for $uri")
            null
        }
    }

    /**
     * Copies bytes from a content URI to [targetPath] using [ContentResolver.openInputStream].
     * Runs on [Dispatchers.IO] to avoid blocking the main thread.
     */
    private suspend fun copyFromUri(
        uri: Uri,
        targetPath: String
    ): QrResult<Unit, QrError.FileError> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext QrResult.Error(
                    QrError.FileError.FileReadError(uri.toString())
                )

            inputStream.use { input ->
                File(targetPath).outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Timber.d("AddDocument: copied bytes from $uri to $targetPath")
            QrResult.Success(Unit)

        } catch (e: IOException) {
            Timber.e(e, "AddDocument: IOException copying from $uri to $targetPath")
            QrResult.Error(QrError.FileError.FileCopyError(uri.toString(), targetPath))
        } catch (e: Exception) {
            Timber.e(e, "AddDocument: unexpected error copying from $uri to $targetPath")
            QrResult.Error(QrError.FileError.FileCopyError(uri.toString(), targetPath))
        }
    }
}