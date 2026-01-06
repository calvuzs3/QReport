package net.calvuz.qreport.photo.domain.repository

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult

/**
 * PhotoFileRepository - Operazioni file specifiche per foto
 *
 * Responsabilità:
 * - Directory structure foto per checkItem
 * - Path generation photo/thumbnail
 * - File size management
 * - Photo file validation
 *
 * SI APPOGGIA a CoreFileRepository per operazioni base
 */
interface PhotoFileRepository {

    // ===== DIRECTORY MANAGEMENT =====

    /**
     * Get main photos directory
     * Compatible with: fileManager.getPhotosDirectory()
     */
    suspend fun getPhotosDirectory(): QrResult<String, QrError>

    /**
     * Get thumbnails subdirectory
     * Creates /photos/thumbnails/
     */
    suspend fun getThumbnailsDirectory(): QrResult<String, QrError>

    /**
     * Create checkItem subdirectory for photos
     * Creates /photos/{checkItemId}/
     */
    suspend fun createCheckItemPhotoDirectory(checkItemId: String): QrResult<String, QrError>

    // ===== PHOTO FILE OPERATIONS =====

    /**
     * Create unique photo file path for checkItem
     * Compatible with: fileManager.createPhotoFile(checkItemId)
     */
    suspend fun createPhotoFilePath(checkItemId: String): QrResult<String, QrError>

    /**
     * Create thumbnail file path for photo
     */
    suspend fun createThumbnailFilePath(photoFileName: String): QrResult<String, QrError>

    /**
     * Get file size for photo
     * Compatible with: fileManager.getFileSize(photoFilePath)
     */
    suspend fun getPhotoFileSize(photoFilePath: String): QrResult<Long, QrError>

    // ===== PHOTO VALIDATION =====

    /**
     * Validate photo file exists and is accessible
     */
    suspend fun validatePhotoFile(photoFilePath: String): QrResult<Boolean, QrError>

    /**
     * Check available storage space for photos
     */
    suspend fun getAvailablePhotoStorage(): QrResult<StorageInfo, QrError>

    // ===== PHOTO CLEANUP =====

    /**
     * Delete photo file and associated thumbnail
     */
    suspend fun deletePhotoFiles(photoFilePath: String, thumbnailPath: String?): QrResult<Unit, QrError>

    /**
     * Cleanup orphaned photo files (no database reference)
     */
    suspend fun cleanupOrphanedPhotos(): QrResult<Int, QrError>

    /**
     * Get all photo files for specific checkItem
     */
    suspend fun listCheckItemPhotos(checkItemId: String): QrResult<List<PhotoFileInfo>, QrError>
}

/**
 * Photo file information
 */
data class PhotoFileInfo(
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val lastModified: Long,
    val hasThumbnail: Boolean
)

/**
 * Storage information for photos
 */
data class StorageInfo(
    val totalSize: Long,
    val photosSize: Long,
    val thumbnailsSize: Long,
    val photoCount: Int,
    val availableSpace: Long
)
