package net.calvuz.qreport.ti.domain.repository

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.app.error.domain.model.QrError
import android.net.Uri
import net.calvuz.qreport.ti.domain.model.SignatureFileInfo
import net.calvuz.qreport.ti.domain.model.SignatureStorageStats

/**
 * Repository interface for digital signature file operations
 * Builds on CoreFileRepository for signature-specific business logic
 *
 * Responsibilities:
 * - Signature bitmap persistence with proper naming
 * - Intervention-specific signature organization
 * - Signature storage statistics and maintenance
 * - Integration with export and sharing systems
 */
interface SignatureFileRepository {

    // ===== SIGNATURE PERSISTENCE =====

    /**
     * Save technician signature bitmap to storage
     * Uses CoreFileRepository for actual file operations
     */
    suspend fun saveTechnicianSignature(
        interventionId: String,
        signatureBitmap: ImageBitmap
    ): QrResult<String, QrError.FileError>

    /**
     * Save customer signature bitmap to storage
     * Uses CoreFileRepository for actual file operations
     */
    suspend fun saveCustomerSignature(
        interventionId: String,
        signatureBitmap: ImageBitmap
    ): QrResult<String, QrError.FileError>

    // ===== SIGNATURE RETRIEVAL =====

    /**
     * Load signature bitmap from file path
     * Returns Android Bitmap for UI display
     */
    suspend fun loadSignatureBitmap(filePath: String): QrResult<Bitmap?, QrError.FileError>

    /**
     * Verify signature file exists and is valid
     */
    suspend fun isSignatureValid(filePath: String): QrResult<Boolean, QrError.FileError>

    /**
     * Get signature file size
     */
    suspend fun getSignatureFileSize(filePath: String): QrResult<Long, QrError.FileError>

    // ===== SIGNATURE MANAGEMENT =====

    /**
     * Delete signature file
     * Uses CoreFileRepository for actual deletion
     */
    suspend fun deleteSignature(filePath: String): QrResult<Unit, QrError.FileError>

    /**
     * Get all signature files for specific intervention
     */
    suspend fun getInterventionSignatures(interventionId: String): QrResult<List<SignatureFileInfo>, QrError.FileError>

    /**
     * Get all signatures with optional filtering
     */
    suspend fun getAllSignatures(): QrResult<List<SignatureFileInfo>, QrError.FileError>

    // ===== CLEANUP & MAINTENANCE =====

    /**
     * Clean up old signature files (maintenance operation)
     * Uses CoreFileRepository cleanup functionality
     */
    suspend fun cleanupOldSignatures(olderThanDays: Int = 30): QrResult<Int, QrError.FileError>

    /**
     * Get comprehensive signature storage statistics
     */
    suspend fun getStorageStats(): QrResult<SignatureStorageStats, QrError.FileError>

    /**
     * Verify signature storage directory integrity
     */
    suspend fun verifyStorageIntegrity(): QrResult<Boolean, QrError.FileError>

    /**
     * Get signatures directory total size
     */
    suspend fun getSignatureDirectorySize(): QrResult<Long, QrError.FileError>

    // ===== EXPORT & SHARING =====

    /**
     * Copy signature to export directory for intervention export
     * Integrates with export system
     */
    suspend fun copySignatureForExport(
        signaturePath: String,
        exportDirPath: String,
        newFilename: String? = null
    ): QrResult<String, QrError.FileError>

    /**
     * Create FileProvider URI for signature sharing
     * Uses CoreFileRepository FileProvider support
     */
    suspend fun createSignatureShareUri(signaturePath: String): QrResult<Uri, QrError.FileError>

    // ===== ORGANIZATION =====

    /**
     * Organize signatures by moving to intervention-specific subdirectory
     */
    suspend fun organizeSignaturesByIntervention(): QrResult<Int, QrError.FileError>

    /**
     * Get signature files organized by intervention ID
     */
    suspend fun getSignaturesByIntervention(): QrResult<Map<String, List<SignatureFileInfo>>, QrError.FileError>


    /**
     * Get the signatures directory path
     * Used by backup system to know where to restore signatures
     *
     * @return Path to signatures directory
     */
 suspend fun getSignaturesDirectory(): QrResult<String, QrError.FileError>


}