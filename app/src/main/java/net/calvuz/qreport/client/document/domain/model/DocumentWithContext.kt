package net.calvuz.qreport.client.document.domain.model

/**
 * Read-only projection used by list screens that need parent-entity context
 * alongside the document, without a separate query.
 *
 * All context fields are nullable because:
 *  - A GLOBAL document has no parent entity.
 *  - An ISLAND document has [islandSerialNumber] but not necessarily [facilityName]
 *    in every screen context.
 */
data class DocumentWithContext(
    val document: Document,
    val islandSerialNumber: String? = null,
    val facilityName: String?       = null,
    val companyName: String?        = null
)