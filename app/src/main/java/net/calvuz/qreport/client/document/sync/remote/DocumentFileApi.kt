package net.calvuz.qreport.client.document.sync.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

/**
 * Retrofit interface for document file transfer endpoints.
 *
 * Separate from [net.calvuz.qreport.sync.data.remote.QReportApi] so that
 * file transfer configuration (timeouts, streaming) can be tuned independently
 * from the entity sync configuration.
 *
 * All endpoints require the Authorization: Bearer header injected by
 * the existing [net.calvuz.qreport.sync.data.remote.DynamicUrlInterceptor].
 */
interface DocumentFileApi {

    /**
     * Returns {id, fileHash, fileSize} for all non-deleted documents on
     * the server. Used to compute the diff before transferring files.
     */
    @GET("documents/manifest")
    suspend fun getManifest(): Response<List<DocumentManifestEntryDto>>

    /**
     * Uploads a document's file bytes as multipart/form-data.
     * The server computes and stores the authoritative SHA-256 hash.
     *
     * Max size: 50MB (enforced by server — application.yaml maxRequestSize).
     */
    @Multipart
    @POST("documents/upload/{id}")
    suspend fun uploadDocument(
        @Path("id") id: String,
        @Part file: MultipartBody.Part
    ): Response<Unit>

    /**
     * Downloads a document's file bytes.
     *
     * @Streaming prevents OkHttp from loading the entire response body
     * into memory — essential for large files.
     */
    @GET("documents/download/{id}")
    @Streaming
    suspend fun downloadDocument(
        @Path("id") id: String
    ): Response<ResponseBody>
}

// ── DTO ───────────────────────────────────────────────────────────────────────

@Serializable
data class DocumentManifestEntryDto(
    val id: String,
    @SerialName("file_hash") val fileHash: String,
    @SerialName("file_size") val fileSize: Long
)