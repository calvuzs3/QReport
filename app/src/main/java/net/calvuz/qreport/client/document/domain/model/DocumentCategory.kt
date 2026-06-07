package net.calvuz.qreport.client.document.domain.model

/**
 * Content category of a document.
 * Used for filtering and UI grouping within any scope.
 */
enum class DocumentCategory {
    ELECTRICAL,     // Electrical schematics
    MECHANICAL,     // Mechanical drawings
    FLUID,          // Fluid / pneumatic diagrams
    MANUAL,         // Operator or maintenance manuals
    CONTRACT,       // Service agreements, warranties
    OTHER           // Any other document type
}