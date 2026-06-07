package net.calvuz.qreport.client.document.sync.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.calvuz.qreport.client.document.domain.model.IslandDocument
import net.calvuz.qreport.client.document.sync.DocumentHash
import net.calvuz.qreport.client.document.sync.DocumentManifestEntry
import net.calvuz.qreport.client.document.sync.DocumentTransferProvider
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [DocumentTransferProvider] implementation backed by the Ktor server.
 *
 * Uses the existing [DocumentFileApi] Retrofit interface.
 * Inherits the Authorization header and dynamic URL rewriting from the
 * OkHttp interceptor chain already configured in [NetworkModule].
 *
 * All I/O runs on [Dispatchers.IO].
 */
@Singleton
class KtorDocumentTransferProvider @Inject constructor(
    private val api: DocumentFileApi
) : DocumentTransferProvider {

    override suspend fun getManifest(): Result<List<DocumentManifestEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = api.getManifest()
                if (!response.isSuccessful) {
                    error("Manifest fetch failed: HTTP ${response.code()}")
                }
                response.body()?.map {
                    DocumentManifestEntry(
                        id       = it.id,
                        fileHash = it.fileHash,
                        fileSize = it.fileSize
                    )
                } ?: emptyList()
            }.onFailure { Timber.e(it, "KtorTransferProvider: getManifest failed") }
        }

    override suspend fun upload(document: IslandDocument): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(document.filePath)
                if (!file.exists()) error("File not found for upload: ${document.filePath}")

                val bytes = file.readBytes()
                val mediaType = document.mimeType.toMediaTypeOrNull()
                    ?: "application/octet-stream".toMediaTypeOrNull()

                val requestBody = bytes.toRequestBody(mediaType)
                val part = MultipartBody.Part.createFormData(
                    name     = "file",
                    filename = document.fileName,
                    body     = requestBody
                )

                val response = api.uploadDocument(document.id, part)
                if (!response.isSuccessful) {
                    error("Upload failed for ${document.id}: HTTP ${response.code()}")
                }

                Timber.d("KtorTransferProvider: uploaded ${document.fileName} (${bytes.size} bytes)")
            }.onFailure { Timber.e(it, "KtorTransferProvider: upload failed id=${document.id}") }
        }

    override suspend fun download(id: String, targetPath: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = api.downloadDocument(id)
                if (!response.isSuccessful) {
                    error("Download failed for $id: HTTP ${response.code()}")
                }

                val body = response.body()
                    ?: error("Empty response body for document $id")

                // Write to target path in chunks — avoids loading all bytes into memory
                val targetFile = File(targetPath)
                targetFile.parentFile?.mkdirs()

                body.byteStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output, bufferSize = 8192)
                    }
                }

                Timber.d("KtorTransferProvider: downloaded document $id to $targetPath")

                // Return hash for post-download verification by DocumentSyncUseCase
                DocumentHash.compute(targetPath)

            }.onFailure { Timber.e(it, "KtorTransferProvider: download failed id=$id") }
        }
}