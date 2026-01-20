package net.calvuz.qreport.backup.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.backup.data.model.ArchiveProgress
import net.calvuz.qreport.backup.data.model.ExtractionProgress
import net.calvuz.qreport.backup.domain.model.BackupValidationResult
import net.calvuz.qreport.backup.domain.model.SignatureManifest

/**
 * Repository interface for signature archive operations
 *
 * Mirrors PhotoArchiveRepository pattern for consistency
 */
interface SignatureArchiveRepository {

    /**
     * Create a ZIP archive of all signature files with progress tracking
     *
     * @param outputPath Path for the output signatures.zip file
     * @return Flow emitting archive creation progress
     */
    suspend fun createSignatureArchive(
        outputPath: String
    ): Flow<ArchiveProgress>

    /**
     * Extract signature files from archive
     *
     * @param archivePath Path to the signatures.zip file
     * @param outputDir Directory to extract signatures to
     * @return Flow emitting extraction progress
     */
    suspend fun extractSignatureArchive(
        archivePath: String,
        outputDir: String
    ): Flow<ExtractionProgress>

    /**
     * Generate manifest of all signature files
     *
     * @return SignatureManifest with file information and hashes
     */
    suspend fun generateSignatureManifest(): SignatureManifest

    /**
     * Validate signature integrity against manifest
     *
     * @param manifest SignatureManifest to validate against
     * @return Validation result with errors and warnings
     */
    suspend fun validateSignatureIntegrity(manifest: SignatureManifest): BackupValidationResult
}