package net.calvuz.qreport.client.document.domain.model

import net.calvuz.qreport.app.file.domain.model.DirectorySpec

/**
 * Resolves the [DirectorySpec] for a document based on its [DocumentScope].
 *
 * Directory layout under filesDir:
 * ```
 * documents/
 * ├── islands/
 * │   └── {islandId}/
 * ├── facilities/
 * │   └── {facilityId}/
 * ├── clients/
 * │   └── {clientId}/
 * └── global/
 * ```
 *
 * Usage:
 * ```kotlin
 * val spec = DocumentDirectories.forScope(document)
 * val dirResult = coreFileRepo.getOrCreateDirectory(spec)
 * ```
 */
object DocumentDirectories {

    /**
     * Returns the [DirectorySpec] for the given document's scope.
     * The spec maps directly to a subdirectory under [android.content.Context.filesDir].
     *
     * @throws IllegalArgumentException if [document] scope is ISLAND/FACILITY/CLIENT
     *   and the corresponding FK is null.
     */
    fun forScope(document: Document): DirectorySpec = when (document.scope) {
        DocumentScope.ISLAND -> {
            requireNotNull(document.islandId) {
                "islandId must not be null when scope == ISLAND"
            }
            DirectorySpec("documents/islands/${document.islandId}")
        }

        DocumentScope.FACILITY -> {
            requireNotNull(document.facilityId) {
                "facilityId must not be null when scope == FACILITY"
            }
            DirectorySpec("documents/facilities/${document.facilityId}")
        }

        DocumentScope.CLIENT -> {
            requireNotNull(document.clientId) {
                "clientId must not be null when scope == CLIENT"
            }
            DirectorySpec("documents/clients/${document.clientId}")
        }

        DocumentScope.GLOBAL -> DirectorySpec("documents/global")
    }

    /**
     * Convenience overload — builds a minimal [Document] shell just to
     * resolve the directory when the full domain object is not yet available
     * (e.g. during import, before the document is persisted).
     */
    fun forScope(
        scope: DocumentScope,
        islandId: String?   = null,
        facilityId: String? = null,
        clientId: String?   = null
    ): DirectorySpec {
        val shell = Document(
            id = "",
            scope = scope,
            islandId = islandId,
            facilityId = facilityId,
            clientId = clientId,
            fileName = "",
            filePath = "",
            fileSize = 0L,
            mimeType = "",
            title = "",
            category = DocumentCategory.OTHER,
            createdAt = 0L,
            updatedAt = 0L
        )
        return forScope(shell)
    }
}