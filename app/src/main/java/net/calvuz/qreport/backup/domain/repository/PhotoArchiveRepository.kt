package net.calvuz.qreport.backup.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.backup.data.model.ArchiveProgress
import net.calvuz.qreport.backup.data.model.ExtractionProgress
import net.calvuz.qreport.backup.domain.model.BackupValidationResult
import net.calvuz.qreport.backup.domain.model.PhotoManifest

/**
 * Repository per backup foto
 */
interface PhotoArchiveRepository {

    /**
     * Crea archivio foto
     */
    suspend fun createPhotoArchive(
        outputPath: String,
        includesThumbnails: Boolean = false
    ): Flow<ArchiveProgress>

    /**
     * Estrai archivio foto
     */
    suspend fun extractPhotoArchive(
        archivePath: String,
        outputDir: String
    ): Flow<ExtractionProgress>

    /**
     * Genera manifesto foto
     */
    suspend fun generatePhotoManifest(): PhotoManifest

    /**
     * Valida integrità foto
     */
    suspend fun validatePhotoIntegrity(manifest: PhotoManifest): BackupValidationResult
}