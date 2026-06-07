package net.calvuz.qreport.client.document.domain.usecase

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.file.domain.repository.CoreFileRepository
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.document.domain.model.IslandDocument
import timber.log.Timber
import javax.inject.Inject

/**
 * Opens a document in an external app using a FileProvider URI.
 *
 * Strategy (same pattern as MapUtils):
 *  1. Exact MIME type → specific app chooser
 *  2. Fallback to '*\/*' → generic chooser (all apps that open any file)
 *  3. No app available → [QrError.IslandDocumentError.NoAppAvailable] (no crash)
 *
 * [startActivity] is never called without a prior [Intent.resolveActivity] check,
 * so [ActivityNotFoundException] cannot reach the caller.
*/
 */
class OpenDocumentUseCase @Inject constructor(
    private val coreFileRepo: CoreFileRepository, @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(
        document: IslandDocument
    ): QrResult<Unit, QrError.IslandDocumentError> {

        // 1. Verify file still exists on disk
        if (!coreFileRepo.fileExists(document.filePath)) {
            Timber.w("OpenDocument: file not found on disk: ${document.filePath}")
            return QrResult.Error(QrError.IslandDocumentError.FileNotFound(document.filePath))
        }

        // 2. Create FileProvider URI
        val uri = when (val r = coreFileRepo.createFileProviderUri(document.filePath)) {
            is QrResult.Error -> {
                Timber.e("OpenDocument: FileProvider URI creation failed for ${document.filePath}")
                return QrResult.Error(QrError.IslandDocumentError.OpenFailed("FileProvider error"))
            }

            is QrResult.Success -> r.data
        }

        // 3. Try opening with exact MIME type, then fall back to "*/*"
        val opened = tryOpen(uri, document.mimeType) ?: tryOpen(uri, "*/*")

        return if (opened != null) {
            Timber.d("OpenDocument: opened ${document.fileName} (mime=${document.mimeType})")
            QrResult.Success(Unit)
        } else {
            Timber.w("OpenDocument: no app available for mime=${document.mimeType}")
            QrResult.Error(QrError.IslandDocumentError.NoAppAvailable(document.mimeType))
        }
    }

    /**
     * Attempts to open [uri] with the given [mimeType].
     * Returns [Unit] on success, null if no app is available.
     * Never throws.
     */
    private fun tryOpen(uri: Uri, mimeType: String): Unit? {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check before launching — avoids ActivityNotFoundException crash
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(
                    Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } else {
                Timber.d("OpenDocument: resolveActivity returned null for mimeType=$mimeType")
                null
            }
        } catch (_: ActivityNotFoundException) {
            Timber.w("OpenDocument: ActivityNotFoundException for mimeType=$mimeType")
            null
        } catch (e: Exception) {
            Timber.e(e, "OpenDocument: unexpected error for mimeType=$mimeType")
            null
        }
    }
}