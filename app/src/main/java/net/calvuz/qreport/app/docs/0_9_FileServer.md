# QReport — 9. File Server (Core Layer)

**Version:** 1.0
**Date:** June 2026
**Package root:** `net.calvuz.qreport.app.file`
**Scope:** Core file system abstraction — foundation for all file-based features

---

## 1. Overview

The file module provides a **thin, generic abstraction** over the Android file system.
It has zero knowledge of business features (Export, Backup, Documents, etc.); those
layers sit above it and depend on it through the `CoreFileRepository` interface.

### 1.1 Design principles

| Principle | Implementation |
|-----------|---------------|
| Zero business logic | Only raw file system operations |
| Feature extensibility | `DirectorySpec` is open — features add directories without touching core |
| Error-typed results | Every operation returns `QrResult<T, QrError.FileError>` |
| Internal storage only | All paths live under `context.filesDir` or `context.cacheDir` |
| FileProvider ready | Built-in support for secure URI sharing across apps |

### 1.2 Position in the architecture

```
┌──────────────────────────────────────────────┐
│             FEATURE LAYERS                   │
│  ExportRepository  BackupRepository          │
│  IslandDocumentRepository  ShareRepository   │
└───────────────────┬──────────────────────────┘
                    │ depends on
┌───────────────────▼──────────────────────────┐
│           CORE FILE LAYER  (this doc)        │
│  CoreFileRepository / CoreFileRepositoryImpl │
└───────────────────┬──────────────────────────┘
                    │
┌───────────────────▼──────────────────────────┐
│         Android File System                  │
│  context.filesDir  /  context.cacheDir       │
└──────────────────────────────────────────────┘
```

---

## 2. Package Structure

```
app/file/
├── domain/
│   ├── model/
│   │   ├── CoreFileInfo.kt       Generic file metadata
│   │   ├── DirectorySpec.kt      Extensible directory identifier
│   │   ├── DirectoryType.kt      Legacy enum (kept for reference, superseded)
│   │   └── FileFilter.kt         Filter criteria for listFiles()
│   └── repository/
│       └── CoreFileRepository.kt Interface — all file operations
│
└── data/
    ├── repository/
    │   └── CoreFileRepositoryImpl.kt  @Singleton implementation
    └── extension/
        └── FileFilterExt.kt      FileFilter.matches() extension
```

---

## 3. Core Models

### 3.1 `CoreFileInfo`

Returned by `listFiles()`. Immutable snapshot of a file entry.

```kotlin
data class CoreFileInfo(
    val name: String,
    val path: String,           // absolute path
    val size: Long,             // bytes
    val lastModified: Long,     // epoch ms
    val isDirectory: Boolean,
    val extension: String?      // null for files with no extension
)
```

### 3.2 `DirectorySpec`

An inline value class used as a directory identifier. Core directories are defined in
the companion object; **features add their own `object` with additional specs** without
modifying core code.

```kotlin
@JvmInline
value class DirectorySpec(val name: String) {

    companion object Core {
        val PHOTOS     = DirectorySpec("photos")
        val TEMP       = DirectorySpec("temp")
        val CACHE      = DirectorySpec("cache")
        val SIGNATURES = DirectorySpec("signatures")
    }
}
```

**Existing feature-level extensions (already defined):**

```kotlin
object SignatureDirectories {
    val SIGNATURES = DirectorySpec("signatures")
    val TECHNICIAN = DirectorySpec("signatures/technician")
    val CUSTOMER   = DirectorySpec("signatures/customer")
    val TEMP       = DirectorySpec("signatures/temp")
    val ARCHIVE    = DirectorySpec("signatures/archive")
}
```

**Pattern for a new feature (example — Island Documents):**

```kotlin
object IslandDocumentDirectories {
    val ROOT     = DirectorySpec("documents")
    val SCHEMAS  = DirectorySpec("documents/schemas")    // electrical / mechanical / fluid
    val MANUALS  = DirectorySpec("documents/manuals")
    val TEMP     = DirectorySpec("documents/temp")
}
```

The `name` value maps directly to a subdirectory under `context.filesDir`, so
`DirectorySpec("documents/schemas")` resolves to
`<filesDir>/documents/schemas/`.

### 3.3 `FileFilter`

Optional predicate for `listFiles()`. All fields are nullable — only non-null fields
are applied.

```kotlin
data class FileFilter(
    val extensions: Set<String>? = null,   // e.g. setOf("pdf", "jpg")
    val namePattern: String? = null,       // regex applied to file name
    val minSize: Long? = null,
    val maxSize: Long? = null,
    val olderThan: Long? = null,           // epoch ms — files older than this
    val newerThan: Long? = null            // epoch ms — files newer than this
)
```

Matching logic lives in `FileFilterExt.kt` as `FileFilter.matches(CoreFileInfo)`.

---

## 4. `CoreFileRepository` — API Reference

### 4.1 Directory management

| Method | Returns | Notes |
|--------|---------|-------|
| `getOrCreateDirectory(spec)` | `QrResult<String, FileError>` | Creates if missing; returns absolute path |
| `createSubDirectory(spec, name)` | `QrResult<String, FileError>` | Creates a named subdir under `spec` |

`getOrCreateDirectory` resolution logic:
- `PHOTOS` → `filesDir/photos`
- `TEMP` → `filesDir/temp`
- `CACHE` → `cacheDir`
- any other spec whose name starts with `cache/` → `cacheDir/<rest>`
- any other spec → `filesDir/<name>` (slash-separated paths create nested dirs)

### 4.2 File operations

| Method | Returns | Notes |
|--------|---------|-------|
| `copyFile(src, dst)` | `QrResult<Unit, FileError>` | Creates parent dirs of `dst` if needed |
| `moveFile(src, dst)` | `QrResult<Unit, QrError>` | Copy + delete source |
| `deleteFile(path)` | `QrResult<Unit, FileError>` | Non-existing file = `Success` (idempotent) |
| `deleteDirectory(path)` | `QrResult<Unit, FileError>` | Recursive; non-existing = `Success` |
| `fileExists(path)` | `Boolean` | Never throws |
| `getFileSize(path)` | `QrResult<Long, FileError>` | Bytes |
| `listFiles(dir, filter?)` | `QrResult<List<CoreFileInfo>, FileError>` | Optional `FileFilter`; bad entries are skipped |

### 4.3 Cleanup

| Method | Returns | Notes |
|--------|---------|-------|
| `cleanupOldFiles(dir, days)` | `QrResult<Int, FileError>` | Returns count of deleted files |
| `getDirectorySize(dir)` | `QrResult<Long, FileError>` | Recursive sum of all files |

### 4.4 FileProvider support

| Method | Returns | Notes |
|--------|---------|-------|
| `createFileProviderUri(path)` | `QrResult<Uri, FileError>` | For sharing files with other apps |
| `getFileProviderAuthority()` | `String` | `<packageName>.fileprovider` |
| `isFileProviderConfigured()` | `Boolean` | Diagnostic check |

---

## 5. Error Handling

All methods return `QrResult<T, QrError.FileError>`. Callers pattern-match on
`Success` / `Error` — no exceptions propagate out of the repository.

Relevant `QrError.FileError` values used by this layer:

// ── Directory operations ──────────────────────────────────────────────────

        /** mkdirs() returned false or threw. */
        data class DirectoryCreateError(val path: String? = null) : FileError

        /** Directory exists but cannot be accessed. */
        data class DirectoryAccessError(val path: String? = null) : FileError

        /** Directory deletion failed. */
        data class DirectoryDeleteError(val path: String? = null) : FileError

        /** Expected directory was not found. */
        data class DirectoryNotFound(val path: String? = null) : FileError

        /** Deletion refused because directory is not empty. */
        data object DirectoryNotEmpty : FileError

        /** Insufficient permissions to operate on a directory. */
        data object DirectoryPermissionDenied : FileError

        // ── File operations ───────────────────────────────────────────────────────

        /** File creation failed. */
        data class FileCreateError(val path: String? = null) : FileError

        /** File read failed. */
        data class FileReadError(val path: String? = null) : FileError

        /** File write failed. */
        data class FileWriteError(val path: String? = null) : FileError

        /** File deletion failed. */
        data class FileDeleteError(val path: String? = null) : FileError

        /** File copy failed. */
        data class FileCopyError(val source: String? = null, val destination: String? = null) : FileError

        /** File move failed. */
        data class FileMoveError(val source: String? = null, val destination: String? = null) : FileError

        /** File rename failed. */
        data class FileRenameError(val path: String? = null) : FileError

        /** Generic file access error. */
        data class FileAccessError(val path: String? = null) : FileError

        /** File was not found. */
        data class FileNotFound(val path: String? = null) : FileError

        /** A file with the same name already exists. */
        data class FileAlreadyExists(val path: String? = null) : FileError

        /** File is locked or in use. */
        data class FileLocked(val path: String? = null) : FileError

        /** File content is corrupted. */
        data class FileCorrupted(val path: String? = null) : FileError

---

## 6. Building a Feature Repository on Top

A feature repository injects `CoreFileRepository` and calls its primitives.
It owns the business logic; core owns nothing beyond the file system.

### Typical pattern

```kotlin
@Singleton
class IslandDocumentRepositoryImpl @Inject constructor(
    private val coreFiles: CoreFileRepository
) : IslandDocumentRepository {

    // Resolve the root directory once, lazily
    private suspend fun rootDir(): String {
        return coreFiles
            .getOrCreateDirectory(IslandDocumentDirectories.ROOT)
            .getOrThrow()           // or map error appropriately
    }

    // Store a document for a specific island
    suspend fun addDocument(
        islandId: String,
        sourcePath: String,
        category: DocumentCategory
    ): QrResult<CoreFileInfo, DocumentError> {
        val spec = DirectorySpec("documents/${category.dirName}/$islandId")
        val dirResult = coreFiles.getOrCreateDirectory(spec)
        // ... copy file, return CoreFileInfo
    }

    // List all documents for an island
    suspend fun getDocuments(
        islandId: String,
        category: DocumentCategory?
    ): QrResult<List<CoreFileInfo>, DocumentError> {
        val dir = "documents/${category?.dirName ?: ""}/$islandId"
        return coreFiles.listFiles(dir, FileFilter(extensions = setOf("pdf", "jpg", "png")))
            .mapError { DocumentError.LIST_FAILED }
    }

    // Remove a document
    suspend fun removeDocument(filePath: String): QrResult<Unit, DocumentError> {
        return coreFiles.deleteFile(filePath)
            .mapError { DocumentError.DELETE_FAILED }
    }
}
```

### Key points

- Feature repositories **never** access `java.io.File` directly.
- Paths are always resolved through `CoreFileRepository` methods to guarantee
  they stay within `filesDir` / `cacheDir`.
- `FileFilter` is constructed in the feature layer to express domain-level queries
  (e.g. "only PDFs", "modified this week").

---

## 7. Sync Readiness

The core file layer is sync-agnostic; it stores bytes and returns metadata. A future
sync layer for file-based features (e.g. Island Documents) would follow the same
pattern used for entity sync (`7_Sync_Android.md`):

| Concept | Entity sync analog | File sync equivalent |
|---------|--------------------|----------------------|
| `updated_at` / `synced_at` | Room column on every entity | Metadata record per file (path, size, hash, timestamps) |
| Soft delete | `is_deleted = true` | Mark file as `pending_delete`, remove after server ACK |
| Last-write-wins | `updated_at` comparison | File hash + `lastModified` comparison |
| Pull endpoint | `/sync/pull?since=` | `/files/pull?since=` returning file list + download URLs |
| Push endpoint | `/sync/push` with entity list | `/files/push` with multipart upload |
| Incremental | Only changed records | Only files where local hash ≠ remote hash |

A `FileMetadataDao` (Room table) would track each managed file:

```
file_metadata
  id            TEXT PK   -- relative path used as stable key
  island_id     TEXT      -- FK to facility_islands
  category      TEXT      -- schema / manual / etc.
  size          INTEGER
  hash          TEXT      -- SHA-256 for change detection
  last_modified INTEGER   -- epoch ms (local file mtime)
  updated_at    INTEGER   -- epoch ms (last write, local or from server)
  synced_at     INTEGER   -- epoch ms (last successful sync)
  is_deleted    INTEGER   -- 0 / 1 soft-delete flag
```

This keeps the file content in `filesDir` and the sync state in Room, consistent
with the rest of the app's architecture.

---

## 8. Implementation Notes

### 8.1 `DirectorySpec` matching in `getOrCreateDirectory`

The `when` block in `CoreFileRepositoryImpl` uses **referential equality** on
`DirectorySpec` instances. Since `DirectorySpec` is an inline value class wrapping
a `String`, equality is value-based: two specs with the same `name` are equal.
The `else` branch handles all custom specs by path construction, so no changes to
`CoreFileRepositoryImpl` are needed when adding a new feature's directory object.

### 8.2 `moveFile` signature discrepancy

`moveFile` returns `QrResult<Unit, QrError>` (not `QrError.FileError`) because the
internal `deleteFile` call after a successful copy can surface any subtype of
`QrError`. Feature callers should pattern-match on `QrError.FileError` and handle
the broader type.

### 8.3 `fileExists` never throws

The method catches all exceptions internally and returns `false`. It is safe to call
from any coroutine scope without wrapping.

### 8.4 `listFiles` resilience

Individual file entries that raise exceptions during mapping are **skipped silently**
(logged as warnings). The result is always a (possibly empty) list, never an error
due to a single bad file entry.