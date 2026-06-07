package net.calvuz.qreport.client.document.domain.model

/**
 * Identifies which hierarchy level a document belongs to.
 *
 * Exactly one FK field in [IslandDocument] is non-null, matching the scope:
 *  - ISLAND   → [IslandDocument.islandId] non-null
 *  - FACILITY → [IslandDocument.facilityId] non-null
 *  - CLIENT   → [IslandDocument.clientId] non-null
 *  - GLOBAL   → all FK fields null — app-wide reference material
 */
enum class DocumentScope {
    ISLAND,
    FACILITY,
    CLIENT,
    GLOBAL
}