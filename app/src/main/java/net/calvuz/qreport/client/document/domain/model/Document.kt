package net.calvuz.qreport.client.document.domain.model

/**
 * A reference document in QReport.
 *
 * Scope and FK fields:
 *  - [scope] determines which FK field is meaningful.
 *  - Exactly one of [islandId], [facilityId], [clientId] is non-null,
 *    matching [scope]. All three are null when scope == [DocumentScope.GLOBAL].
 *
 * Storage:
 *  - [filePath] is an absolute path inside the app's filesDir — never an
 *    external content URI. The file is guaranteed to exist when [isActive]
 *    is true and [isDeleted] is false.
 *  - [fileName] is the original name at import time (e.g. "schema_v3.pdf").
 *  - [mimeType] is detected at import time so the OS can open the file
 *    correctly without re-detection later.
 *
 * Editing:
 *  - [title] defaults to [fileName] at import but can be freely edited.
 *  - [category] and [notes] can be updated after import.
 *
 * Lifecycle (project-wide two-stage soft-delete):
 *  isActive=true,  isDeleted=false  →  normal
 *  isActive=false, isDeleted=false  →  deactivated  (first stage)
 *  isActive=false, isDeleted=true   →  marked deleted (second stage / sync marker)
 *
 * Sync:
 *  - [updatedAt] is set on every local write.
 *  - [syncedAt]  is set after a successful push to the server; null = never synced.
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
    val fileName: String,               // original file name at import
    val filePath: String,               // absolute path inside filesDir
    val fileSize: Long,                 // bytes
    val mimeType: String,               // e.g. "application/pdf"
    val fileHash: String? = null,       // null until first sync; computed at import

    // ===== METADATA =====
    val title: String,                  // editable label; defaults to fileName
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
    /**
     * The scope-appropriate FK as a single nullable string.
     * Null for [DocumentScope.GLOBAL] documents.
     */
    
    val scopeEntityId: String?
        get() = when (scope) {
            DocumentScope.ISLAND   -> islandId
            DocumentScope.FACILITY -> facilityId
            DocumentScope.CLIENT   -> clientId
            DocumentScope.GLOBAL   -> null
        }

    /**
     * True if this record has local changes that have not been
     * pushed to the server yet.
     */
    val isPendingSync: Boolean
        get() = updatedAt > (syncedAt ?: 0L)
}