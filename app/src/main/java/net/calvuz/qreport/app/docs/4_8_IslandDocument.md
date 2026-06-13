# QReport — 4_8 Document Feature Reference

**Version:** 2.0
**Date:** June 2026
**Scope:** `client/document` — domain models, Room schema, repository, use cases, UI structure, file management, sync readiness

---

## 1. OVERVIEW

The Document feature allows field technicians to attach reference documents
(electrical schematics, mechanical drawings, fluid diagrams, manuals, contracts)
to any level of the data hierarchy and access them offline on-site.

```
Client ──────────────────────────────────── Documents (scope=CLIENT)
├── Facilities ──────────────────────────── Documents (scope=FACILITY)
│   └── Islands ─────────────────────────── Documents (scope=ISLAND)
│       └── Mechanical Units
└── [Global] ────────────────────────────── Documents (scope=GLOBAL)
```

Documents are **imported** from outside the app (system file picker or "Open with"
intent) and stored in `filesDir` under a stable internal path. The DB record is the
source of truth for the UI; the file system is the source of truth for the bytes.

### 1.1 Design decisions

| Decision | Choice | Rationale |
|---|---|---|
| Single table | One `documents` table with `scope` discriminant | Avoids four parallel tables with identical structure; consistent queries and sync |
| FK strategy | All scope FKs nullable, no DB-level constraint | Mirrors `MaintenanceLog.mechanicalUnitId`; GLOBAL documents have no FK, and documents survive parent deactivation |
| File types | All MIME types accepted | Field technicians carry proprietary formats; validation is advisory not blocking |
| Opening | FileProvider URI + multi-strategy intent | Graceful fallback to `*/*` chooser; never crashes |
| Delete policy | Soft-delete (two-stage) + file removal | Consistent with sync mechanism |
| Sync readiness | `synced_at` column from day one | File content sync added later without schema migration |

---

## 2. DOMAIN MODELS

### 2.1 DocumentScope

```kotlin
// client/document/domain/model/DocumentScope.kt

/**
 * Identifies which hierarchy level a document belongs to.
 * Exactly one FK field in [Document] is non-null, matching the scope.
 * GLOBAL documents have no FK — they are app-wide reference material.
 */
enum class DocumentScope {
    ISLAND,     // islandId non-null
    FACILITY,   // facilityId non-null
    CLIENT,     // clientId non-null
    GLOBAL      // no FK — normative docs, generic manuals
}
```

### 2.2 DocumentCategory

```kotlin
// client/document/domain/model/DocumentCategory.kt

/**
 * Content category — used for filtering and UI grouping.
 */
enum class DocumentCategory {
    ELECTRICAL,     // Electrical schematics
    MECHANICAL,     // Mechanical drawings
    FLUID,          // Fluid / pneumatic diagrams
    MANUAL,         // Operator or maintenance manuals
    CONTRACT,       // Service agreements, warranties
    OTHER           // Any other document type
}
```

### 2.3 DocumentMimeTypes

```kotlin
// client/document/domain/model/DocumentMimeTypes.kt

/**
 * MIME type registry for documents.
 *
 * Maps known MIME types to a display name and default category.
 * Unknown MIME types are accepted — validation is advisory only.
 */
object DocumentMimeTypes {

    data class MimeTypeInfo(
        val mimeType: String,
        val displayName: String,
        val defaultCategory: DocumentCategory
    )

    val ALL: List<MimeTypeInfo> = listOf(
        MimeTypeInfo("application/pdf",  "PDF",           DocumentCategory.OTHER),
        MimeTypeInfo("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                     "Word (.docx)",     DocumentCategory.MANUAL),
        MimeTypeInfo("application/msword",
                     "Word (.doc)",      DocumentCategory.MANUAL),
        MimeTypeInfo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                     "Excel (.xlsx)",    DocumentCategory.OTHER),
        MimeTypeInfo("application/vnd.ms-excel",
                     "Excel (.xls)",     DocumentCategory.OTHER),
        MimeTypeInfo("application/vnd.openxmlformats-officedocument.presentationml.presentation",
                     "PowerPoint (.pptx)", DocumentCategory.OTHER),
        MimeTypeInfo("text/plain",       "Testo (.txt)",  DocumentCategory.OTHER),
        MimeTypeInfo("text/csv",         "CSV",           DocumentCategory.OTHER),
        MimeTypeInfo("image/jpeg",       "Immagine JPEG", DocumentCategory.OTHER),
        MimeTypeInfo("image/png",        "Immagine PNG",  DocumentCategory.OTHER),
        MimeTypeInfo("application/zip",  "Archivio ZIP",  DocumentCategory.OTHER),
        MimeTypeInfo("application/x-rar-compressed", "Archivio RAR", DocumentCategory.OTHER),
        MimeTypeInfo("application/octet-stream", "File binario", DocumentCategory.OTHER)
    )

    private val byMimeType = ALL.associateBy { it.mimeType }

    fun forMimeType(mime: String): MimeTypeInfo? = byMimeType[mime]
    fun isKnown(mime: String): Boolean = byMimeType.containsKey(mime)

    /** Pass to GetContent launcher. */
    const val PICKER_ALL       = "*/*"
    const val PICKER_PDF_ONLY  = "application/pdf"
}
```

### 2.4 Document

```kotlin
// client/document/domain/model/Document.kt

/**
 * A reference document in QReport.
 *
 * Scope and FK fields:
 *  - [scope] determines which FK field is meaningful.
 *  - Exactly one of [islandId], [facilityId], [clientId] is non-null,
 *    matching [scope]. All are null when scope == GLOBAL.
 *
 * [filePath] is an absolute path inside the app's filesDir — never an
 * external URI. The file is always present when [isActive] is true and
 * [isDeleted] is false.
 *
 * [title] defaults to the original file name but can be edited.
 *
 * [mimeType] is stored at import time so the OS can open the file
 * correctly without re-detecting the type later.
 */
data class Document(
    val id: String,

    // ===== SCOPE =====
    val scope: DocumentScope,
    val islandId: String?   = null,     // non-null when scope == ISLAND
    val facilityId: String? = null,     // non-null when scope == FACILITY
    val clientId: String?   = null,     // non-null when scope == CLIENT
                                        // all null when scope == GLOBAL

    // ===== FILE =====
    val fileName: String,               // original file name (e.g. "schema_v3.pdf")
    val filePath: String,               // absolute path in filesDir
    val fileSize: Long,                 // bytes
    val mimeType: String,               // e.g. "application/pdf"

    // ===== METADATA =====
    val title: String,                  // editable label — defaults to fileName
    val category: DocumentCategory,
    val notes: String? = null,

    // ===== LIFECYCLE =====
    val createdAt: Long,                // epoch milliseconds
    val updatedAt: Long,                // epoch milliseconds
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,

    // ===== SYNC =====
    val syncedAt: Long? = null          // null = never synced
) {
    /** Convenience: the scope-appropriate FK as a single nullable string. */
    val scopeEntityId: String?
        get() = when (scope) {
            DocumentScope.ISLAND   -> islandId
            DocumentScope.FACILITY -> facilityId
            DocumentScope.CLIENT   -> clientId
            DocumentScope.GLOBAL   -> null
        }
}
```

### 2.5 DocumentWithContext

```kotlin
// client/document/domain/model/DocumentWithContext.kt

/**
 * Read-only projection used by list screens that need parent context
 * without a separate query.
 */
data class DocumentWithContext(
    val document: Document,
    val islandSerialNumber: String?  = null,
    val facilityName: String?        = null,
    val companyName: String?         = null
)
```

---

## 3. DATABASE SCHEMA (ROOM)

### 3.1 DocumentEntity

```kotlin
// client/document/data/local/entity/DocumentEntity.kt

/**
 * Room entity for the island_documents table.
 *
 * Scope FK design:
 *  - [islandId], [facilityId], [clientId] are all nullable.
 *  - No DB-level FK constraints are enforced (same pattern as
 *    MaintenanceLog.mechanicalUnitId) — this allows GLOBAL documents
 *    to have no FK and prevents orphan constraint violations when a
 *    parent entity is deactivated or deleted.
 *  - Referential integrity is enforced at the use case level.
 *
 * Lifecycle fields:
 *  - [isActive]   false = document logically deactivated (first delete stage)
 *  - [isDeleted]  true  = document marked for purge / server sync (second stage)
 *  - [updatedAt]  updated on every write; used for sync conflict resolution
 *
 * All normal queries filter WHERE is_deleted = 0.
 */
@Entity(
    tableName = "island_documents",
    indices = [
        Index(value = ["scope"]),
        Index(value = ["island_id"]),
        Index(value = ["facility_id"]),
        Index(value = ["client_id"]),
        Index(value = ["category"]),
        Index(value = ["is_active"]),
        Index(value = ["is_deleted"]),
        Index(value = ["updated_at"])
    ]
    // No foreignKeys block — see class KDoc
)
data class DocumentEntity(
    @PrimaryKey
    val id: String,

    // ===== SCOPE =====
    @ColumnInfo(name = "scope")
    val scope: String,                  // DocumentScope.name

    @ColumnInfo(name = "island_id")
    val islandId: String? = null,

    @ColumnInfo(name = "facility_id")
    val facilityId: String? = null,

    @ColumnInfo(name = "client_id")
    val clientId: String? = null,

    // ===== FILE =====
    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "file_size")
    val fileSize: Long,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    // ===== METADATA =====
    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "category")
    val category: String,               // DocumentCategory.name

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    // ===== LIFECYCLE =====
    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Boolean = true,

    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,

    // ===== SYNC =====
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null
)
```

### 3.2 DocumentDao

```kotlin
// client/document/data/local/dao/DocumentDao.kt

@Dao
interface DocumentDao {

    // ===== REACTIVE — by scope =====

    @Query("""
        SELECT * FROM island_documents
        WHERE island_id = :islandId AND is_deleted = 0
        ORDER BY created_at DESC
    """)
    fun getDocumentsForIslandFlow(islandId: String): Flow<List<DocumentEntity>>

    @Query("""
        SELECT * FROM island_documents
        WHERE facility_id = :facilityId AND is_deleted = 0
        ORDER BY created_at DESC
    """)
    fun getDocumentsForFacilityFlow(facilityId: String): Flow<List<DocumentEntity>>

    @Query("""
        SELECT * FROM island_documents
        WHERE client_id = :clientId AND is_deleted = 0
        ORDER BY created_at DESC
    """)
    fun getDocumentsForClientFlow(clientId: String): Flow<List<DocumentEntity>>

    @Query("""
        SELECT * FROM island_documents
        WHERE scope = 'GLOBAL' AND is_deleted = 0
        ORDER BY created_at DESC
    """)
    fun getGlobalDocumentsFlow(): Flow<List<DocumentEntity>>

    // ===== REACTIVE — filtered by category =====

    @Query("""
        SELECT * FROM island_documents
        WHERE island_id = :islandId AND category = :category AND is_deleted = 0
        ORDER BY created_at DESC
    """)
    fun getDocumentsForIslandByCategoryFlow(
        islandId: String,
        category: String
    ): Flow<List<DocumentEntity>>

    // ===== SUSPEND =====

    @Query("SELECT * FROM island_documents WHERE id = :id AND is_deleted = 0")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query("SELECT COUNT(*) FROM island_documents WHERE island_id = :islandId AND is_deleted = 0")
    suspend fun countDocumentsForIsland(islandId: String): Int

    @Query("SELECT COUNT(*) FROM island_documents WHERE scope = 'GLOBAL' AND is_deleted = 0")
    suspend fun countGlobalDocuments(): Int

    // ===== SYNC QUERIES =====

    @Query("""
        SELECT * FROM island_documents
        WHERE updated_at > COALESCE(synced_at, 0)
        ORDER BY updated_at ASC
    """)
    suspend fun getPendingSync(): List<DocumentEntity>

    @Query("""
        SELECT * FROM island_documents
        WHERE updated_at > :since
        ORDER BY updated_at ASC
    """)
    suspend fun getChangedSince(since: Long): List<DocumentEntity>

    // ===== WRITE =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("""
        UPDATE island_documents
        SET is_active = 0, updated_at = :timestamp
        WHERE id = :id
    """)
    suspend fun deactivateDocument(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE island_documents
        SET is_active = 0, is_deleted = 1, updated_at = :timestamp
        WHERE id = :id
    """)
    suspend fun markDocumentDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE island_documents SET synced_at = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocuments(documents: List<DocumentEntity>)
}
```

### 3.3 Room migration

The `island_documents` table is added in **migration 4 → 5**:

```kotlin
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS island_documents (
                id          TEXT NOT NULL PRIMARY KEY,
                scope       TEXT NOT NULL,
                island_id   TEXT,
                facility_id TEXT,
                client_id   TEXT,
                file_name   TEXT NOT NULL,
                file_path   TEXT NOT NULL,
                file_size   INTEGER NOT NULL,
                mime_type   TEXT NOT NULL,
                title       TEXT NOT NULL,
                category    TEXT NOT NULL,
                notes       TEXT,
                created_at  INTEGER NOT NULL,
                updated_at  INTEGER NOT NULL,
                is_active   INTEGER NOT NULL DEFAULT 1,
                is_deleted  INTEGER NOT NULL DEFAULT 0,
                synced_at   INTEGER
            )
        """)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_island_documents_scope       ON island_documents(scope)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_island_documents_island_id   ON island_documents(island_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_island_documents_facility_id ON island_documents(facility_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_island_documents_client_id   ON island_documents(client_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_island_documents_category    ON island_documents(category)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_island_documents_is_deleted  ON island_documents(is_deleted)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_island_documents_updated_at  ON island_documents(updated_at)")
    }
}
```

Bump `DATABASE_VERSION` to `5` in `QReportApplication`.

---

## 4. FILE STORAGE

### 4.1 Directory layout

```
filesDir/
└── documents/
    ├── islands/
    │   └── {islandId}/
    │       ├── schema_elettrico_v2.pdf
    │       └── manuale_fanuc_r30ib.pdf
    ├── facilities/
    │   └── {facilityId}/
    │       └── planimetria_nord.pdf
    ├── clients/
    │   └── {clientId}/
    │       └── contratto_2024.pdf
    └── global/
        ├── norma_en_iso_10218.pdf
        └── manuale_robot_fanuc.pdf
```

`DirectorySpec` per ogni scope:

```kotlin
// client/document/domain/model/DocumentDirectories.kt

object DocumentDirectories {
    fun forScope(document: Document): DirectorySpec = when (document.scope) {
        DocumentScope.ISLAND   -> DirectorySpec("documents/islands/${document.islandId}")
        DocumentScope.FACILITY -> DirectorySpec("documents/facilities/${document.facilityId}")
        DocumentScope.CLIENT   -> DirectorySpec("documents/clients/${document.clientId}")
        DocumentScope.GLOBAL   -> DirectorySpec("documents/global")
    }
}
```

### 4.2 Import flow

```
User taps "Aggiungi documento"
    │
    ▼
ActivityResultContracts.GetContent("*/*")   ← or "application/pdf"
    │  returns content URI (valid only during this session)
    ▼
AddDocumentUseCase
    ├── 1. Read file name, size, MIME type via ContentResolver
    ├── 2. Advisory MIME type check (warn but do not block unknown types)
    ├── 3. coreFileRepo.getOrCreateDirectory(DocumentDirectories.forScope(...))
    ├── 4. Copy bytes from content URI to internal path
    │        ContentResolver.openInputStream(uri) → File.outputStream()
    └── 5. DocumentRepository.insert()
              └── DocumentDao.insertDocument()
```

The original content URI is used only during step 4 and never stored.
The `filePath` in the entity is the stable internal path.

### 4.3 "Open with QReport" — incoming intent

When the user opens a file from Files, Drive, or email choosing QReport,
the file arrives via `Intent.ACTION_VIEW` or `Intent.ACTION_SEND` in `MainActivity`.
The nav graph routes to `AssociateDocumentScreen`, where the user picks scope,
parent entity, and category. The same `AddDocumentUseCase` handles both flows.

**AndroidManifest.xml addition:**

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <action android:name="android.intent.action.SEND" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:mimeType="*/*" />
</intent-filter>
```

### 4.4 Delete flow

```
User deletes document
    │
    ▼
DeleteDocumentUseCase
    ├── 1. DocumentRepository.markDeleted(id)   ← DB soft-delete
    └── 2. coreFileRepo.deleteFile(filePath)    ← remove bytes
```

File deletion is non-critical: if it fails, the record is already
soft-deleted and hidden from queries. A background cleanup job retries
on next launch by scanning for orphaned files.

---

## 5. REPOSITORY

### 5.1 DocumentRepository interface

```kotlin
// client/document/domain/repository/DocumentRepository.kt

interface DocumentRepository {

    // ===== REACTIVE =====
    fun getDocumentsForIslandFlow(islandId: String): Flow<List<Document>>
    fun getDocumentsForFacilityFlow(facilityId: String): Flow<List<Document>>
    fun getDocumentsForClientFlow(clientId: String): Flow<List<Document>>
    fun getGlobalDocumentsFlow(): Flow<List<Document>>
    fun getDocumentsForIslandByCategoryFlow(
        islandId: String,
        category: DocumentCategory
    ): Flow<List<Document>>

    // ===== SUSPEND =====
    suspend fun getDocumentById(id: String): Result<Document?>
    suspend fun countDocumentsForIsland(islandId: String): Result<Int>
    suspend fun countGlobalDocuments(): Result<Int>

    // ===== WRITE =====
    suspend fun insertDocument(document: Document): Result<Unit>
    suspend fun updateDocument(document: Document): Result<Unit>
    suspend fun markDeleted(id: String): Result<Unit>

    // ===== SYNC =====
    suspend fun getPendingSync(): Result<List<Document>>
    suspend fun getChangedSince(since: Long): Result<List<Document>>
    suspend fun markSynced(id: String, syncedAt: Long): Result<Unit>
    suspend fun upsertDocuments(documents: List<Document>): Result<Unit>
}
```

> The repository layer uses `kotlin.Result<T>`.
> Error translation to `QrResult<D, QrError>` happens in use cases.

---

## 6. ERROR MODEL

```kotlin
// app/error/domain/model/QrError.kt  (addendum)

sealed interface DocumentError : QrError {

    // ── Validation ────────────────────────────────────────────────────────────
    data class MissingTitle(val message: String? = null) : DocumentError
    data class FileTooLarge(val actualBytes: Long, val maxBytes: Long) : DocumentError

    // ── Business rules ────────────────────────────────────────────────────────
    /** The referenced parent entity does not exist or is inactive. */
    data class ParentNotFound(val scope: DocumentScope, val id: String?) : DocumentError

    // ── File operations ───────────────────────────────────────────────────────
    data class ImportFailed(val message: String? = null) : DocumentError
    data class FileNotFound(val path: String? = null) : DocumentError
    data class NoAppAvailable(val mimeType: String) : DocumentError
    data class OpenFailed(val message: String? = null) : DocumentError

    // ── Persistence ───────────────────────────────────────────────────────────
    data class CreateError(val message: String? = null) : DocumentError
    data class LoadError(val message: String? = null) : DocumentError
    data class UpdateError(val message: String? = null) : DocumentError
    data class DeleteError(val message: String? = null) : DocumentError
}
```

---

## 7. USE CASES

### 7.1 Full list

```
AddDocumentUseCase               import from URI → copy + insert (any scope)
GetDocumentsForIslandUseCase     returns Flow
GetDocumentsForFacilityUseCase   returns Flow
GetDocumentsForClientUseCase     returns Flow
GetGlobalDocumentsUseCase        returns Flow
GetDocumentByIdUseCase
UpdateDocumentUseCase            edit title / category / notes
DeleteDocumentUseCase            soft-delete + file removal
OpenDocumentUseCase              FileProvider URI → external app, with fallback
```

### 7.2 AddDocumentUseCase

```kotlin
// client/document/domain/usecase/AddDocumentUseCase.kt

class AddDocumentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val coreFileRepo: CoreFileRepository,
    private val context: Context
) {
    companion object {
        private const val MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024  // 50 MB
    }

    suspend operator fun invoke(
        scope: DocumentScope,
        scopeEntityId: String?,         // null when scope == GLOBAL
        sourceUri: Uri,
        category: DocumentCategory,
        title: String? = null,
        notes: String? = null
    ): QrResult<Document, QrError.DocumentError> {

        // 1. Resolve file metadata
        val (fileName, mimeType, fileSize) = resolveUriMetadata(sourceUri)
            ?: return QrResult.Error(QrError.DocumentError.ImportFailed("Cannot read URI"))

        // 2. Size check
        if (fileSize > MAX_FILE_SIZE_BYTES)
            return QrResult.Error(
                QrError.DocumentError.FileTooLarge(fileSize, MAX_FILE_SIZE_BYTES)
            )

        // 3. Advisory MIME check — log warning but do not block
        if (!DocumentMimeTypes.isKnown(mimeType))
            Timber.w("AddDocument: unknown MIME type '$mimeType' — proceeding")

        // 4. Build document object early to use DocumentDirectories.forScope()
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val partialDoc = Document(
            id = id,
            scope = scope,
            islandId   = if (scope == DocumentScope.ISLAND)   scopeEntityId else null,
            facilityId = if (scope == DocumentScope.FACILITY) scopeEntityId else null,
            clientId   = if (scope == DocumentScope.CLIENT)   scopeEntityId else null,
            fileName = fileName,
            filePath = "",              // filled in step 6
            fileSize = fileSize,
            mimeType = mimeType,
            title = title?.takeIf { it.isNotBlank() } ?: fileName,
            category = category,
            notes = notes,
            createdAt = now,
            updatedAt = now
        )

        // 5. Ensure target directory exists
        val dirSpec = DocumentDirectories.forScope(partialDoc)
        val dirPath = when (val r = coreFileRepo.getOrCreateDirectory(dirSpec)) {
            is QrResult.Error ->
                return QrResult.Error(QrError.DocumentError.ImportFailed("Cannot create directory"))
            is QrResult.Success -> r.data
        }

        // 6. Copy file bytes from URI
        val targetPath = "$dirPath/$fileName"
        val copyResult = copyFromUri(sourceUri, targetPath)
        if (copyResult is QrResult.Error)
            return QrResult.Error(QrError.DocumentError.ImportFailed(copyResult.error.toString()))

        val document = partialDoc.copy(filePath = targetPath)

        // 7. Persist — rollback file on failure
        return documentRepository.insertDocument(document).fold(
            onSuccess = { QrResult.Success(document) },
            onFailure = {
                coreFileRepo.deleteFile(targetPath)
                QrResult.Error(QrError.DocumentError.CreateError(it.message))
            }
        )
    }

    private fun resolveUriMetadata(uri: Uri): Triple<String, String, Long>? {
        return try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null, null, null
            ) ?: return null
            cursor.use {
                if (!it.moveToFirst()) return null
                val name = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                val size = it.getLong(it.getColumnIndexOrThrow(OpenableColumns.SIZE))
                val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                Triple(name, mime, size)
            }
        } catch (e: Exception) {
            Timber.e(e, "AddDocument: cannot resolve URI metadata")
            null
        }
    }

    private suspend fun copyFromUri(
        uri: Uri,
        targetPath: String
    ): QrResult<Unit, QrError.FileError> {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    File(targetPath).outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext QrResult.Error(QrError.FileError.FileReadError(uri.toString()))
                QrResult.Success(Unit)
            } catch (e: IOException) {
                QrResult.Error(QrError.FileError.FileCopyError(uri.toString(), targetPath))
            }
        }
    }
}
```

### 7.3 DeleteDocumentUseCase

```kotlin
// client/document/domain/usecase/DeleteDocumentUseCase.kt

class DeleteDocumentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val coreFileRepo: CoreFileRepository
) {
    suspend operator fun invoke(
        documentId: String
    ): QrResult<Unit, QrError.DocumentError> {

        val document = documentRepository.getDocumentById(documentId).getOrNull()
            ?: return QrResult.Error(QrError.DocumentError.FileNotFound(documentId))

        // DB soft-delete first
        val dbResult = documentRepository.markDeleted(documentId)
        if (dbResult.isFailure)
            return QrResult.Error(
                QrError.DocumentError.DeleteError(dbResult.exceptionOrNull()?.message)
            )

        // File removal — non-critical, cleanup job retries if needed
        when (coreFileRepo.deleteFile(document.filePath)) {
            is QrResult.Error -> Timber.w(
                "DeleteDocument: file removal failed for ${document.filePath} — " +
                "DB already soft-deleted, cleanup will retry."
            )
            is QrResult.Success -> Unit
        }

        return QrResult.Success(Unit)
    }
}
```

### 7.4 OpenDocumentUseCase

```kotlin
// client/document/domain/usecase/OpenDocumentUseCase.kt

/**
 * Opens a document in an external app using a FileProvider URI.
 *
 * Strategy:
 *  1. Exact MIME type + specific app chooser
 *  2. Fallback to "*\/*" — shows all apps that handle any file
 *  3. No app available → return NoAppAvailable error (no crash)
 */
class OpenDocumentUseCase @Inject constructor(
    private val coreFileRepo: CoreFileRepository,
    private val context: Context
) {
    suspend operator fun invoke(
        document: Document
    ): QrResult<Unit, QrError.DocumentError> {

        if (!coreFileRepo.fileExists(document.filePath))
            return QrResult.Error(QrError.DocumentError.FileNotFound(document.filePath))

        val uri = when (val r = coreFileRepo.createFileProviderUri(document.filePath)) {
            is QrResult.Error ->
                return QrResult.Error(QrError.DocumentError.OpenFailed())
            is QrResult.Success -> r.data
        }

        // Strategy 1: exact MIME type
        val opened = tryOpen(uri, document.mimeType)
            // Strategy 2: generic fallback
            ?: tryOpen(uri, "*/*")
            // Strategy 3: no app
            ?: return QrResult.Error(QrError.DocumentError.NoAppAvailable(document.mimeType))

        return QrResult.Success(Unit)
    }

    private fun tryOpen(uri: Uri, mimeType: String): Unit? {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(
                    Intent.createChooser(intent, null)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                Unit
            } else null
        } catch (e: ActivityNotFoundException) {
            Timber.w("OpenDocument: no app for mimeType=$mimeType")
            null
        }
    }
}
```

---

## 8. UI STRUCTURE

### 8.1 Entry points

Documents appear at multiple levels:

```
IslandDetailScreen  → DocumentsTab (scope=ISLAND)
FacilityDetailScreen → DocumentsTab (scope=FACILITY)   [future]
ClientDetailScreen  → DocumentsTab (scope=CLIENT)       [future]
GlobalDocumentsScreen                                   [future]
AssociateDocumentScreen ← "Open with QReport" intent
```

The `DocumentsTab` composable is reusable — it takes a `Flow<List<Document>>`
and an `onAdd` callback; scope selection is handled by the caller.

### 8.2 AssociateDocumentScreen layout

Shown when QReport receives an "Open with" intent from another app:

```
┌─────────────────────────────────────────┐
│  ← Associa Documento                   │
├─────────────────────────────────────────┤
│  📄  schema_elettrico.pdf   2.3 MB      │
├─────────────────────────────────────────┤
│  Allega a                               │
│  ● Isola specifica                      │
│  ○ Stabilimento                         │
│  ○ Cliente                              │
│  ○ Documento generale                   │
├─────────────────────────────────────────┤
│  Isola                                  │
│  ┌──────────────────────────────────┐   │
│  │ 🔍 Cerca isola...                │   │
│  ├──────────────────────────────────┤   │
│  │ ◉  IS-001 — Polaris Move        │   │
│  │    Stabilimento Nord             │   │
│  └──────────────────────────────────┘   │
├─────────────────────────────────────────┤
│  Categoria   [▾ Schemi Elettrici]       │
│  Titolo      [schema_elettrico.pdf    ] │
│  Note        [                        ] │
├─────────────────────────────────────────┤
│  [    Annulla    ]  [    Salva    ]      │
└─────────────────────────────────────────┘
```

### 8.3 DocumentsTab layout (island scope)

```
┌─────────────────────────────────────────────┐
│  [ELETTRICO] [MECCAN.]  [FLUIDI]  [TUTTI]  │  ← filter chips
├─────────────────────────────────────────────┤
│  📄  Schema_elettrico_v2.pdf          42KB  │
│      Schemi Elettrici  •  12/06/2026        │
├─────────────────────────────────────────────┤
│  📄  Manuale_fanuc_R30iB.pdf         3.2MB  │
│      Manuali  •  01/03/2026                 │
├─────────────────────────────────────────────┤
│                              [+] Aggiungi   │
└─────────────────────────────────────────────┘
```

### 8.4 DocumentCard variants

| Variant | Usage |
|---------|-------|
| `FULL` | Expanded — notes, size, mime type, scope badge, action buttons |
| `COMPACT` | Default list item — title, category chip, date, size |
| `MINIMAL` | Dense list for search results |

Long-press → selection mode → bulk delete.

### 8.5 ViewModel

```kotlin
// client/document/presentation/DocumentViewModel.kt

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val addDocument: AddDocumentUseCase,
    private val deleteDocument: DeleteDocumentUseCase,
    private val openDocument: OpenDocumentUseCase,
    private val updateDocument: UpdateDocumentUseCase,
    private val getForIsland: GetDocumentsForIslandUseCase,
    private val getForFacility: GetDocumentsForFacilityUseCase,
    private val getGlobal: GetGlobalDocumentsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentUiState())
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()

    fun loadForIsland(islandId: String) { /* collect Flow */ }
    fun loadForFacility(facilityId: String) { /* collect Flow */ }
    fun loadGlobal() { /* collect Flow */ }

    fun onCategoryFilterSelected(category: DocumentCategory?) { /* filter */ }
    fun onDocumentPicked(scope: DocumentScope, scopeEntityId: String?, uri: Uri) { /* invoke use case */ }
    fun onOpenDocument(document: Document) { /* invoke use case */ }
    fun onDeleteDocument(documentId: String) { /* invoke use case */ }
    fun onUpdateDocument(document: Document) { /* invoke use case */ }
}

data class DocumentUiState(
    val documents: List<Document> = emptyList(),
    val categoryFilter: DocumentCategory? = null,
    val isLoading: Boolean = false,
    val error: QrError.DocumentError? = null
)
```

---

## 9. SYNC READINESS

The entity is prepared for the existing sync mechanism (`7_Sync_Android.md`).
Scope FK fields allow the server to route documents correctly per hierarchy level.

| Concept | Entity sync analog | Document sync equivalent |
|---|---|---|
| `updated_at` / `synced_at` | On every entity | Present in `DocumentEntity` |
| Soft delete | `is_deleted = true` propagated | Same — record pushed with `is_deleted = true` |
| Last-write-wins | `updated_at` comparison | Metadata: same. File: SHA-256 hash comparison |
| Pull endpoint | `/sync/pull?since=` | `/documents/pull?since=` — returns metadata list |
| Push endpoint | `/sync/push` (JSON) | `/documents/push` — multipart: metadata JSON + file bytes |
| Incremental | Only changed records | Only files where local hash ≠ remote hash |
| Scope routing | N/A | Server uses `scope` + FK fields to organise storage |

A `fileHash` (SHA-256) column can be added in a future migration for content
change detection. `getPendingSync()` already returns the right records via the
`updated_at > synced_at` filter.

---

## 10. PACKAGE STRUCTURE

```
client/document/
├── domain/
│   ├── model/
│   │   ├── Document.kt
│   │   ├── DocumentScope.kt
│   │   ├── DocumentCategory.kt
│   │   ├── DocumentMimeTypes.kt
│   │   ├── DocumentDirectories.kt
│   │   └── DocumentWithContext.kt
│   ├── repository/
│   │   └── DocumentRepository.kt
│   └── usecase/
│       ├── AddDocumentUseCase.kt
│       ├── GetDocumentsForIslandUseCase.kt
│       ├── GetDocumentsForFacilityUseCase.kt
│       ├── GetDocumentsForClientUseCase.kt
│       ├── GetGlobalDocumentsUseCase.kt
│       ├── GetDocumentByIdUseCase.kt
│       ├── UpdateDocumentUseCase.kt
│       ├── DeleteDocumentUseCase.kt
│       └── OpenDocumentUseCase.kt
│
├── data/
│   ├── local/
│   │   ├── entity/
│   │   │   └── DocumentEntity.kt
│   │   └── dao/
│   │       └── DocumentDao.kt
│   └── repository/
│       └── DocumentRepositoryImpl.kt
│
└── presentation/
    ├── DocumentViewModel.kt
    ├── DocumentUiState.kt
    └── components/
        ├── DocumentsTab.kt             (reusable — takes Flow + onAdd)
        ├── DocumentCard.kt             (FULL / COMPACT / MINIMAL)
        ├── AddDocumentBottomSheet.kt   (picker launch + category/title)
        └── AssociateDocumentScreen.kt  (scope picker for "Open with" intent)
```

---

*Document: 4_8_Client_Documents.md — QReport v2.0 — June 2026*