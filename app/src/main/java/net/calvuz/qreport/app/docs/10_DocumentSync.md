# QReport — 10. Document Sync

**Version:** 1.0
**Date:** June 2026
**Scope:** Bidirectional file synchronisation for `island_documents`
**Complements:** `7_Sync_Android.md` (entity sync), `4_8_Client_IslandDocuments.md` (feature)

---

## 1. Overview

The document sync feature extends the existing entity sync (§7) with a second,
independent channel dedicated to file bytes. The two channels are deliberately
separate:

```
Channel 1 — Entity sync (existing, unchanged)
  POST /sync/push   →  island_documents metadata included in SyncPayload JSON
  GET  /sync/pull   →  metadata received by other devices

Channel 2 — File sync (new)
  GET  /documents/manifest          →  {id, fileHash} list for diff
  POST /documents/upload/{id}       →  upload file bytes
  GET  /documents/download/{id}     →  download file bytes
```

**Why two channels?**
Entity sync is fast and atomic — a 50KB JSON payload with hundreds of records.
File sync is slow and large — a single PDF can be 30MB. Keeping them separate
means a file upload failure never blocks metadata sync, and the device can
show document metadata in the UI ("download pending") before the bytes arrive.

### 1.1 Design principles

| Principle | Implementation |
|---|---|
| Hash on bytes only | SHA-256 computed on raw file content — never includes metadata |
| Metadata immutability of hash | Editing title/category/notes never changes `fileHash` |
| Incremental transfers | Only files where local hash ≠ server hash are transferred |
| Storage abstraction | `DocumentStorageProvider` interface — local today, S3/MinIO tomorrow |
| Transfer abstraction | `DocumentTransferProvider` interface — Ktor today, presigned URLs tomorrow |
| Offline-first | File sync runs in background; UI works fully without it |
| 50MB limit | Enforced on both Android (AddDocumentUseCase) and Ktor (application.yaml) |

---

## 2. Hash Strategy

### 2.1 What is hashed

```
fileHash = SHA-256( raw bytes of the file )
```

The hash is computed exactly once — when the file is first imported or downloaded.
It is stored in `IslandDocumentEntity.fileHash` (Android) and `island_documents.file_hash`
(PostgreSQL).

### 2.2 When the hash changes

| Event | fileHash | updatedAt |
|---|---|---|
| Import (AddDocumentUseCase) | **Computed** | Set |
| Edit title / category / notes | **Unchanged** | Updated |
| Soft-delete | **Unchanged** | Updated |
| Download from server | **Recomputed and verified** | — |
| File replacement (future) | **Recomputed** | Updated |

This guarantees that the manifest diff detects only real content changes.
A rename or re-categorisation never triggers a re-download.

### 2.3 Hash computation — Android

```kotlin
// client/document/domain/model/DocumentHash.kt

object DocumentHash {

    /**
     * Computes SHA-256 over the raw bytes of a file.
     * Must be called on Dispatchers.IO.
     */
    fun compute(filePath: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        File(filePath).inputStream().use { stream ->
            val buffer = ByteArray(8192)
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Verifies a downloaded file against an expected hash. */
    fun verify(filePath: String, expectedHash: String): Boolean =
        compute(filePath) == expectedHash
}
```

### 2.4 Hash computation — Ktor server

```kotlin
// server: util/DocumentHash.kt

object DocumentHash {
    fun compute(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
```

---

## 3. Storage Abstraction

### 3.1 DocumentStorageProvider — server interface

```kotlin
// server: storage/DocumentStorageProvider.kt

/**
 * Abstracts file storage on the server side.
 *
 * Today: LocalFileStorageProvider — files under /opt/qreport/documents/
 * Future: S3StorageProvider, MinioStorageProvider — same interface, different binding
 */
interface DocumentStorageProvider {
    suspend fun store(id: String, bytes: ByteArray, mimeType: String): StorageResult
    suspend fun retrieve(id: String): ByteArray?
    suspend fun delete(id: String): Boolean
    suspend fun exists(id: String): Boolean
}

sealed class StorageResult {
    data class Success(val path: String) : StorageResult()
    data class Failure(val reason: String) : StorageResult()
}
```

### 3.2 LocalFileStorageProvider — today's implementation

```kotlin
// server: storage/LocalFileStorageProvider.kt

class LocalFileStorageProvider(
    private val basePath: String = "/opt/qreport/documents"
) : DocumentStorageProvider {

    override suspend fun store(id: String, bytes: ByteArray, mimeType: String): StorageResult {
        return try {
            val file = File("$basePath/$id")
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            StorageResult.Success(file.absolutePath)
        } catch (e: Exception) {
            StorageResult.Failure(e.message ?: "Write failed")
        }
    }

    override suspend fun retrieve(id: String): ByteArray? =
        File("$basePath/$id").takeIf { it.exists() }?.readBytes()

    override suspend fun delete(id: String): Boolean =
        File("$basePath/$id").delete()

    override suspend fun exists(id: String): Boolean =
        File("$basePath/$id").exists()
}
```

Files are stored flat by document `id` — no nested directories on the server.
The `scope` and FK fields are in PostgreSQL, not in the path.

### 3.3 Future: S3/MinIO binding

When switching to object storage, only two changes are needed:

1. Add `S3StorageProvider` implementing `DocumentStorageProvider`
2. Change the Hilt/Ktor binding from `LocalFileStorageProvider` to `S3StorageProvider`

No changes to routes, use cases, or Android code.

### 3.4 storage_backend column — PostgreSQL

A `storage_backend` column is added from day one so the server knows where each
file lives, enabling gradual migration without downtime:

```sql
ALTER TABLE island_documents
    ADD COLUMN file_hash       TEXT,
    ADD COLUMN storage_backend TEXT NOT NULL DEFAULT 'local';

CREATE INDEX idx_island_documents_file_hash ON island_documents(file_hash);
```

| Value | Meaning |
|-------|---------|
| `local` | File on VM filesystem under `/opt/qreport/documents/` |
| `s3` | Object key in S3 bucket (future) |
| `minio` | Object key in MinIO instance (future) |

---

## 4. Transfer Abstraction — Android

### 4.1 DocumentTransferProvider interface

```kotlin
// client/document/sync/DocumentTransferProvider.kt

/**
 * Abstracts the file transfer mechanism on the Android side.
 *
 * Today: KtorDocumentTransferProvider — calls /documents/* on the Ktor server
 * Future: S3TransferProvider — receives presigned URLs from Ktor, uploads directly
 *
 * The DocumentSyncUseCase depends only on this interface.
 */
interface DocumentTransferProvider {
    suspend fun getManifest(): Result<List<DocumentManifestEntry>>
    suspend fun upload(document: Document): Result<Unit>
    suspend fun download(id: String, targetPath: String): Result<String>  // returns fileHash
}

data class DocumentManifestEntry(
    val id: String,
    val fileHash: String,
    val fileSize: Long
)
```

### 4.2 KtorDocumentTransferProvider — today's implementation

```kotlin
// client/document/sync/KtorDocumentTransferProvider.kt

@Singleton
class KtorDocumentTransferProvider @Inject constructor(
    private val api: DocumentFileApi,
    private val coreFileRepo: CoreFileRepository
) : DocumentTransferProvider {

    override suspend fun getManifest(): Result<List<DocumentManifestEntry>> =
        runCatching {
            api.getManifest().body() ?: emptyList()
        }

    override suspend fun upload(document: IslandDocument): Result<Unit> =
        runCatching {
            val bytes = File(document.filePath).readBytes()
            val part = MultipartBody.Part.createFormData(
                name     = "file",
                filename = document.fileName,
                body     = bytes.toRequestBody(document.mimeType.toMediaType())
            )
            api.uploadDocument(document.id, part)
        }

    override suspend fun download(id: String, targetPath: String): Result<String> =
        runCatching {
            val response = api.downloadDocument(id)
            val body = response.body() ?: error("Empty response for document $id")
            val bytes = body.bytes()
            File(targetPath).writeBytes(bytes)
            DocumentHash.compute(targetPath)    // returns hash for verification
        }
}
```

---

## 5. Database Changes

### 5.1 Android — Migration 5 → 6

```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE island_documents ADD COLUMN file_hash TEXT"
        )
    }
}
```

Add to `IslandDocumentEntity`:
```kotlin
@ColumnInfo(name = "file_hash")
val fileHash: String? = null    // null until first sync; computed at import
```

Bump `DATABASE_VERSION` to `6`.

### 5.2 Android — AddDocumentUseCase update

After copying the file, compute and store the hash:

```kotlin
// Inside AddDocumentUseCase.invoke(), after copyFromUri() succeeds:

val fileHash = withContext(Dispatchers.IO) {
    DocumentHash.compute(targetPath)
}

val document = partialDoc.copy(
    filePath = targetPath,
    fileHash = fileHash         // ← add this field
)
```

### 5.3 PostgreSQL

```sql
ALTER TABLE island_documents
    ADD COLUMN file_hash       TEXT,
    ADD COLUMN storage_backend TEXT NOT NULL DEFAULT 'local';

CREATE INDEX idx_island_documents_file_hash ON island_documents(file_hash);
```

---

## 6. Retrofit API — Android

```kotlin
// sync/data/remote/DocumentFileApi.kt

interface DocumentFileApi {

    /**
     * Returns the list of {id, fileHash, fileSize} for all non-deleted
     * documents on the server. Used to compute the diff before syncing.
     */
    @GET("documents/manifest")
    suspend fun getManifest(): Response<List<DocumentManifestEntryDto>>

    /**
     * Uploads a document's file bytes.
     * The server computes the hash, stores the file, and updates file_hash
     * in PostgreSQL.
     */
    @Multipart
    @POST("documents/upload/{id}")
    suspend fun uploadDocument(
        @Path("id") id: String,
        @Part file: MultipartBody.Part
    ): Response<Unit>

    /**
     * Downloads a document's file bytes.
     * @Streaming prevents OkHttp from loading the whole body into memory.
     */
    @GET("documents/download/{id}")
    @Streaming
    suspend fun downloadDocument(
        @Path("id") id: String
    ): Response<ResponseBody>
}

@Serializable
data class DocumentManifestEntryDto(
    val id: String,
    @SerialName("file_hash") val fileHash: String,
    @SerialName("file_size") val fileSize: Long
)
```

---

## 7. DocumentSyncUseCase — Android

```kotlin
// client/document/sync/DocumentSyncUseCase.kt

/**
 * Orchestrates bidirectional file sync.
 *
 * Runs AFTER the normal entity sync session (SyncUseCase) so that
 * metadata records are already in Room before file bytes are transferred.
 *
 * Algorithm:
 *  1. GET /documents/manifest  →  server's {id, hash} list
 *  2. Load local documents from Room
 *  3. Compute diff:
 *       - present locally only  →  upload
 *       - present server only   →  download
 *       - hash mismatch         →  server wins  →  download
 *       - hash match            →  skip
 *  4. After each transfer: update fileHash + syncedAt in Room
 */
class DocumentSyncUseCase @Inject constructor(
    private val repository: DocumentRepository,
    private val transferProvider: DocumentTransferProvider,
    private val coreFileRepo: CoreFileRepository
) {
    data class DocumentSyncResult(
        val uploaded: Int   = 0,
        val downloaded: Int = 0,
        val skipped: Int    = 0,
        val errors: Int     = 0
    )

    suspend operator fun invoke(): Result<DocumentSyncResult> = runCatching {
        var uploaded = 0; var downloaded = 0; var skipped = 0; var errors = 0

        // 1. Server manifest
        val serverManifest = transferProvider.getManifest().getOrThrow()
        val serverMap = serverManifest.associateBy { it.id }

        // 2. Local documents (all non-deleted)
        val localDocs = repository.getChangedSince(0L).getOrThrow()
            .filter { !it.isDeleted }
        val localMap = localDocs.associateBy { it.id }

        // 3. Diff and transfer

        // Upload: local only
        for (doc in localDocs) {
            if (doc.id !in serverMap) {
                when (transferProvider.upload(doc)) {
                    is Result.success -> {
                        repository.markSynced(doc.id, System.currentTimeMillis())
                        uploaded++
                    }
                    else -> errors++
                }
            }
        }

        // Download: server only OR hash mismatch (server wins)
        for (entry in serverManifest) {
            val local = localMap[entry.id]
            val needsDownload = when {
                local == null                   -> true   // not on this device
                local.fileHash == null          -> true   // never synced
                local.fileHash != entry.fileHash -> true  // content changed on server
                else                            -> false  // identical
            }

            if (!needsDownload) { skipped++; continue }

            // Resolve target path — use existing local path or build from id
            val targetPath = local?.filePath
                ?: buildTargetPath(entry.id, local)

            when (val result = transferProvider.download(entry.id, targetPath)) {
                is Result.success -> {
                    // Verify hash after download
                    val downloadedHash = result.getOrThrow()
                    if (downloadedHash == entry.fileHash) {
                        repository.markSynced(entry.id, System.currentTimeMillis())
                        downloaded++
                    } else {
                        Timber.e("DocumentSync: hash mismatch after download id=${entry.id}")
                        errors++
                    }
                }
                else -> errors++
            }
        }

        DocumentSyncResult(uploaded, downloaded, skipped, errors)
    }

    private fun buildTargetPath(id: String, doc: IslandDocument?): String {
        // Fallback path when document is not yet in local Room
        // (server-only document arriving for the first time)
        return "${coreFileRepo.getFilesDir()}/documents/incoming/$id"
    }
}
```

---

## 8. Ktor Routes — Server

### 8.1 DocumentRoutes.kt

```kotlin
// server: routes/DocumentRoutes.kt

fun Route.documentRoutes(
    repository: DocumentServerRepository,
    storage: DocumentStorageProvider
) {
    route("/documents") {

        // ── Manifest ─────────────────────────────────────────────────────────
        get("/manifest") {
            val manifest = repository.getManifest()  // SELECT id, file_hash, file_size
            call.respond(manifest)
        }

        // ── Upload ───────────────────────────────────────────────────────────
        post("/upload/{id}") {
            val id = call.parameters["id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing id")

            // Enforce 50MB limit (also set in application.yaml)
            val multipart = call.receiveMultipart()
            var bytes: ByteArray? = null
            var mimeType = "application/octet-stream"

            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    bytes = part.streamProvider().readBytes()
                    mimeType = part.contentType?.toString() ?: mimeType
                }
                part.dispose()
            }

            val fileBytes = bytes
                ?: return@post call.respond(HttpStatusCode.BadRequest, "No file part")

            // Compute hash on server — authoritative source
            val hash = DocumentHash.compute(fileBytes)

            when (storage.store(id, fileBytes, mimeType)) {
                is StorageResult.Success -> {
                    repository.updateFileHash(id, hash, fileBytes.size.toLong())
                    call.respond(HttpStatusCode.OK)
                }
                is StorageResult.Failure -> {
                    call.respond(HttpStatusCode.InternalServerError, "Storage failed")
                }
            }
        }

        // ── Download ──────────────────────────────────────────────────────────
        get("/download/{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.NotFound)

            val bytes = storage.retrieve(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "File not found: $id")

            val mimeType = repository.getMimeType(id) ?: "application/octet-stream"
            call.respondBytes(bytes, ContentType.parse(mimeType))
        }
    }
}
```

### 8.2 application.yaml addition

```yaml
ktor:
  deployment:
    port: 8080
    maxRequestSize: 52428800   # 50 MB — applies to multipart uploads

upload:
  maxSizeBytes: 52428800       # explicit limit read by routes
```

### 8.3 DocumentServerRepository additions

```kotlin
// server: repository/DocumentServerRepository.kt  (additions to existing)

fun getManifest(): List<DocumentManifestEntryDto> {
    return transaction {
        IslandDocuments
            .select { IslandDocuments.isDeleted eq false }
            .mapNotNull { row ->
                val hash = row[IslandDocuments.fileHash] ?: return@mapNotNull null
                DocumentManifestEntryDto(
                    id       = row[IslandDocuments.id],
                    fileHash = hash,
                    fileSize = row[IslandDocuments.fileSize]
                )
            }
    }
}

fun updateFileHash(id: String, hash: String, size: Long) {
    transaction {
        IslandDocuments.update({ IslandDocuments.id eq id }) {
            it[fileHash]       = hash
            it[fileSize]       = size
            it[storageBackend] = "local"
            it[updatedAt]      = System.currentTimeMillis()
        }
    }
}

fun getMimeType(id: String): String? {
    return transaction {
        IslandDocuments
            .select { IslandDocuments.id eq id }
            .singleOrNull()
            ?.get(IslandDocuments.mimeType)
    }
}
```

---

## 9. Integration with Existing SyncUseCase

The document metadata (`island_documents` records) travels through the existing
`SyncUseCase` in the normal JSON payload. Two additions are needed:

### 9.1 Add DocumentDto to SyncPayload

```kotlin
// sync/data/remote/dto/RemoteDtos.kt  (addition)

@Serializable
data class DocumentDto(
    val id: String,
    val scope: String,
    @SerialName("island_id")   val islandId: String?   = null,
    @SerialName("facility_id") val facilityId: String? = null,
    @SerialName("client_id")   val clientId: String?   = null,
    @SerialName("file_name")   val fileName: String,
    @SerialName("file_size")   val fileSize: Long,
    @SerialName("mime_type")   val mimeType: String,
    @SerialName("file_hash")   val fileHash: String?   = null,
    val title: String,
    val category: String,
    val notes: String?         = null,
    @SerialName("created_at")  val createdAt: Long,
    @SerialName("updated_at")  val updatedAt: Long,
    @SerialName("is_active")   val isActive: Boolean,
    @SerialName("is_deleted")  val isDeleted: Boolean,
    @SerialName("synced_at")   val syncedAt: Long?     = null
    // filePath is NOT included — paths are local to each device
)
```

Note: `filePath` is never transferred. Each device resolves its own local path
from `DocumentDirectories.forScope()` after receiving the metadata.

### 9.2 SyncPayload addition

```kotlin
@Serializable
data class SyncPayload(
    // ... existing fields ...
    val documents: List<DocumentDto> = emptyList()   // ← ADD
)
```

### 9.3 Trigger document file sync after entity sync

```kotlin
// sync/domain/usecase/SyncUseCase.kt  (addition at end of invoke())

// After entity sync completes successfully, trigger file sync in background
if (syncMode == SyncMode.REMOTE_ENABLED) {
    documentSyncUseCase()   // fire-and-forget — failures don't affect entity sync result
}
```

---

## 10. Sync Flow — Complete Sequence

```
Device                                          Server
  │                                               │
  │  ── Phase 1: Entity sync (existing) ──────────│
  │                                               │
  │  POST /sync/push?since=T                      │
  │  Body includes: documents metadata list       │
  │ ──────────────────────────────────────────►  │
  │                                               │  Upsert documents metadata
  │                                               │  Pull documents changed since T
  │  Response: SyncResponse                       │
  │  pulledPayload includes: documents metadata   │
  │ ◄──────────────────────────────────────────  │
  │                                               │
  │  Apply document metadata to Room              │
  │  (filePath NOT received — resolved locally)   │
  │                                               │
  │  ── Phase 2: File sync (new) ──────────────── │
  │                                               │
  │  GET /documents/manifest                      │
  │ ──────────────────────────────────────────►  │
  │  [{id, fileHash, fileSize}, ...]              │
  │ ◄──────────────────────────────────────────  │
  │                                               │
  │  Compute diff (local hashes vs server)        │
  │                                               │
  │  For each local-only document:                │
  │  POST /documents/upload/{id}                  │
  │  Body: multipart file bytes                   │
  │ ──────────────────────────────────────────►  │
  │                                               │  Store bytes, compute+save hash
  │  200 OK                                       │
  │ ◄──────────────────────────────────────────  │
  │                                               │
  │  For each server-only or hash-mismatch:       │
  │  GET /documents/download/{id}                 │
  │ ──────────────────────────────────────────►  │
  │  bytes                                        │
  │ ◄──────────────────────────────────────────  │
  │                                               │
  │  Verify hash(downloaded bytes) == serverHash  │
  │  Save to filesDir, update fileHash in Room    │
```

---

## 11. Package Structure

### Android additions

```
client/document/
└── sync/
    ├── DocumentHash.kt                  SHA-256 utility
    ├── DocumentManifestEntry.kt         local model for manifest diff
    ├── DocumentTransferProvider.kt      interface
    ├── KtorDocumentTransferProvider.kt  Retrofit implementation
    └── DocumentSyncUseCase.kt           diff + upload + download orchestration
```

### Server additions

```
qreport-server/
└── src/main/kotlin/net/calvuz/qreport/
    ├── storage/
    │   ├── DocumentStorageProvider.kt   interface
    │   └── LocalFileStorageProvider.kt  filesystem implementation
    ├── routes/
    │   └── DocumentRoutes.kt            /documents/manifest, /upload, /download
    └── util/
        └── DocumentHash.kt              SHA-256 utility
```

---

## 12. Error Handling

| Scenario | Behaviour |
|---|---|
| Upload fails (network) | Retry on next sync; metadata already in DB |
| Download fails (network) | Retry on next sync; UI shows "pending download" |
| Hash mismatch after download | Log error, delete partial file, retry next sync |
| Server file missing (404) | Log warning; skip; metadata remains in Room |
| Storage full on device | `QrError.FileError.InsufficientSpace`; skip remaining downloads |
| File > 50MB on server | Should not occur; enforced at upload time |

---

## 13. Future: Presigned URL flow (S3/MinIO)

When switching to object storage, the transfer flow changes but the interfaces stay:

```
Device                        Ktor                        S3/MinIO
  │                             │                             │
  │  POST /documents/presign    │                             │
  │ ──────────────────────►    │                             │
  │                             │  GeneratePresignedUrl()     │
  │  {uploadUrl, downloadUrl}   │ ──────────────────────────►│
  │ ◄──────────────────────    │                             │
  │                             │                             │
  │  PUT {uploadUrl}            │                             │
  │ ────────────────────────────────────────────────────────►│
  │  200 OK                     │                             │
  │ ◄────────────────────────────────────────────────────────│
```

`KtorDocumentTransferProvider` is replaced by `S3DocumentTransferProvider`.
`DocumentSyncUseCase` is unchanged.

---

*Document: 10_DocumentSync.md — QReport v1.0 — June 2026*