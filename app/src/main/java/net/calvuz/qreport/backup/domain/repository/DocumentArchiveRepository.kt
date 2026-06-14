package net.calvuz.qreport.backup.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.backup.data.model.ArchiveProgress
import net.calvuz.qreport.backup.data.model.ExtractionProgress
import net.calvuz.qreport.backup.domain.model.BackupValidationResult
import net.calvuz.qreport.backup.domain.model.DocumentManifest

/**
 * Repository interface for document file archive operations
 *
 * Mirrors SignatureArchiveRepository pattern for consistency
 */
interface DocumentArchiveRepository {

    /**
     * Create a ZIP archive of all document files with progress tracking
     *
     * @param outputPath Path for the output documents.zip file
     * @return Flow emitting archive creation progress
     */
    suspend fun createDocumentArchive(
        outputPath: String
    ): Flow<ArchiveProgress>

    /**
     * Extract document files from archive
     *
     * @param archivePath Path to the documents.zip file
     * @param outputDir Directory to extract documents to
     * @return Flow emitting extraction progress
     */
    suspend fun extractDocumentArchive(
        archivePath: String,
        outputDir: String
    ): Flow<ExtractionProgress>

    /**
     * Generate manifest of all document files
     *
     * @return DocumentManifest with file information and hashes
     */
    suspend fun generateDocumentManifest(): DocumentManifest

    /**
     * Validate document file integrity against manifest
     *
     * @param manifest DocumentManifest to validate against
     * @return Validation result with errors and warnings
     */
    suspend fun validateDocumentIntegrity(manifest: DocumentManifest): BackupValidationResult
}
