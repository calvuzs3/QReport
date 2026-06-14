package net.calvuz.qreport.backup.domain.model

import kotlinx.serialization.Serializable

/**
 * DocumentBackupInfo - Information for a single document file backup
 *
 * Follows SignatureBackupInfo pattern.
 * [relativePath] mirrors the DocumentDirectories layout under the "documents/"
 * base directory (e.g. "islands/{islandId}/{fileName}", "global/{fileName}"),
 * so extraction reconstructs the original file layout.
 */
@Serializable
data class DocumentBackupInfo(
    val documentId: String,
    val scope: String,              // DocumentScope.name
    val fileName: String,
    val relativePath: String,
    val sizeBytes: Long,
    val sha256Hash: String
)
