package net.calvuz.qreport.backup.domain.model

import kotlinx.serialization.Serializable

/**
 * DocumentManifest - Manifest for document files in backup
 *
 * Follows SignatureManifest pattern for consistency.
 * Used to track document files included in documents.zip
 */
@Serializable
data class DocumentManifest(
    val totalDocuments: Int,
    val totalSizeMB: Double,
    val documents: List<DocumentBackupInfo>
) {
    companion object {
        /**
         * Create an empty DocumentManifest
         */
        fun empty(): DocumentManifest {
            return DocumentManifest(
                totalDocuments = 0,
                totalSizeMB = 0.0,
                documents = emptyList()
            )
        }
    }
}
